/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.texture;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlas.Preparations;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import io.vram.frex.api.texture.SpriteFinder;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.mixinterface.TextureAtlasExt;
import grondag.canvas.mixinterface.TextureAtlasPreparationExt;

public class SpriteIndex {
	private static final Object2ObjectOpenHashMap<ResourceLocation, SpriteIndex> MAP = new Object2ObjectOpenHashMap<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	public static final SpriteIndex getOrCreate(ResourceLocation id) {
		return MAP.computeIfAbsent(id, SpriteIndex::new);
	}

	private ObjectArrayList<TextureAtlasSprite> spriteIndex = null;
	private final IntArrayList spriteAnimationIndex = new IntArrayList();
	private TextureAtlas atlas;
	private IntConsumer canvas_frameAnimationConsumer;
	private ResourceCache<SpriteFinder> spriteFinder;
	private int atlasWidth;
	private int atlasHeight;
	public final ResourceLocation id;

	private SpriteIndex(ResourceLocation id) {
		this.id = id;
		spriteFinder = new ResourceCache<>(this::loadSpriteFinder);
	}

	private SpriteFinder loadSpriteFinder() {
		return SpriteFinder.get(atlas);
	}

	public void reset(Preparations dataIn, ObjectArrayList<TextureAtlasSprite> spriteIndexIn, TextureAtlas atlasIn) {
		if (Configurator.enableLifeCycleDebug || Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("Lifecycle Event: SpriteIndex reset");
		}

		atlas = atlasIn;
		canvas_frameAnimationConsumer = ((TextureAtlasExt) atlasIn).canvas_frameAnimationConsumer();

		spriteIndex = spriteIndexIn;
		atlasWidth = ((TextureAtlasPreparationExt) dataIn).canvas_atlasWidth();
		atlasHeight = ((TextureAtlasPreparationExt) dataIn).canvas_atlasHeight();

		spriteAnimationIndex.clear();

		for (final var sprite : spriteIndexIn) {
			spriteAnimationIndex.add(((SpriteExt) sprite).canvas_animationIndex());
		}
	}

	public TextureAtlasSprite fromId(int spriteId) {
		return spriteIndex.get(spriteId);
	}

	public int animationIndexFromSpriteId(int spriteId) {
		return spriteAnimationIndex.getInt(spriteId);
	}

	public float mapU(int spriteId, float unmappedU) {
		final TextureAtlasSprite sprite = spriteIndex.get(spriteId);
		final float u0 = sprite.getU0();
		return u0 + unmappedU * (sprite.getU1() - u0);
	}

	public float mapV(int spriteId, float unmappedV) {
		final TextureAtlasSprite sprite = spriteIndex.get(spriteId);
		final float v0 = sprite.getV0();
		return v0 + unmappedV * (sprite.getV1() - v0);
	}

	public int atlasWidth() {
		return atlasWidth;
	}

	public int atlasHeight() {
		return atlasHeight;
	}

	public TextureAtlas atlas() {
		return atlas;
	}

	public SpriteFinder spriteFinder() {
		return spriteFinder.getOrLoad();
	}

	public void trackPerFrameAnimation(int spriteId) {
		final int animationIndex = animationIndexFromSpriteId(spriteId);

		if (animationIndex >= 0) {
			canvas_frameAnimationConsumer.accept(animationIndex);
		}
	}
}
