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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.light.LightmapHd;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.mixinterface.FrameBufferExt;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.MaterialProgramManager;
import grondag.canvas.shader.ProcessShader;
import grondag.canvas.terrain.util.TerrainModelSpace;

//PERF: handle VAO properly here before re-enabling VAO
public class PipelineManager {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: PipelineManager static init");
		}
	}

	static ProcessShader debugShader;

	static final int[] ATTACHMENTS_DOUBLE = {FramebufferInfo.COLOR_ATTACHMENT, FramebufferInfo.COLOR_ATTACHMENT + 1};
	static Framebuffer mcFbo;
	static int mainFbo = -1;
	static int mainColor;
	static int texEmissive = -1;
	static int canvasFboId = -1;
	static VboBuffer drawBuffer;
	static int h;
	static int w;
	private static int oldTex0;
	private static int oldTex1;

	private static boolean active = false;

	public static void prepareForFrame() {
		handleRecompile();

		assert !active;
		sync();

		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : Pipeline.onWorldRenderStart) {
			pass.run(w, h);
		}

		endFullFrameRender();

		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
	}

	public static void enableCanvasPrimaryFramebuffer() {
		if (!active) {
			active = true;

			// WIP: remove
			if (MinecraftClient.isFabulousGraphicsOrBetter()) {
				GL21.glDrawBuffers(ATTACHMENTS_DOUBLE);
				GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, texEmissive, 0);
			}
		}
	}

	public static void disableCanvasPrimaryFrambuffer() {
		if (active) {
			active = false;

			// WIP: remove
			if (MinecraftClient.isFabulousGraphicsOrBetter()) {
				GL21.glDrawBuffers(FramebufferInfo.COLOR_ATTACHMENT);
				GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT + 1, GL21.GL_TEXTURE_2D, 0, 0);
			}
		}
	}

	public static void handleRecompile() {
		boolean doIt = false;

		while (CanvasMod.RECOMPILE.wasPressed()) {
			doIt = true;
		}

		if (doIt) {
			CanvasMod.LOG.info(I18n.translate("info.canvas.reloading"));
			GlShaderManager.INSTANCE.reload();
			LightmapHdTexture.reload();
			LightmapHd.reload();
			MaterialProgramManager.INSTANCE.reload();
			TerrainModelSpace.reload();
			PipelineLoader.INSTANCE.apply(MinecraftClient.getInstance().getResourceManager());
			Pipeline.reload();
			MaterialTextureState.reload();
		}
	}

	static void beginFullFrameRender() {
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

	static void setProjection(int pixelWidth, int pixelHeight) {
		RenderSystem.loadIdentity();
		GlStateManager.ortho(0.0D, pixelWidth, pixelHeight, 0.0D, 1000.0, 3000.0);
	}

	static void endFullFrameRender() {
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

	public static void afterRenderHand() {
		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : Pipeline.afterRenderHand) {
			pass.run(w, h);
		}

		endFullFrameRender();
	}

	static void renderDebug(int glId, int lod) {
		beginFullFrameRender();

		drawBuffer.bind();
		RenderSystem.viewport(0, 0, w, h);
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mainFbo);
		PipelineManager.setProjection(w, h);
		GlStateManager.activeTexture(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		GlStateManager.bindTexture(glId);
		setProjection(w, h);
		debugShader.activate().size(w, h).lod(lod);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endFullFrameRender();
	}

	private static void sync() {
		assert RenderSystem.isOnRenderThread();

		mcFbo = MinecraftClient.getInstance().getFramebuffer();
		final FrameBufferExt mcFboExt = ((FrameBufferExt) mcFbo);

		if (Pipeline.needsReload() || mcFboExt.canvas_colorAttachment() != mainColor || mcFbo.textureHeight != h || mcFbo.textureWidth != w) {
			Pipeline.close();
			tearDown();

			debugShader = new ProcessShader(new Identifier("canvas:shaders/internal/process/copy_lod.vert"), new Identifier("canvas:shaders/internal/process/copy_lod.frag"), "_cvu_input");
			mainFbo = mcFbo.fbo;
			canvasFboId = GlStateManager.genFramebuffers();
			mainColor = mcFboExt.canvas_colorAttachment();

			w = mcFbo.textureWidth;
			h = mcFbo.textureHeight;

			Pipeline.activate(w, h);
			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, mcFbo.fbo);

			assert mainColor == Pipeline.getImage("default_main").glId();

			// WIP: remove
			if (MinecraftClient.isFabulousGraphicsOrBetter()) {
				texEmissive = Pipeline.getImage("emissive").glId();
			}

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
		if (debugShader != null) {
			debugShader.unload();
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
