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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteAtlasTexture.Data;
import net.minecraft.util.Identifier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.SpriteAtlasTextureDataExt;
import grondag.canvas.mixinterface.SpriteExt;

@Environment(EnvType.CLIENT)
public class SpriteIndex {
	private static final Object2ObjectOpenHashMap<Identifier, SpriteIndex> MAP = new Object2ObjectOpenHashMap<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	public static final SpriteIndex getOrCreate(Identifier id) {
		return MAP.computeIfAbsent(id, SpriteIndex::new);
	}

	private ObjectArrayList<Sprite> spriteIndex = null;
	private final IntArrayList spriteAnimationIndex = new IntArrayList();
	private SpriteAtlasTexture atlas;
	private ResourceCache<SpriteFinder> spriteFinder;
	private int atlasWidth;
	private int atlasHeight;
	public final Identifier id;

	private SpriteIndex(Identifier id) {
		this.id = id;
		spriteFinder = new ResourceCache<>(this::loadSpriteFinder);
	}

	private SpriteFinder loadSpriteFinder() {
		return SpriteFinder.get(atlas);
	}

	public void reset(Data dataIn, ObjectArrayList<Sprite> spriteIndexIn, SpriteAtlasTexture atlasIn) {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: SpriteInfoTexture init");
		}

		atlas = atlasIn;
		spriteIndex = spriteIndexIn;
		atlasWidth = ((SpriteAtlasTextureDataExt) dataIn).canvas_atlasWidth();
		atlasHeight = ((SpriteAtlasTextureDataExt) dataIn).canvas_atlasHeight();

		spriteAnimationIndex.clear();

		for (final var sprite : spriteIndexIn) {
			spriteAnimationIndex.add(((SpriteExt) sprite).canvas_animationIndex());
		}
	}

	public Sprite fromId(int spriteId) {
		return spriteIndex.get(spriteId);
	}

	public int animationIndexFromSpriteId(int spriteId) {
		return spriteAnimationIndex.getInt(spriteId);
	}

	public float mapU(int spriteId, float unmappedU) {
		final Sprite sprite = spriteIndex.get(spriteId);
		final float u0 = sprite.getMinU();
		return u0 + unmappedU * (sprite.getMaxU() - u0);
	}

	public float mapV(int spriteId, float unmappedV) {
		final Sprite sprite = spriteIndex.get(spriteId);
		final float v0 = sprite.getMinV();
		return v0 + unmappedV * (sprite.getMaxV() - v0);
	}

	public int atlasWidth() {
		return atlasWidth;
	}

	public int atlasHeight() {
		return atlasHeight;
	}

	public SpriteAtlasTexture atlas() {
		return atlas;
	}

	public SpriteFinder spriteFinder() {
		return spriteFinder.getOrLoad();
	}
}
