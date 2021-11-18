/*
 * Copyright © Original Authors
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

package grondag.canvas.material.property;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.texture.SpriteFinder;
import io.vram.frex.api.texture.SpriteIndex;

import grondag.canvas.CanvasMod;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.texture.MaterialIndexProvider;
import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

public class TextureMaterialState {
	public final int index;
	public final ResourceLocation id;

	private AbstractTexture texture;
	private boolean isAtlas;
	private SpriteFinder spriteFinder;
	private SpriteIndex spriteIndex;
	private MaterialIndexProvider donglenator;

	private TextureMaterialState(int index, ResourceLocation id) {
		this.index = index;
		this.id = id;
	}

	private void retreiveTexture() {
		if (texture == null) {
			final TextureManager tm = Minecraft.getInstance().getTextureManager();
			// forces registration
			tm.bindForSetup(id);
			texture = tm.getTexture(id);
			isAtlas = texture != null && texture instanceof TextureAtlas;

			if (isAtlas) {
				spriteIndex = SpriteIndex.getOrCreate(id);
				donglenator = MaterialIndexProvider.getOrCreateForAtlas(id);
				spriteFinder = SpriteFinder.get((TextureAtlas) texture);
			} else {
				spriteIndex = null;
				donglenator = MaterialIndexProvider.GENERIC;
			}
		}
	}

	public SpriteFinder spriteFinder() {
		retreiveTexture();
		return spriteFinder;
	}

	public MaterialIndexProvider materialIndexProvider() {
		retreiveTexture();
		return donglenator;
	}

	public AbstractTexture texture() {
		retreiveTexture();
		return texture;
	}

	public TextureAtlas textureAsAtlas() {
		return (TextureAtlas) texture();
	}

	public boolean isAtlas() {
		retreiveTexture();
		return isAtlas;
	}

	public SpriteIndex spriteIndex() {
		retreiveTexture();
		return spriteIndex;
	}

	public void enable(boolean bilinear) {
		// Note we don't call texture enable anywhere here - it only applies to fixed function pipeline

		if (activeState == this) {
			if (bilinear != activeIsBilinearFilter) {
				CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
				CanvasTextureState.bindTexture(texture().getId());
				setFilter(bilinear);
				activeIsBilinearFilter = bilinear;
			}
		} else {
			if (this == TextureMaterialState.NO_TEXTURE) {
				CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
				CanvasTextureState.bindTexture(0);
			} else {
				CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
				CanvasTextureState.bindTexture(texture().getId());
				setFilter(bilinear);

				activeIsBilinearFilter = bilinear;
				activeState = this;
			}
		}
	}

	/**
	 * Vanilla logic for texture filtering works like this:
	 *
	 * <p><pre>
	 * 	int minFilter;
	 * 	int magFilter;
	 *
	 * 	if (bilinear) {
	 * 		minFilter = mipmap ? GL21.GL_LINEAR_MIPMAP_LINEAR : GL21.GL_LINEAR;
	 * 		magFilter = GL21.GL_LINEAR;
	 * 	} else {
	 * 		minFilter = mipmap ? GL21.GL_NEAREST_MIPMAP_LINEAR : GL21.GL_NEAREST;
	 * 		magFilter = GL21.GL_NEAREST;
	 * 	}
	 *
	 * 	GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, minFilter);
	 * 	GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, magFilter);
	 * </pre>
	 *
	 * <p>In vanilla,"bilinear" enables blur on magnification, makes minification blur LOD-linear.
	 * It seems to only be used only be used for enchantment glint.
	 *
	 * <p>The vanilla mipmap parameter enables minification blur (linear with nearest LOD).
	 * Vanilla does not use GL_LINEAR_MIPMAP_LINEAR for most rendering because atlas textures
	 * cause artifacts when sampled across LODS levels, especially for randomized rotated sprites
	 * like grass.
	 *
	 * <p>Canvas controlled mipmap in shader, so we always set that to GL_NEAREST_MIPMAP_LINEAR unless
	 * bilinear filtering is needed.
	 */
	private static void setFilter(boolean bilinear) {
		if (bilinear) {
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_LINEAR_MIPMAP_LINEAR);
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_LINEAR);
		} else {
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_NEAREST_MIPMAP_LINEAR);
			GFX.texParameter(GFX.GL_TEXTURE_2D, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_NEAREST);
		}
	}

	private static int nextIndex = 1;
	private static final TextureMaterialState[] STATES = new TextureMaterialState[MaterialConstants.MAX_TEXTURE_STATES];
	private static final Object2ObjectOpenHashMap<ResourceLocation, TextureMaterialState> MAP = new Object2ObjectOpenHashMap<>(256, Hash.VERY_FAST_LOAD_FACTOR);

	public static final TextureMaterialState NO_TEXTURE = new TextureMaterialState(0, TextureManager.INTENTIONAL_MISSING_TEXTURE) {
		@Override
		public void enable(boolean bilinear) {
			if (activeState != this) {
				activeState = this;
			}
		}
	};

	public static final TextureMaterialState MISSING;

	private static TextureMaterialState activeState = NO_TEXTURE;
	private static boolean activeIsBilinearFilter = false;

	public static void disable() {
		if (activeState != null) {
			activeState = null;
		}
	}

	static {
		STATES[0] = NO_TEXTURE;
		MAP.defaultReturnValue(NO_TEXTURE);
		MISSING = fromId(TextureManager.INTENTIONAL_MISSING_TEXTURE);
	}

	public static TextureMaterialState fromIndex(int index) {
		return STATES[index];
	}

	private static boolean shouldWarn = true;

	// PERF: use cow or other method to avoid synch
	public static synchronized TextureMaterialState fromId(ResourceLocation id) {
		TextureMaterialState state = MAP.get(id);

		if (state == NO_TEXTURE) {
			if (nextIndex >= MaterialConstants.MAX_TEXTURE_STATES) {
				if (shouldWarn) {
					shouldWarn = false;
					CanvasMod.LOG.warn(String.format("Maximum unique textures (%d) exceeded when attempting to add %s.  Missing texture will be used.",
							MaterialConstants.MAX_TEXTURE_STATES, id.toString()));
					CanvasMod.LOG.warn("Previously encountered textures are listed below. Subsequent warnings are suppressed.");

					for (final TextureMaterialState extant : STATES) {
						CanvasMod.LOG.info(extant == null ? "Null (this is a bug)" : extant.id.toString());
					}
				}

				return MISSING;
			}

			final int index = nextIndex++;
			state = new TextureMaterialState(index, id);
			MAP.put(id, state);
			STATES[index] = state;
		}

		return state;
	}

	public static void reload() {
		MAP.values().forEach(t -> {
			t.texture = null;
		});
	}
}
