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
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.varia.CanvasGlHelper;

@Environment(EnvType.CLIENT)
public class SpriteInfoTexture implements AutoCloseable {
	protected int glId = -1;
	private final SpriteFinder spriteFinder;
	private final int textureSize;
	private final ObjectArrayList<Sprite> indexed = new ObjectArrayList<>();

	private SpriteInfoTexture() {
		glId = TextureUtil.generateId();
		final SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager().method_24153(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
		spriteFinder = SpriteFinder.get(atlas);
		int size = 1;
		try(final SpriteInfoImage image = new SpriteInfoImage(atlas, indexed)) {
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

	public void normalize(MutableQuadViewImpl quad, int textureIndex) {
		final Sprite sprite = spriteFinder.find(quad, textureIndex);
		final int spriteId = ((SpriteExt) sprite).canvas_id();
		final float u0 = sprite.getMinU();
		final float v0 = sprite.getMinV();
		final float uSpanInv = 1f / (sprite.getMaxU() - u0);
		final float vSpanInv = 1f / (sprite.getMaxV() - v0);

		quad.spriteRaw(0, textureIndex, (quad.spriteRawU(0, textureIndex) - u0) * uSpanInv, (quad.spriteRawV(0, textureIndex) - v0) * vSpanInv);
		quad.spriteRaw(1, textureIndex, (quad.spriteRawU(1, textureIndex) - u0) * uSpanInv, (quad.spriteRawV(1, textureIndex) - v0) * vSpanInv);
		quad.spriteRaw(2, textureIndex, (quad.spriteRawU(2, textureIndex) - u0) * uSpanInv, (quad.spriteRawV(2, textureIndex) - v0) * vSpanInv);
		quad.spriteRaw(3, textureIndex, (quad.spriteRawU(3, textureIndex) - u0) * uSpanInv, (quad.spriteRawV(3, textureIndex) - v0) * vSpanInv);
		quad.spriteId(textureIndex, spriteId);
	}

	public int coordinate(int spriteId) {
		// PERF: shifts here - textureSize always a power of 2
		return (spriteId * 0x10000 + 1) / textureSize;
	}

	public float denormalizeU(int spriteId, float spriteU) {
		final Sprite sprite = indexed.get(spriteId);
		final float u0 = sprite.getMinU();
		return u0 + spriteU * (sprite.getMaxU() - u0);
	}

	public float denormalizeV(int spriteId, float spriteV) {
		final Sprite sprite = indexed.get(spriteId);
		final float v0 = sprite.getMinV();
		return v0 + spriteV * (sprite.getMaxV() - v0);
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

	public static void reload() {
		if(instance != null) {
			instance.close();
		}

		instance = null;
	}
}
