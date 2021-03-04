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

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glReadBuffer;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL46;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.AttachmentConfig;
import grondag.canvas.pipeline.config.FramebufferConfig;
import grondag.canvas.varia.CanvasGlHelper;

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

		GlStateManager.bindFramebuffer(GL_FRAMEBUFFER, fboGlId);

		if (config.colorAttachments.length == 0) {
			glDrawBuffer(GL_NONE);
			glReadBuffer(GL_NONE);
		} else {
			glDrawBuffers(attachmentPoints);
		}

		assert CanvasGlHelper.checkError();

		// TODO: needs better handling of arrays, 3D and other target type
		// and attachments need a way to specify level

		for (int i = 0; i < config.colorAttachments.length; ++i) {
			final AttachmentConfig ac = config.colorAttachments[i];
			final Image img = Pipeline.getImage(ac.image.name);

			if (img == null) {
				CanvasMod.LOG.warn(String.format("Framebuffer %s cannot be completely configured because color attachment %s was not found",
						config.name, ac.image.name));
			} else if (img.config.target == GL46.GL_TEXTURE_2D) {
				GL46.glFramebufferTexture2D(GL_FRAMEBUFFER, FramebufferInfo.COLOR_ATTACHMENT + i, img.config.target, img.glId(), ac.lod);
				assert CanvasGlHelper.checkError();
			} else if (img.config.target == GL46.GL_TEXTURE_2D_ARRAY || img.config.target == GL46.GL_TEXTURE_3D) {
				GL46.glFramebufferTextureLayer(GL_FRAMEBUFFER, FramebufferInfo.COLOR_ATTACHMENT + i, img.glId(), ac.lod, 0);
				assert CanvasGlHelper.checkError();
			}
		}

		if (config.depthAttachment != null) {
			final Image img = Pipeline.getImage(config.depthAttachment.image.name);

			if (img == null) {
				CanvasMod.LOG.warn(String.format("Framebuffer %s cannot be completely configured because depth attachment %s was not found",
						config.name, config.depthAttachment.image.name));
			} else if (img.config.target == GL46.GL_TEXTURE_2D) {
				GL46.glFramebufferTexture2D(GL_FRAMEBUFFER, FramebufferInfo.DEPTH_ATTACHMENT, img.config.target, img.glId(), 0);
				assert CanvasGlHelper.checkError();
			} else if (img.config.target == GL46.GL_TEXTURE_2D_ARRAY || img.config.target == GL46.GL_TEXTURE_3D) {
				GL46.glFramebufferTextureLayer(GL_FRAMEBUFFER, FramebufferInfo.DEPTH_ATTACHMENT, img.glId(), 0, 0);
				assert CanvasGlHelper.checkError();
			}
		}

		final int check = GlStateManager.checkFramebufferStatus(GL_FRAMEBUFFER);

		if (check != GL_FRAMEBUFFER_COMPLETE) {
			CanvasMod.LOG.warn("Framebuffer " + config.name + " has invalid status " + check + " " + GlSymbolLookup.reverseLookup(check));
		}
	}

	public void clear() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);

		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.depthMask(true);

		if (colorClearFlags == 1) {
			// Try for combined depth/color clear if have single color
			int mask = GL_COLOR_BUFFER_BIT;

			if (config.depthAttachment.clear) {
				mask |= GL_DEPTH_BUFFER_BIT;
				GlStateManager.clearDepth(config.depthAttachment.clearDepth);
			}

			GlStateManager.clearColor(clearColor[0][R], clearColor[0][G], clearColor[0][B], clearColor[0][A]);
			GlStateManager.clear(mask, MinecraftClient.IS_SYSTEM_MAC);
		} else {
			// Clears happen separately in other cases
			if (config.depthAttachment.clear) {
				GlStateManager.clearDepth(config.depthAttachment.clearDepth);
				GlStateManager.clear(GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
			}

			if (colorClearFlags != 0) {
				final int count = clearColor.length;

				for (int i = 0; i < count; ++i) {
					if ((colorClearFlags & (1 << i)) != 0) {
						glDrawBuffer(FramebufferInfo.COLOR_ATTACHMENT + i);
						GlStateManager.clearColor(clearColor[i][R], clearColor[i][G], clearColor[i][B], clearColor[i][A]);
						GlStateManager.clear(GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
					}
				}

				glDrawBuffers(attachmentPoints);
				assert CanvasGlHelper.checkError();
			}
		}
	}

	public void bind() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);
		assert CanvasGlHelper.checkError();
	}

	void close() {
		if (fboGlId != -1) {
			GlStateManager.deleteFramebuffers(fboGlId);
			fboGlId = -1;
		}
	}

	public void copyDepthFrom(PipelineFramebuffer source) {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		final Image srcImg = Pipeline.getImage(source.config.depthAttachment.image.name);
		final Image myImg = Pipeline.getImage(config.depthAttachment.image.name);

		if (GlStateManager.supportsGl30()) {
			GlStateManager.bindFramebuffer(GL46.GL_READ_FRAMEBUFFER, source.fboGlId);
			GlStateManager.bindFramebuffer(GL46.GL_DRAW_FRAMEBUFFER, fboGlId);
			GlStateManager.blitFramebuffer(0, 0, srcImg.width, srcImg.height, 0, 0, myImg.width, myImg.height, GL46.GL_DEPTH_BUFFER_BIT, GL46.GL_NEAREST);
		} else {
			if (myImg.glId() != 0) {
				final int saveImg = GlStateManager.getActiveBoundTexture();
				GlStateManager.bindTexture(srcImg.glId());
				GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fboGlId);
				GlStateManager.copyTexSubImage2d(GL46.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
						Math.min(srcImg.width, myImg.width),
						Math.min(srcImg.height, myImg.height));
				GlStateManager.bindTexture(saveImg);
			}
		}

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, 0);
	}
}
