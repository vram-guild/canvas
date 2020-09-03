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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteAtlasTexture.Data;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.SpriteAtlasTextureDataExt;
import grondag.canvas.mixinterface.SpriteAtlasTextureExt;

@Environment(EnvType.CLIENT)
public class SpriteInfoTexture implements AutoCloseable {
	protected int glId = -1;
	private final int textureSize;
	final ObjectArrayList<Sprite> spriteIndex;
	public final SpriteAtlasTexture atlas;
	public final int atlasWidth;
	public final int atlasHeight;

	public final SpriteFinder spriteFinder;

	private SpriteInfoTexture(Data atlasData) {
		final SpriteAtlasTexture atlas = (SpriteAtlasTexture) MinecraftClient.getInstance().getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
		this.atlas = atlas;
		atlasWidth = ((SpriteAtlasTextureDataExt)atlasData).canvas_atlasWidth();
		atlasHeight = ((SpriteAtlasTextureDataExt)atlasData).canvas_atlasHeight();
		spriteFinder = SpriteFinder.get(atlas);

		final ObjectArrayList<Sprite> spriteIndex = ((SpriteAtlasTextureExt) atlas).canvas_spriteIndex();
		this.spriteIndex = spriteIndex;

		final int spriteCount = spriteIndex.size();
		textureSize = MathHelper.smallestEncompassingPowerOfTwo(spriteCount);

		if (!RenderSystem.isOnRenderThread()) {
			RenderSystem.recordRenderCall(() -> {
				createImage(spriteCount);
			});
		} else {
			createImage(spriteCount);
		}
	}

	private void createImage(int spriteCount) {
		try(final SpriteInfoImage image = new SpriteInfoImage(spriteIndex, spriteCount, textureSize)) {
			glId = TextureUtil.generateId();

			GlStateManager.activeTexture(TextureData.SPRITE_INFO);
			GlStateManager.bindTexture(glId);

			// Bragging rights and eternal gratitude to Wyn Price (https://github.com/Wyn-Price)
			// for reminding me pixelStore exists, thus fixing #92 and preserving a tattered
			// remnant of my sanity. I owe you a favor!

			GlStateManager.pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
			GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
			GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
			GlStateManager.pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);

			image.upload();
			image.close();

			GlStateManager.enableTexture();
			RenderSystem.matrixMode(GL21.GL_TEXTURE);
			RenderSystem.loadIdentity();
			RenderSystem.matrixMode(GL21.GL_MODELVIEW);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LEVEL, 0);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_LOD, 0);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LOD, 0);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_S, GL21.GL_REPEAT);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_T, GL21.GL_REPEAT);
			GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);

			GlStateManager.bindTexture(0);
			GlStateManager.disableTexture();
			GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
		} catch (final Exception e) {
			CanvasMod.LOG.warn("Unable to create sprite info texture due to error:", e);

			if (glId != -1) {
				TextureUtil.deleteId(glId);
				glId = -1;
			}
		}
	}

	@Override
	public void close() {
		if (glId != -1) {
			disable();
			TextureUtil.deleteId(glId);
			glId = -1;
		}
	}

	public void disable() {
		GlStateManager.activeTexture(TextureData.SPRITE_INFO);
		GlStateManager.bindTexture(0);
		GlStateManager.disableTexture();
		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public void enable() {
		GlStateManager.activeTexture(TextureData.SPRITE_INFO);
		GlStateManager.bindTexture(glId);
		GlStateManager.enableTexture();
		GlStateManager.activeTexture(TextureData.MC_SPRITE_ATLAS);
	}

	public int coordinate(int spriteId) {
		return spriteId;
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

	private static SpriteInfoTexture instance;
	private static Data atlasData;

	public static SpriteInfoTexture instance() {
		SpriteInfoTexture result = instance;

		if(result == null) {
			result = new SpriteInfoTexture(atlasData);
			instance = result;
			atlasData = null;
		}

		return result;
	}

	public static void reset(Data input) {
		if(instance != null) {
			instance.close();
		}

		instance = null;
		atlasData = input;
	}

	public int textureSize() {
		return textureSize;
	}
}
