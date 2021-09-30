/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
