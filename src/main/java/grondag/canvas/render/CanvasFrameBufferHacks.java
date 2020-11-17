/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.render;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.mixinterface.FrameBufferExt;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ProcessShader;
import grondag.canvas.shader.ProcessShaders;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.client.util.InputUtil;

//PERF: handle VAO properly here before re-enabling VAO
public class CanvasFrameBufferHacks {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: CanvasFrameBufferHacks static init");
		}
	}

	static final ProcessShader copy = ProcessShaders.create("canvas:shaders/internal/process/copy", "_cvu_input");
	static final ProcessShader emissiveColor = ProcessShaders.create("canvas:shaders/internal/process/emissive_color", "_cvu_base", "_cvu_extras");
	static final ProcessShader bloom = ProcessShaders.create("canvas:shaders/internal/process/bloom", "_cvu_base", "_cvu_bloom");
	static final ProcessShader copyLod = ProcessShaders.create("canvas:shaders/internal/process/copy_lod", "_cvu_input");
	static final ProcessShader downsample = ProcessShaders.create("canvas:shaders/internal/process/downsample", "_cvu_input");
	static final ProcessShader upsample = ProcessShaders.create("canvas:shaders/internal/process/upsample", "cvu_input", "cvu_prior");
	static final ProcessShader reflectionColor = ProcessShaders.create("canvas:shaders/internal/process/reflection_color", "_cvu_base", "_cvu_extras", "_cvu_normal", "_cvu_depth");
	static final ProcessShader reflection = ProcessShaders.create("canvas:shaders/internal/process/reflection", "_cvu_base", "_cvu_reflection", "_cvu_extras");
	static final ProcessShader depthNormal = ProcessShaders.create("canvas:shaders/internal/process/debug_depth_normal", "_cvu_normal", "_cvu_depth");
	static final int[] ATTACHMENTS_TRIPLE = {FramebufferInfo.COLOR_ATTACHMENT, FramebufferInfo.COLOR_ATTACHMENT + 1, FramebufferInfo.COLOR_ATTACHMENT + 2};
	static Framebuffer mcFbo;
	static FrameBufferExt mcFboExt;
	static int mainFbo = -1;
	static int mainColor;
	static int mainDepth;
	//	static int mainHDR;
	static int texExtras = -1;
	static int texNormal = -1;
	static int texEmissiveColor = -1;
	static int texMainCopy;
	static int texBloomDownsample;
	static int texBloomUpsample;
	static int texReflectionColor = -1;
	static int texReflectionDownsample;
	static int texReflectionUpsample;
	static int canvasFboId = -1;
	static VboBuffer drawBuffer;
	static int h;
	static int w;
	private static int oldTex0;
	private static int oldTex1;
	private static int oldTex2;
	private static int oldTex3;

	private static boolean active = false;

	private static void clearAttachments() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFboId);
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texExtras, 0);
		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texNormal, 0);
		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
	}

	public static void prepareForFrame() {
		assert !active;
		sync();
		clearAttachments();
	}

	public static void startExtrasCapture() {
		if (!active) {
			active = true;
			GL21.glDrawBuffers(ATTACHMENTS_TRIPLE);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, texExtras, 0);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 2, GL21.GL_TEXTURE_2D, texNormal, 0);
		}
	}

	public static void endExtrasCapture() {
		if (active) {
			active = false;
			GL21.glDrawBuffers(FramebufferInfo.COLOR_ATTACHMENT);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, 0, 0);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 2, GL21.GL_TEXTURE_2D, 0, 0);
		}
	}

	private static void startCopy() {
		GlStateManager.activeTexture(GL21.GL_TEXTURE3);
		oldTex3 = GlStateManager.getActiveBoundTexture();
		GlStateManager.activeTexture(GL21.GL_TEXTURE2);
		oldTex2 = GlStateManager.getActiveBoundTexture();
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
		GlStateManager.activeTexture(GL21.GL_TEXTURE3);
		GlStateManager.bindTexture(oldTex3);
		GlStateManager.disableTexture();
		GlStateManager.activeTexture(GL21.GL_TEXTURE2);
		GlStateManager.bindTexture(oldTex2);
		GlStateManager.disableTexture();
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
		GlStateManager.bindTexture(texExtras);
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
		upsample.activate().intensity(1f); // <- reuse uniform. used to multiply sum (for averaging)

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

	public static void applyScreenSpaceReflection(Matrix4f projectionMatrix) {
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

		// Inverse projection matrix to get view space coordinates
		Matrix4f invProjection = new Matrix4f(projectionMatrix);
		invProjection.invert();

		// Draw to intermediate texture
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texReflectionColor, 0);
		reflectionColor.activate().size(w, h).projection(projectionMatrix).invProjection(invProjection);

		GlStateManager.activeTexture(GL21.GL_TEXTURE3);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(mainDepth);
		GlStateManager.activeTexture(GL21.GL_TEXTURE2);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texNormal);
		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texExtras);

		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.bindTexture(mainColor);

		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		// Disable blur for now as it is causing artifacts. May be better to improve the roughness
		// function instead as the main purpose of blur was to improve the look of rough surfaces.

//		// build reflection mipmaps, blurring as part of downscale
//		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texReflectionDownsample, 0);
//		GlStateManager.bindTexture(texReflectionColor);
//		downsample.activate().distance(1f, 1f).size(w, h).lod(0);
//		setProjection(w, h);
//		RenderSystem.viewport(0, 0, w, h);
//		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
//
//		GlStateManager.bindTexture(texReflectionDownsample);
//
//		for (int d = 1; d <= 6; ++d) {
//			final int sw = (w >> d);
//			final int sh = (h >> d);
//			downsample.size(sw, sh).lod(d - 1);
//			setProjection(sw, sh);
//			RenderSystem.viewport(0, 0, sw, sh);
//			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texReflectionDownsample, d);
//			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
//		}
//
//		// upscale reflection mipmaps, bluring again as we go
//		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
//		GlStateManager.enableTexture();
//		GlStateManager.bindTexture(texReflectionUpsample);
//		upsample.activate().intensity(0.5f); // <- reuse uniform. used to multiply sum (for averaging)
//
//		for (int d = 6; d >= 0; --d) {
//			final int sw = (w >> d);
//			final int sh = (h >> d);
//			upsample.distance(10, 10).size(sw, sh).lod(d);
//			setProjection(sw, sh);
//			RenderSystem.viewport(0, 0, sw, sh);
//			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texReflectionUpsample, d);
//			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
//		}

		// Switch back to MC fbo to draw combined color + reflection color
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
		RenderSystem.viewport(0, 0, w, h);
		reflection.activate().size(w, h);
		setProjection(w, h);

		GlStateManager.activeTexture(GL21.GL_TEXTURE2);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texExtras);

		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texReflectionColor);

		// Framebuffer attachment shouldn't draw to self so use copy created earlier
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.bindTexture(texMainCopy);

		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	public static void debugDepthNormal() {
		final long handle = MinecraftClient.getInstance().getWindow().getHandle();
		boolean depth = (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT));

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFboId);
		startCopy();
		drawBuffer.bind();
		RenderSystem.viewport(0, 0, w, h);
		setProjection(w, h);

		// Draw to copy
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, texMainCopy, 0);
		// Reuse uniform
		if (depth) {
			depthNormal.activate().size(w, h).intensity(1.0f).distance(1, 512).activate();
		} else {
			depthNormal.activate().size(w, h).intensity(0.0f).activate();
		}
		GlStateManager.activeTexture(GL21.GL_TEXTURE1);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(mainDepth);
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texNormal);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		// Copy result into main fbo
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
		copy.activate().size(w, h);
		GlStateManager.bindTexture(texMainCopy);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	public static void debugEmissive() {
		startCopy();
		drawBuffer.bind();
		RenderSystem.viewport(0, 0, w, h);
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(texBloomDownsample);

		final long handle = MinecraftClient.getInstance().getWindow().getHandle();

		if ((InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT))) {
			GlStateManager.bindTexture(texEmissiveColor);
		} else {
			GlStateManager.bindTexture(texExtras);
		}

		setProjection(w, h);
		copyLod.activate().size(w, h).lod(0).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	public static void debugBlur(int level) {
		startCopy();
		drawBuffer.bind();
		RenderSystem.viewport(0, 0, w, h);

		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		final long handle = MinecraftClient.getInstance().getWindow().getHandle();

		if ((InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT))) {
			GlStateManager.bindTexture(texBloomDownsample);
		} else {
			GlStateManager.bindTexture(texBloomUpsample);
		}

		setProjection(w, h);
		copyLod.activate().size(w, h).lod(level);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	private static void setupBlurLod() {
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LEVEL, 6);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_LOD, 0);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LOD, 6);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_LINEAR_MIPMAP_NEAREST);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_LINEAR);

		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 1, GL21.GL_RGBA8, w >> 1, h >> 1, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 2, GL21.GL_RGBA8, w >> 2, h >> 2, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 3, GL21.GL_RGBA8, w >> 3, h >> 3, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 4, GL21.GL_RGBA8, w >> 4, h >> 4, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 5, GL21.GL_RGBA8, w >> 5, h >> 5, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 6, GL21.GL_RGBA8, w >> 6, h >> 6, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
	}

	private static void sync() {
		assert RenderSystem.isOnRenderThread();

		mcFbo = MinecraftClient.getInstance().getFramebuffer();
		mcFboExt = ((FrameBufferExt) mcFbo);

		if (mcFboExt.canvas_colorAttachment() != mainColor || mcFbo.textureHeight != h || mcFbo.textureWidth != w) {
			tearDown();
			mainFbo = mcFbo.fbo;

			canvasFboId = GlStateManager.genFramebuffers();

			mainColor = mcFboExt.canvas_colorAttachment();
			mainDepth = mcFboExt.canvas_depthAttachment();

			w = mcFbo.textureWidth;
			h = mcFbo.textureHeight;

			//			mainHDR = createColorAttachment(w, h, true);
			texExtras = createColorAttachment(w, h);
			texNormal = createColorAttachment(w, h);

			texMainCopy = createColorAttachment(w, h);

			texEmissiveColor = createColorAttachment(w, h);
			texBloomDownsample = createColorAttachment(w, h);
			GlStateManager.bindTexture(texBloomDownsample);
			setupBlurLod();

			texBloomUpsample = createColorAttachment(w, h);
			GlStateManager.bindTexture(texBloomUpsample);
			setupBlurLod();

			texReflectionColor = createColorAttachment(w, h);
			texReflectionDownsample = createColorAttachment(w, h);
			GlStateManager.bindTexture(texReflectionDownsample);
			setupBlurLod();

			texReflectionUpsample = createColorAttachment(w, h);
			GlStateManager.bindTexture(texReflectionUpsample);
			setupBlurLod();

			// don't want filtering when copy back from main
			GlStateManager.bindTexture(texMainCopy);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);

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

	private static int createColorAttachment(int width, int height) {
		return createColorAttachment(width, height, false);
	}

	private static int createColorAttachment(int width, int height, boolean hdr) {
		final int result = TextureUtil.generateId();
		GlStateManager.bindTexture(result);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_LINEAR);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_LINEAR);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_S, GL21.GL_CLAMP);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_T, GL21.GL_CLAMP);

		if (hdr) {
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, ARBTextureFloat.GL_RGBA16F_ARB, width, height, 0, GL21.GL_RGBA, GL21.GL_FLOAT, null);
		} else {
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, width, height, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, null);
		}

		return result;
	}

	private static void tearDown() {
		if (texExtras != -1) {
			TextureUtil.deleteId(texExtras);
			TextureUtil.deleteId(texMainCopy);
			texExtras = -1;
		}

		if (texEmissiveColor != -1) {
			TextureUtil.deleteId(texEmissiveColor);
			//			TextureUtil.deleteId(mainHDR);
			TextureUtil.deleteId(texBloomDownsample);
			TextureUtil.deleteId(texBloomUpsample);
			texEmissiveColor = -1;
		}

		if (texReflectionColor != -1) {
			TextureUtil.deleteId(texReflectionColor);
			TextureUtil.deleteId(texReflectionDownsample);
			TextureUtil.deleteId(texReflectionUpsample);
			texReflectionColor = -1;
		}

		if (texNormal != -1) {
			TextureUtil.deleteId(texNormal);
			texNormal = -1;
		}

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
