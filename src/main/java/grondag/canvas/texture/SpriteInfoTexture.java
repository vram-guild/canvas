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

import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;

@Environment(EnvType.CLIENT)
public class SpriteInfoTexture implements AutoCloseable {
	protected int glId = -1;

	private SpriteInfoTexture() {
		glId = TextureUtil.generateId();
		final SpriteAtlasTexture atlas = MinecraftClient.getInstance().getBakedModelManager().method_24153(SpriteAtlasTexture.BLOCK_ATLAS_TEX);

		try(final SpriteInfoImage image = new SpriteInfoImage(atlas)) {
			GL21.glActiveTexture(TextureData.SPRITE_INFO);
			GL21.glBindTexture(GL21.GL_TEXTURE_1D, glId);
			image.upload();
			GL21.glActiveTexture(TextureData.MC_SPRITE_ATLAS);
		}
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

	public static int lookup(MutableQuadViewImpl quad, float spriteV) {
		// TODO Auto-generated method stub
		return 0;
	}
}
