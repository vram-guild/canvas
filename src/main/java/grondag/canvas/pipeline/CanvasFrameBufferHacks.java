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

public class CanvasFrameBufferHacks {

	// TODO: tear down
	static Framebuffer fbo;
	static int mainFbo = -1;
	static int mainColor;

	//	static int mainHDR;
	static int emissive  = -1;
	static int mainCopy;

	static int bloomSample;
	static int bloomBlur;

	static int canvasFbo = -1;

	static VboBuffer vboFull;

	static int h;
	static int w;

	static final int[] ATTACHMENTS_DOUBLE = {FramebufferInfo.COLOR_ATTACHMENT, FramebufferInfo.COLOR_ATTACHMENT + 1};

	private static void clearAttachments() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFbo);
		GlStateManager.clearColor(0, 0, 0, 0);
		//		GL21.glDrawBuffers(ATTACHMENTS_DOUBLE);

		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, emissive, 0);
		//		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, fullBlur, 0);
		//		GlStateManager.viewport(0, 0, w, h);
		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);

		//		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, half, 0);
		//		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, halfBlur, 0);
		//		GlStateManager.viewport(0, 0, w / 2, h / 2);
		//		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		//
		//		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, quarter, 0);
		//		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, quarterBlur, 0);
		//		GlStateManager.viewport(0, 0, w / 4, h / 4);
		//		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		//
		//		GL21.glDrawBuffers(FramebufferInfo.COLOR_ATTACHMENT);
		//		GlStateManager.viewport(0, 0, w, h);
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
		RenderSystem.loadIdentity();
		GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0, 3000.0);
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
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

		// FIX: handle VAO properly here before re-enabling
		vboFull.bind();

		RenderSystem.viewport(0, 0, w, h);

		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, bloomSample, 0);
		GlStateManager.bindTexture(emissive);
		ProcessShaders.bloomSample(w, h).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, mainCopy, 0);
		GlStateManager.bindTexture(mainColor);
		ProcessShaders.copy(w, h).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		if (!BufferDebug.shouldSkipBlur()) {

			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, bloomBlur, 0);
			RenderSystem.viewport(0, 0, w, h);
			GlStateManager.bindTexture(bloomSample);
			ProcessShaders.blur(0.5f, 0, w, h).activate();
			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, bloomSample, 0);
			RenderSystem.viewport(0, 0, w, h);
			GlStateManager.bindTexture(bloomBlur);
			ProcessShaders.blurResize(0, 0.5f, w, h);
			GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
			RenderSystem.viewport(0, 0, w, h);
			ProcessShaders.bloom(w, h).activate();

			GlStateManager.activeTexture(GL21.GL_TEXTURE0);
			GlStateManager.enableTexture();
			GlStateManager.bindTexture(mainCopy);

			GlStateManager.activeTexture(GL21.GL_TEXTURE1);
			GlStateManager.enableTexture();
			GlStateManager.bindTexture(bloomSample);

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
		RenderSystem.viewport(w / 2, h / 2, w / 2, h / 2);

		// FIX: handle VAO properly here before re-enabling
		vboFull.bind();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(emissive);
		ProcessShaders.copy(w, h).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	public static void debugEmissiveCascade() {
		startCopy();
		RenderSystem.viewport(w / 2, h / 2, w / 2, h / 2);

		// FIX: handle VAO properly here before re-enabling
		vboFull.bind();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(bloomSample);
		ProcessShaders.copy(w, h).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	public static void debugBlur() {
		debugEmissiveCascade();
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
			bloomSample = createColorAttachment(w, h);
			bloomBlur = createColorAttachment(w, h);

			// don't want filtering when copy back from main
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
			TextureUtil.deleteId(bloomSample);
			TextureUtil.deleteId(bloomBlur);
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
