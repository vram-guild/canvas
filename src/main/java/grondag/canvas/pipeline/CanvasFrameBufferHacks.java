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
	static int full  = -1;
	static int fullBlur;
	static int half;
	static int halfBlur;
	static int quarter;
	static int quarterBlur;

	static int canvasFbo = -1;

	static VboBuffer copyVbo;

	static int h;
	static int w;

	static final int[] ATTACHMENTS_DOUBLE = {FramebufferInfo.COLOR_ATTACHMENT, FramebufferInfo.COLOR_ATTACHMENT + 1};

	private static void clearAttachments() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, canvasFbo);
		GlStateManager.clearColor(0, 0, 0, 0);
		//		GL21.glDrawBuffers(ATTACHMENTS_DOUBLE);

		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, full, 0);
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
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, full, 0);
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
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, half, 0);
		startCopy();

		// FIX: handle VAO properly here before re-enabling
		copyVbo.bind();

		RenderSystem.viewport(0, 0, w / 2, h / 2);
		GlStateManager.bindTexture(full);
		ProcessShaders.copy(w, h).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL21.GL_TEXTURE_2D, quarter, 0);
		RenderSystem.viewport(0, 0, w / 4, h / 4);
		GlStateManager.bindTexture(full);
		//		ProcessShaders.copyResize(w / 4, h / 4);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
	}

	public static void debugEmissive() {
		startCopy();
		RenderSystem.viewport(w / 2, h / 2, w / 2, h / 2);

		// FIX: handle VAO properly here before re-enabling
		copyVbo.bind();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(full);
		ProcessShaders.copy(w, h).activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
	}

	public static void debugEmissiveCascade() {
		startCopy();

		// FIX: handle VAO properly here before re-enabling
		copyVbo.bind();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(full);
		ProcessShaders.copy(w, h).activate();
		RenderSystem.viewport(w / 2, h / 2, w / 2, h / 2);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		RenderSystem.viewport(w * 3 / 4, h * 3 / 4, w / 4, h / 4);
		GlStateManager.bindTexture(half);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		RenderSystem.viewport(w * 7 / 8, h * 7 / 8, w / 8, h / 8);
		GlStateManager.bindTexture(quarter);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endCopy();
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

			full = createColorAttachment(w, h);
			fullBlur = createColorAttachment(w, h);
			half = createColorAttachment(w / 2, h / 2);
			halfBlur = createColorAttachment(w / 2, h / 2);
			quarter = createColorAttachment(w / 4, h / 4);
			quarterBlur = createColorAttachment(w / 4, h / 4);

			GlStateManager.bindTexture(0);

			final VertexCollectorImpl collector = new VertexCollectorImpl();
			collector.addf(0, 0, 0.2f, 0, 1f);
			collector.addf(1f, 0, 0.2f, 1f, 1f);
			collector.addf(1f, 1f, 0.2f, 1f, 0f);
			collector.addf(0, 1f, 0.2f, 0, 0f);

			copyVbo = new VboBuffer(collector.byteSize(), MaterialVertexFormats.PROCESS_VERTEX_UV);
			collector.toBuffer(copyVbo.intBuffer());
			copyVbo.upload();
		}
	}

	private static int createColorAttachment(int width, int height) {
		final int result = TextureUtil.generateId();
		GlStateManager.bindTexture(result);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_S, GL21.GL_CLAMP);
		GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_T, GL21.GL_CLAMP);
		GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, width, height, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
		return result;
	}

	private static void tearDown() {
		if (full != -1) {
			TextureUtil.deleteId(full);
			TextureUtil.deleteId(fullBlur);
			TextureUtil.deleteId(half);
			TextureUtil.deleteId(halfBlur);
			TextureUtil.deleteId(quarter);
			TextureUtil.deleteId(quarterBlur);
			full = -1;
		}

		if (copyVbo != null) {
			copyVbo.close();
			copyVbo = null;
		}

		if (canvasFbo > -1) {
			GlStateManager.deleteFramebuffers(canvasFbo);
			canvasFbo = -1;
		}
	}
}
