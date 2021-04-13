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

import com.mojang.blaze3d.platform.TextureUtil;
import it.unimi.dsi.fastutil.Hash;
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
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public class SpriteInfoTexture {
	private static final Object2ObjectOpenHashMap<Identifier, SpriteInfoTexture> MAP = new Object2ObjectOpenHashMap<>(64, Hash.VERY_FAST_LOAD_FACTOR);

	public static final SpriteInfoTexture getOrCreate(Identifier id) {
		return MAP.computeIfAbsent(id, SpriteInfoTexture::new);
	}

	private ObjectArrayList<Sprite> spriteIndex = null;
	private SpriteAtlasTexture atlas;
	private SpriteFinder spriteFinder;
	private int atlasWidth;
	private int atlasHeight;
	private int spriteCount = -1;
	private int glId = 0;
	private int bufferId;
	public final Identifier id;

	private SpriteInfoTexture(Identifier id) {
		this.id = id;
	}

	public void reset(Data dataIn, ObjectArrayList<Sprite> spriteIndexIn, SpriteAtlasTexture atlasIn) {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: SpriteInfoTexture init");
		}

		if (glId != 0) {
			disable();
			TextureUtil.releaseTextureId(glId);
			glId = 0;
		}

		if (bufferId != 0) {
			GFX.deleteBuffers(bufferId);
			bufferId = 0;
		}

		atlas = atlasIn;
		spriteFinder = SpriteFinder.get(atlas);
		spriteIndex = spriteIndexIn;
		spriteCount = spriteIndex.size();
		atlasWidth = ((SpriteAtlasTextureDataExt) dataIn).canvas_atlasWidth();
		atlasHeight = ((SpriteAtlasTextureDataExt) dataIn).canvas_atlasHeight();
	}

	private void createImageIfNeeded() {
		if (glId == 0) {
			createImage();
		}
	}

	private void createImage() {
		try (SpriteInfoImage image = new SpriteInfoImage(spriteIndex, spriteCount)) {
			glId = GFX.genTexture();
			bufferId = GFX.genBuffer();

			CanvasTextureState.activeTextureUnit(TextureData.SPRITE_INFO);
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, glId);

			image.upload(bufferId);

			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, 0);
			CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to create sprite info texture due to error:", e);

			if (glId != 0) {
				GFX.deleteTexture(glId);
				glId = 0;
			}
		}
	}

	public static void disable() {
		CanvasTextureState.activeTextureUnit(TextureData.SPRITE_INFO);
		CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, 0);
		CanvasTextureState.activeTextureUnit(TextureData.MC_SPRITE_ATLAS);
	}

	public void enable() {
		createImageIfNeeded();
		CanvasTextureState.activeTextureUnit(TextureData.SPRITE_INFO);
		CanvasTextureState.bindTexture(GFX.GL_TEXTURE_BUFFER, glId);
	}

	public Sprite fromId(int spriteId) {
		return spriteIndex.get(spriteId);
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
		return spriteFinder;
	}
}
