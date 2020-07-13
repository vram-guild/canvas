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

import java.nio.IntBuffer;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.texture.TextureUtil;

/**
 * Leaner adaptation of Minecraft NativeImageBackedTexture suitable for our needs.
 */
public class SimpleTexture implements AutoCloseable {
	private SimpleImage image;
	protected int glId = -1;

	public SimpleTexture(SimpleImage image, int internalFormat) {
		this.image = image;

		bindTexture();
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
		GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);
		GlStateManager.texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, image.width, image.height, 0, image.pixelDataFormat, image.pixelDataType, (IntBuffer)null);
	}

	public int getGlId() {
		if (glId == -1) {
			glId = TextureUtil.generateId();
		}

		return glId;
	}

	public void clearGlId() {
		if (glId != -1) {
			TextureUtil.deleteId(glId);
			glId = -1;
		}
	}

	public void bindTexture() {
		GlStateManager.bindTexture(getGlId());
	}

	public void upload() {
		bindTexture();
		image.upload(0, 0, 0, false);
	}

	public void uploadPartial(int x, int y, int width, int height) {
		bindTexture();
		image.upload(0, x, y, x, y, width, height, false);
	}

	@Nullable
	public SimpleImage getImage() {
		return image;
	}

	public void setImage(SimpleImage image) throws Exception {
		this.image.close();
		this.image = image;
	}

	@Override
	public void close() {
		image.close();
		clearGlId();
		image = null;
	}
}
