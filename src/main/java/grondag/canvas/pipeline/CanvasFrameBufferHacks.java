/*******************************************************************************
 * Copyright 2020 grondag
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

package grondag.canvas.pipeline;

import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.TextureUtil;

import grondag.canvas.buffer.allocation.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.GlProgram;

//FIX: handle VAO properly here before re-enabling VAO
public class CanvasFrameBufferHacks {

	static final ProcessShader copy = ProcessShaders.create("canvas:shaders/internal/process/copy", "_cvu_input");
	static final ProcessShader bloom = ProcessShaders.create("canvas:shaders/internal/process/bloom", "_cvu_base", "_cvu_bloom");
	static final ProcessShader copyLod = ProcessShaders.create("canvas:shaders/internal/process/copy_lod", "_cvu_input");
	static final ProcessShader blurLod = ProcessShaders.create("canvas:shaders/internal/process/blur_lod", "_cvu_input");

	// TODO: tear down
	static Framebuffer fbo;
	static int mainFbo = -1;
	static int mainColor;

	//	static int mainHDR;
	static int emissive  = -1;
	static int mainCopy;

	static int bloomCascade;
	static int bloomCascadeSwap;

	static int canvasFbo = -1;

	static VboBuffer vboFull;

	static int h;
	static int w;

	static final int[] ATTACHMENTS_DOUBLE = {FramebufferInfo.COLOR_ATTACHMENT, FramebufferInfo.COLOR_ATTACHMENT + 1};

	private static void clearAttachments() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFbo);
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, emissive, 0);
		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
	}

	public static void startEmissiveCapture(boolean first) {
		if (first) {
			sync();
			clearAttachments();
		}

		GL21.glDrawBuffers(ATTACHMENTS_DOUBLE);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, emissive, 0);
	}

	public static void endEmissiveCapture() {
		GL21.glDrawBuffers(FramebufferInfo.COLOR_ATTACHMENT);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, 0, 0);
	}

	private static int oldTex;

	private static void startCopy() {
		oldTex = GlStateManager.getActiveBoundTexture();
		RenderSystem.depthMask(false);
		RenderSystem.disableBlend();
		RenderSystem.disableCull();
		RenderSystem.disableAlphaTest();
		RenderSystem.disableDepthTest();
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		RenderSystem.pushMatrix();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
	}

	private static void setProjection(int pixelWidth, int pixelHeight) {
		RenderSystem.loadIdentity();
		GlStateManager.ortho(0.0D, pixelWidth, pixelHeight, 0.0D, 1000.0, 3000.0);
	}

	private static void endCopy() {
		VboBuffer.unbind();
		GlStateManager.bindTexture(0);
		GlStateManager.bindTexture(oldTex);
		GlProgram.deactivate();
		RenderSystem.popMatrix();
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.viewport(0, 0, w, h);
	}

	public static void applyBloom() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFbo);
		startCopy();

		vboFull.bind();

		setProjection(w, h);
		RenderSystem.viewport(0, 0, w, h);

		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, mainCopy, 0);
		GlStateManager.bindTexture(mainColor);
		copy.activate().size(w, h);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		// build mip map
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, bloomCascade, 0);
		GlStateManager.bindTexture(emissive);
		copyLod.activate().size(w >> 1, h >> 1).lod(0);
		setProjection(w >> 1, h >> 1);
		RenderSystem.viewport(0, 0, w >> 1, h >> 1);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		GlStateManager.bindTexture(bloomCascade);

		for (int d = 1; d <= 7; ++d) {
			final int sw = (w >> (d + 1));
			final int sh = (h >> (d + 1));
			copyLod.size(sw, sh).lod(d - 1);
			setProjection(sw, sh);
			RenderSystem.viewport(0, 0, sw, sh);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, bloomCascade, d);
			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
		}

		if (!BufferDebug.shouldSkipBlur()) {

			blurLod.activate().distance(0.5f, 0).size(w / 2, h / 2).lod(0).activate();

			for (int d = 0; d <= 7; ++d) {
				final int sw = (w >> (d + 1));
				final int sh = (h >> (d + 1));
				blurLod.distance(0.5f, 0).size(sw, sh).lod(d);
				setProjection(sw, sh);
				RenderSystem.viewport(0, 0, sw, sh);
				GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, bloomCascadeSwap, d);
				//				GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
			}

			GlStateManager.bindTexture(bloomCascadeSwap);

			for (int d = 0; d <= 7; ++d) {
				final int sw = (w >> (d + 1));
				final int sh = (h >> (d + 1));
				blurLod.distance(0, 0.5f).size(sw, sh).lod(d);
				setProjection(sw, sh);
				RenderSystem.viewport(0, 0, sw, sh);
				GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, bloomCascade, d);
				//				GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
			}

			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
			RenderSystem.viewport(0, 0, w, h);
			bloom.activate().size(w, h);
			setProjection(w, h);

			GlStateManager.activeTexture(GL21.GL_TEXTURE0);
			GlStateManager.enableTexture();
			GlStateManager.bindTexture(mainCopy);

			GlStateManager.activeTexture(GL21.GL_TEXTURE1);
			GlStateManager.enableTexture();
			GlStateManager.bindTexture(bloomCascade);

			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

			GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		} else {
			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
			RenderSystem.viewport(0, 0, w, h);
		}

		endCopy();

	}

	public static void debugEmissive() {
		startCopy();
		vboFull.bind();
		RenderSystem.viewport(w / 2, h / 2, w / 2, h / 2);
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(bloomCascade);
		setProjection(w >> 1, h >> 1);
		copyLod.activate().size(w >> 1, h >> 1).lod(0).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	public static void debugEmissiveCascade() {
		startCopy();
		vboFull.bind();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(bloomCascade);
		copyLod.activate().size(w, h).lod(0).activate();

		for (int lod = 0; lod <= 7; ++lod) {
			final int dw = (w >> (lod + 1));
			final int dh = (h >> (lod + 1));
			RenderSystem.viewport(w - dw, h -  dh, dw, dh);
			setProjection(dw, dh);
			copyLod.size(dw, dh).lod(lod);
			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
		}
		endCopy();
	}

	public static void debugBlur(int level) {
		startCopy();
		vboFull.bind();
		RenderSystem.viewport(0, 0, w, h);
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(bloomCascade);
		setProjection(w, h);
		copyLod.activate().size(w, h).lod(level);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	private static void setupBloomLod() {
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LEVEL, 7);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_LOD, 0);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAX_LOD, 7);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_LINEAR_MIPMAP_NEAREST);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_LINEAR_MIPMAP_NEAREST);

		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 1, GL21.GL_RGBA8, w >> 2, h >> 2, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 2, GL21.GL_RGBA8, w >> 3, h >> 3, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 3, GL21.GL_RGBA8, w >> 4, h >> 4, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 4, GL21.GL_RGBA8, w >> 5, h >> 5, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 5, GL21.GL_RGBA8, w >> 6, h >> 6, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 6, GL21.GL_RGBA8, w >> 7, h >> 7, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 7, GL21.GL_RGBA8, w >> 8, h >> 8, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
	}

	private static void sync() {
		assert RenderSystem.isOnRenderThread();

		fbo = MinecraftClient.getInstance().getFramebuffer();

		if (fbo.colorAttachment != mainColor || fbo.textureHeight != h || fbo.textureWidth != w) {
			tearDown();
			mainFbo = fbo.fbo;

			canvasFbo = GlStateManager.genFramebuffers();

			mainColor = fbo.colorAttachment;

			w = fbo.textureWidth;
			h = fbo.textureHeight;

			//			mainHDR = createColorAttachment(w, h, true);
			emissive = createColorAttachment(w, h);
			mainCopy = createColorAttachment(w, h);

			bloomCascade = createColorAttachment(w / 2, h / 2);
			GlStateManager.bindTexture(bloomCascade);
			setupBloomLod();

			bloomCascadeSwap = createColorAttachment(w / 2, h / 2);
			GlStateManager.bindTexture(bloomCascadeSwap);
			setupBloomLod();


			// don't want filtering when copy back from main   TODO: confirm
			GlStateManager.bindTexture(mainCopy);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);

			GlStateManager.bindTexture(0);

			final VertexCollectorImpl collector = new VertexCollectorImpl();
			collector.addf(0, 0, 0.2f, 0, 1f);
			collector.addf(1f, 0, 0.2f, 1f, 1f);
			collector.addf(1f, 1f, 0.2f, 1f, 0f);
			collector.addf(0, 1f, 0.2f, 0, 0f);

			vboFull = new VboBuffer(collector.byteSize(), MaterialVertexFormats.PROCESS_VERTEX_UV);
			collector.toBuffer(vboFull.intBuffer());
			vboFull.upload();
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


		if  (hdr) {
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, ARBTextureFloat.GL_RGBA16F_ARB, width, height, 0, GL21.GL_RGBA, GL21.GL_FLOAT, (IntBuffer)null);
		} else {
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, width, height, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		}
		return result;
	}

	private static void tearDown() {
		if (emissive != -1) {
			TextureUtil.deleteId(emissive);
			//			TextureUtil.deleteId(mainHDR);
			TextureUtil.deleteId(mainCopy);
			TextureUtil.deleteId(bloomCascade);
			TextureUtil.deleteId(bloomCascadeSwap);
			emissive = -1;
		}

		if (vboFull != null) {
			vboFull.close();
			vboFull = null;
		}

		if (canvasFbo > -1) {
			GlStateManager.deleteFramebuffers(canvasFbo);
			canvasFbo = -1;
		}
	}
}
