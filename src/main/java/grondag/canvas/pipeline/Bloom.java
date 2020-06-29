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
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.TextureUtil;

import grondag.canvas.buffer.allocation.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ProcessShaders;

public class Bloom {

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

	static int clearFbo = -1;

	static VboBuffer drawBuffer;

	static int h;
	static int w;

	static final int[] captureBuffers = {FramebufferInfo.COLOR_ATTACHMENT, FramebufferInfo.COLOR_ATTACHMENT + 1};

	private static void clearBloom() {
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, clearFbo);
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
	}

	public static void startBloom(boolean first) {
		if (first) {
			sync();
			clearBloom();
		}

		GL21.glDrawBuffers(captureBuffers);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, full, 0);
	}

	public static void endBloom() {
		GL21.glDrawBuffers(FramebufferInfo.COLOR_ATTACHMENT);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, 0, 0);
		//		debugBase();
	}

	// TODO: remove or make it a config option for testing
	public static void debugBase() {
		final int oldTex = GlStateManager.getActiveBoundTexture();

		RenderSystem.viewport(w / 2, h / 2, w / 2, h / 2);
		RenderSystem.depthMask(false);
		RenderSystem.disableBlend();
		RenderSystem.disableCull();
		RenderSystem.disableAlphaTest();
		RenderSystem.disableDepthTest();
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		RenderSystem.pushMatrix();
		RenderSystem.loadIdentity();
		//		RenderSystem.shadeModel(GL21.GL_FLAT);
		GlStateManager.ortho(0.0D, w, h, 0.0D, 1000.0, 3000.0);

		// FIX: handle VAO properly here before re-enabling
		drawBuffer.bind();
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(full);
		ProcessShaders.copy().activate();
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);
		VboBuffer.unbind();
		GlStateManager.bindTexture(0);
		//		GlStateManager.disableTexture();

		GlStateManager.bindTexture(oldTex);
		GlProgram.deactivate();
		RenderSystem.popMatrix();
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();

		RenderSystem.viewport(0, 0, w, h);
	}

	private static void sync() {
		assert RenderSystem.isOnRenderThread();

		fbo = MinecraftClient.getInstance().getFramebuffer();

		if (fbo.colorAttachment != mainColor || fbo.textureHeight != h || fbo.textureWidth != w) {
			tearDown();
			mainFbo = fbo.fbo;
			full = TextureUtil.generateId();
			fullBlur = TextureUtil.generateId();
			half = TextureUtil.generateId();
			halfBlur = TextureUtil.generateId();
			quarter = TextureUtil.generateId();
			quarterBlur = TextureUtil.generateId();

			clearFbo = GlStateManager.genFramebuffers();


			mainColor = fbo.colorAttachment;

			w = fbo.textureWidth;
			h = fbo.textureHeight;

			GlStateManager.bindTexture(full);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_NEAREST);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_NEAREST);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_S, GL21.GL_CLAMP);
			GlStateManager.texParameter(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_WRAP_T, GL21.GL_CLAMP);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w, h, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);

			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, clearFbo);
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, ARBFramebufferObject.GL_COLOR_ATTACHMENT0, GL21.GL_TEXTURE_2D, full, 0);

			GlStateManager.viewport(0, 0, w, h);
			GlStateManager.clearColor(0, 0, 0, 0);
			GlStateManager.clear(GL21.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);

			GlStateManager.bindTexture(fullBlur);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w, h, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);

			GlStateManager.bindTexture(half);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 2, h / 2, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
			GlStateManager.bindTexture(halfBlur);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 2, h / 2, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);

			GlStateManager.bindTexture(quarter);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 4, h / 4, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
			GlStateManager.bindTexture(quarterBlur);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 4, h / 4, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);

			GlStateManager.bindTexture(0);

			final VertexCollectorImpl collector = new VertexCollectorImpl();
			collector.addf(0, 0, 0.2f, 0, 1f);
			collector.addf(w, 0, 0.2f, 1f, 1f);
			collector.addf(w, h, 0.2f, 1f, 0f);
			collector.addf(0, h, 0.2f, 0, 0f);

			drawBuffer = new VboBuffer(collector.byteSize(), MaterialVertexFormats.PROCESS_VERTEX_UV);
			collector.toBuffer(drawBuffer.intBuffer());
			drawBuffer.upload();
		}
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

		if (drawBuffer != null) {
			drawBuffer.close();
			drawBuffer = null;
		}

		if (clearFbo > -1) {
			GlStateManager.deleteFramebuffers(clearFbo);
			clearFbo = -1;
		}
	}
}
