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
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.InputUtil;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.mixinterface.FrameBufferExt;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ProcessShader;
import grondag.canvas.shader.ProcessShaders;

//PERF: handle VAO properly here before re-enabling VAO
public class CanvasFrameBufferHacks {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasFrameBufferHacks static init");
		}
	}

	static final ProcessShader copy = ProcessShaders.create("canvas:shaders/internal/process/copy", "_cvu_input");
	static final ProcessShader emissiveColor = ProcessShaders.create("canvas:shaders/internal/process/emissive_color", "_cvu_base", "_cvu_emissive");
	static final ProcessShader bloom = ProcessShaders.create("canvas:shaders/internal/process/bloom", "_cvu_base", "_cvu_bloom");
	static final ProcessShader copyLod = ProcessShaders.create("canvas:shaders/internal/process/copy_lod", "_cvu_input");
	static final ProcessShader downsample = ProcessShaders.create("canvas:shaders/internal/process/downsample", "_cvu_input");
	static final ProcessShader upsample = ProcessShaders.create("canvas:shaders/internal/process/upsample", "cvu_input", "cvu_prior");
	static final int[] ATTACHMENTS_DOUBLE = {FramebufferInfo.COLOR_ATTACHMENT, FramebufferInfo.COLOR_ATTACHMENT + 1};
	static Framebuffer mcFbo;
	static FrameBufferExt mcFboExt;
	static int mainFbo = -1;
	static int mainColor;
	//	static int mainHDR;
	static int texEmissive = -1;
	static int texEmissiveColor;
	static int texMainCopy;
	static int texBloomDownsample;
	static int texBloomUpsample;
	static int canvasFboId = -1;
	static VboBuffer drawBuffer;
	static int h;
	static int w;
	private static int oldTex0;
	private static int oldTex1;

	private static boolean active = false;

	private static void clearAttachments() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFboId);
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texEmissive, 0);
		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
	}

	public static void prepareForFrame() {
		assert !active;
		sync();
		clearAttachments();
	}

	public static void startEmissiveCapture() {
		if (!active) {
			active = true;
			GL21.glDrawBuffers(ATTACHMENTS_DOUBLE);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, texEmissive, 0);
		}
	}

	public static void endEmissiveCapture() {
		if (active) {
			active = false;
			GL21.glDrawBuffers(FramebufferInfo.COLOR_ATTACHMENT);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, 0, 0);
		}
	}

	static void startCopy() {
		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		oldTex1 = GlStateManager.getActiveBoundTexture();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		oldTex0 = GlStateManager.getActiveBoundTexture();

		RenderSystem.depthMask(false);
		RenderSystem.disableBlend();
		RenderSystem.disableCull();
		RenderSystem.disableAlphaTest();
		RenderSystem.disableDepthTest();
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		RenderSystem.pushMatrix();
	}

	private static void setProjection(int pixelWidth, int pixelHeight) {
		RenderSystem.loadIdentity();
		GlStateManager.ortho(0.0D, pixelWidth, pixelHeight, 0.0D, 1000.0, 3000.0);
	}

	private static void endCopy() {
		VboBuffer.unbind();
		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		GlStateManager.bindTexture(oldTex1);
		GlStateManager.disableTexture();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.bindTexture(oldTex0);
		GlProgram.deactivate();
		RenderSystem.popMatrix();
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.viewport(0, 0, w, h);
	}

	// Based on approach described by Jorge Jiminez, 2014
	// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
	public static void applyBloom() {
		if (Configurator.enableBufferDebug && BufferDebug.current() == BufferDebug.NORMAL) {
			final long handle = MinecraftClient.getInstance().getWindow().getHandle();

			if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
				return;
			}
		}

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFboId);
		startCopy();

		drawBuffer.bind();

		setProjection(w, h);
		RenderSystem.viewport(0, 0, w, h);

		// copy MC fbo color attachment - need it at end for combine step
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texMainCopy, 0);
		GlStateManager.bindTexture(mainColor);
		copy.activate().size(w, h);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		// select emissive portions for blur
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texEmissiveColor, 0);
		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texEmissive);
		emissiveColor.activate().size(w, h);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
		GlStateManager.bindTexture(0);
		GlStateManager.disableTexture();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);

		// build bloom mipmaps, blurring as part of downscale
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texBloomDownsample, 0);
		GlStateManager.bindTexture(texEmissiveColor);
		downsample.activate().distance(1f, 1f).size(w, h).lod(0);
		setProjection(w, h);
		RenderSystem.viewport(0, 0, w, h);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		GlStateManager.bindTexture(texBloomDownsample);

		for (int d = 1; d <= 6; ++d) {
			final int sw = (w >> d);
			final int sh = (h >> d);
			downsample.size(sw, sh).lod(d - 1);
			setProjection(sw, sh);
			RenderSystem.viewport(0, 0, sw, sh);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texBloomDownsample, d);
			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
		}

		final float bloomScale = Configurator.bloomScale;

		// upscale bloom mipmaps, bluring again as we go
		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texBloomUpsample);
		upsample.activate();

		for (int d = 6; d >= 0; --d) {
			final int sw = (w >> d);
			final int sh = (h >> d);
			upsample.distance(bloomScale, bloomScale).size(sw, sh).lod(d);
			setProjection(sw, sh);
			RenderSystem.viewport(0, 0, sw, sh);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texBloomUpsample, d);
			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
		}

		// Switch back to MC fbo to draw combined color + bloom
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
		RenderSystem.viewport(0, 0, w, h);
		bloom.activate().size(w, h).distance(bloomScale, bloomScale).intensity(Configurator.bloomIntensity);
		setProjection(w, h);

		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texBloomUpsample);

		// Framebuffer attachment shouldn't draw to self so use copy created earlier
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.bindTexture(texMainCopy);

		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	static void renderDebug(int mainId, int sneakId, int lod) {
		startCopy();
		drawBuffer.bind();
		RenderSystem.viewport(0, 0, w, h);
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texBloomDownsample);

		final long handle = MinecraftClient.getInstance().getWindow().getHandle();

		if ((InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT))) {
			GlStateManager.bindTexture(sneakId);
		} else {
			GlStateManager.bindTexture(mainId);
		}

		setProjection(w, h);
		copyLod.activate().size(w, h).lod(lod).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	private static void sync() {
		assert RenderSystem.isOnRenderThread();

		mcFbo = MinecraftClient.getInstance().getFramebuffer();
		mcFboExt = ((FrameBufferExt) mcFbo);

		if (Pipeline.needsReload() || mcFboExt.canvas_colorAttachment() != mainColor || mcFbo.textureHeight != h || mcFbo.textureWidth != w) {
			Pipeline.close();
			tearDown();
			mainFbo = mcFbo.fbo;

			canvasFboId = GlStateManager.genFramebuffers();

			mainColor = mcFboExt.canvas_colorAttachment();

			w = mcFbo.textureWidth;
			h = mcFbo.textureHeight;

			Pipeline.activate(w, h);

			//			mainHDR = createColorAttachment(w, h, true);
			texEmissive = Pipeline.getImage(PipelineConfig.IMG_EMISSIVE).glId();
			texEmissiveColor = Pipeline.getImage(PipelineConfig.IMG_EMISSIVE_COLOR).glId();
			texMainCopy = Pipeline.getImage(PipelineConfig.IMG_MAIN_COPY).glId();
			texBloomDownsample = Pipeline.getImage(PipelineConfig.IMG_BLOOM_DOWNSAMPLE).glId();
			texBloomUpsample = Pipeline.getImage(PipelineConfig.IMG_BLOOM_UPSAMPLE).glId();

			GlStateManager.bindTexture(0);

			final VertexCollectorImpl collector = new VertexCollectorImpl();
			collector.add(0f, 0f, 0.2f, 0, 1f);
			collector.add(1f, 0f, 0.2f, 1f, 1f);
			collector.add(1f, 1f, 0.2f, 1f, 0f);
			collector.add(0f, 1f, 0.2f, 0f, 0f);

			drawBuffer = new VboBuffer(collector.byteSize(), CanvasVertexFormats.PROCESS_VERTEX_UV);
			collector.toBuffer(drawBuffer.intBuffer());
			drawBuffer.upload();

			collector.clear(); // releases storage
		}
	}

	private static void tearDown() {
		if (drawBuffer != null) {
			drawBuffer.close();
			drawBuffer = null;
		}

		if (canvasFboId > -1) {
			GlStateManager.deleteFramebuffers(canvasFboId);
			canvasFboId = -1;
		}
	}
}
