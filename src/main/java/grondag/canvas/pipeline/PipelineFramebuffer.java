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

package grondag.canvas.pipeline;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.AttachmentConfig;
import grondag.canvas.pipeline.config.FramebufferConfig;

// FEAT: handle clear masks
public class PipelineFramebuffer {
	final FramebufferConfig config;
	final float[][] clearColor;
	final int[] attachmentPoints;
	final int colorClearFlags;
	final int clearMask;

	private int fboGlId = -1;

	private static final int R = 0;
	private static final int G = 1;
	private static final int B = 2;
	private static final int A = 3;

	PipelineFramebuffer(FramebufferConfig config, int width, int height) {
		this.config = config;

		final int count = config.colorAttachments.length;

		clearColor = new float[count][4];
		attachmentPoints = new int[count];
		int attachmentPoint = FramebufferInfo.COLOR_ATTACHMENT;
		int clearFlags = 0;

		for (int i = 0; i < count; ++i) {
			if (config.colorAttachments[i].clear) {
				clearFlags |= (1 << i);
				final int color = config.colorAttachments[i].clearColor;
				clearColor[i][A] = ((color >> 24) & 0xFF) / 255f;
				clearColor[i][R] = ((color >> 16) & 0xFF) / 255f;
				clearColor[i][G] = ((color >> 8) & 0xFF) / 255f;
				clearColor[i][B] = (color & 0xFF) / 255f;
			}

			attachmentPoints[i] = attachmentPoint++;
		}

		colorClearFlags = clearFlags;

		clearMask = (colorClearFlags != 0) ? 1 : 0;

		open(width, height);
	}

	public int glId() {
		return fboGlId;
	}

	void open(int width, int height) {
		fboGlId = GlStateManager.genFramebuffers();

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);

		GL21.glDrawBuffers(attachmentPoints);

		for (int i = 0; i < config.colorAttachments.length; ++i) {
			final AttachmentConfig ac = config.colorAttachments[i];
			final Image img = Pipeline.getImage(ac.image.name);

			if (img == null) {
				CanvasMod.LOG.warn(String.format("Frambuffer %s cannot be completetly configured because color attachment %s was not found",
						config.name, ac.image.name));
			} else {
				GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + i, GL21.GL_TEXTURE_2D, img.glId(), ac.lod);
			}
		}

		if (config.depthAttachment != null) {
			final Image img = Pipeline.getImage(config.depthAttachment.image.name);

			if (img == null) {
				CanvasMod.LOG.warn(String.format("Frambuffer %s cannot be completetly configured because depth attachment %s was not found",
						config.name, config.depthAttachment.image.name));
			} else {
				GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.DEPTH_ATTACHMENT, GL21.GL_TEXTURE_2D, img.glId, 0);
			}
		}
	}

	public void clear() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);

		if (colorClearFlags == 1) {
			// Try for combined depth/color clear if have single color
			int mask = GL21.GL_COLOR_BUFFER_BIT;

			if (config.depthAttachment.clear) {
				mask |= GL21.GL_DEPTH_BUFFER_BIT;
				GlStateManager.clearDepth(config.depthAttachment.clearDepth);
			}

			GlStateManager.clearColor(clearColor[0][R], clearColor[0][G], clearColor[0][B], clearColor[0][A]);
			GlStateManager.clear(mask, MinecraftClient.IS_SYSTEM_MAC);
		} else {
			// Clears happen separately in other cases
			if (config.depthAttachment.clear) {
				GlStateManager.clearDepth(config.depthAttachment.clearDepth);
				GlStateManager.clear(GL21.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
			}

			if (colorClearFlags != 0) {
				final int count = clearColor.length;

				for (int i = 0; i < count; ++i) {
					if ((colorClearFlags & (1 << i)) != 0) {
						GL21.glDrawBuffer(FramebufferInfo.COLOR_ATTACHMENT + i);
						GlStateManager.clearColor(clearColor[i][R], clearColor[i][G], clearColor[i][B], clearColor[i][A]);
						GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
					}
				}

				GL21.glDrawBuffers(attachmentPoints);
			}
		}
	}

	public void bind() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);
	}

	void close() {
		if (fboGlId != -1) {
			GlStateManager.deleteFramebuffers(fboGlId);
			fboGlId = -1;
		}
	}
}
