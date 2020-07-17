/*******************************************************************************
 * Copyright 2019, 2020 grondag
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package grondag.canvas.texture;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.SpriteAtlasTextureExt;
import grondag.canvas.varia.CanvasGlHelper;

@Environment(EnvType.CLIENT)
public class SpriteInfoTexture implements AutoCloseable {
	protected int glId = -1;
	private final int textureSize;
	final ObjectArrayList<Sprite> spriteIndex;
	public final SpriteAtlasTexture atlas;
	public final SpriteFinder spriteFinder;

	private SpriteInfoTexture() {
		glId = TextureUtil.generateId();
		final SpriteAtlasTexture atlas = (SpriteAtlasTexture) MinecraftClient.getInstance().getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
		this.atlas = atlas;
		spriteFinder = SpriteFinder.get(atlas);

		final ObjectArrayList<Sprite> spriteIndex = ((SpriteAtlasTextureExt) atlas).canvas_spriteIndex();
		this.spriteIndex = spriteIndex;

		int size = 1;
		try(final SpriteInfoImage image = new SpriteInfoImage(spriteIndex)) {
			size = image.size;
			GL21.glActiveTexture(TextureData.SPRITE_INFO);
			assert CanvasGlHelper.checkError();
			GL21.glBindTexture(GL21.GL_TEXTURE_1D, glId);
			assert CanvasGlHelper.checkError();
			GL21.glTexParameteri(GL11.GL_TEXTURE_1D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
			assert CanvasGlHelper.checkError();
			GL21.glTexParameteri(GL11.GL_TEXTURE_1D, GL12.GL_TEXTURE_MIN_LOD, 0);
			assert CanvasGlHelper.checkError();
			GL21.glTexParameteri(GL21.GL_TEXTURE_1D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
			assert CanvasGlHelper.checkError();
			GL21.glTexParameteri(GL21.GL_TEXTURE_1D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);
			assert CanvasGlHelper.checkError();
			image.upload();
			image.close();
			GL21.glActiveTexture(TextureData.MC_SPRITE_ATLAS);
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to create sprite info texture due to error:", e);
		}

		textureSize = size;
	}

	@Override
	public void close() {
		if (glId != -1) {
			TextureUtil.deleteId(glId);
			glId = -1;
		}
	}

	public void disable() {
		GL21.glActiveTexture(TextureData.SPRITE_INFO);
		GL21.glBindTexture(GL21.GL_TEXTURE_1D, 0);
		GL21.glActiveTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void enable() {
		GL21.glActiveTexture(TextureData.SPRITE_INFO);
		GL21.glBindTexture(GL21.GL_TEXTURE_1D, glId);
		GL21.glActiveTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public int coordinate(int spriteId) {
		// PERF: shifts here - textureSize always a power of 2
		return (spriteId * 0x10000 + 1) / textureSize;
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

	private static SpriteInfoTexture instance;

	public static SpriteInfoTexture instance() {
		SpriteInfoTexture result = instance;

		if(result == null) {
			result = new SpriteInfoTexture();
			instance = result;
		}

		return result;
	}

	public static void reset() {
		if(instance != null) {
			instance.close();
		}

		instance = null;
	}
}
