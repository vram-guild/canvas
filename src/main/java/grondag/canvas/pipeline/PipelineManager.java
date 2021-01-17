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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GraphicsMode;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.pipeline.pass.Pass;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ProcessShader;
import grondag.canvas.varia.CanvasGlHelper;

//PERF: handle VAO properly here before re-enabling VAO
public class PipelineManager {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: PipelineManager static init");
		}
	}

	static ProcessShader debugShader;

	static VboBuffer drawBuffer;
	static int h;
	static int w;
	private static int oldTex0;
	private static int oldTex1;

	private static boolean active = false;

	public static int width() {
		return w;
	}

	public static int height() {
		return h;
	}

	public static void reloadIfNeeded() {
		if (Pipeline.needsReload()) {
			init(w, h);
		}

		handleRecompile();
	}

	public static void beforeWorldRender() {
		assert !active;

		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : Pipeline.onWorldRenderStart) {
			pass.run(w, h);
		}

		endFullFrameRender();

		Pipeline.defaultFbo.bind();
	}

	public static void handleRecompile() {
		boolean doIt = false;

		while (CanvasMod.RECOMPILE.wasPressed()) {
			doIt = true;
		}

		if (doIt) {
			CanvasMod.LOG.info(I18n.translate("info.canvas.reloading"));
			Canvas.INSTANCE.recompile();
		}
	}

	static void beginFullFrameRender() {
		// UGLY: put state preservation into texture manager
		CanvasTextureState.activeTextureUnit(GL21.GL_TEXTURE1);
		oldTex1 = CanvasTextureState.getActiveBoundTexture();
		CanvasTextureState.activeTextureUnit(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		oldTex0 = CanvasTextureState.getActiveBoundTexture();

		RenderSystem.depthMask(false);
		RenderSystem.disableBlend();
		RenderSystem.disableCull();
		RenderSystem.disableAlphaTest();
		RenderSystem.disableDepthTest();
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		RenderSystem.pushMatrix();
	}

	public static void setProjection(int pixelWidth, int pixelHeight) {
		RenderSystem.loadIdentity();
		GlStateManager.ortho(0.0D, pixelWidth, pixelHeight, 0.0D, 1000.0, 3000.0);
	}

	static void endFullFrameRender() {
		VboBuffer.unbind();
		CanvasTextureState.activeTextureUnit(GL21.GL_TEXTURE1);
		CanvasTextureState.bindTexture(oldTex1);
		GlStateManager.disableTexture();
		CanvasTextureState.activeTextureUnit(GL21.GL_TEXTURE0);
		CanvasTextureState.bindTexture(oldTex0);
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

	public static void beFabulous() {
		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : Pipeline.fabulous) {
			pass.run(w, h);
		}

		endFullFrameRender();
	}

	static void renderDebug(int glId, int lod) {
		beginFullFrameRender();

		drawBuffer.bind();
		RenderSystem.viewport(0, 0, w, h);
		Pipeline.defaultFbo.bind();
		PipelineManager.setProjection(w, h);
		CanvasTextureState.activeTextureUnit(GL21.GL_TEXTURE0);
		GlStateManager.enableTexture();
		CanvasTextureState.bindTexture(glId);
		setProjection(w, h);
		debugShader.activate().size(w, h).lod(lod);
		GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endFullFrameRender();
	}

	public static void init(int width, int height) {
		assert RenderSystem.isOnRenderThread();

		assert CanvasGlHelper.checkError();

		Pipeline.close();
		tearDown();
		assert CanvasGlHelper.checkError();

		w = width;
		h = height;

		Pipeline.activate(w, h);
		assert CanvasGlHelper.checkError();

		final MinecraftClient mc = MinecraftClient.getInstance();

		if (mc.worldRenderer != null) {
			((WorldRendererExt) mc.worldRenderer).canvas_setupFabulousBuffers();
			assert CanvasGlHelper.checkError();
		}

		mc.options.graphicsMode = Pipeline.isFabulous() ? GraphicsMode.FABULOUS : GraphicsMode.FANCY;

		debugShader = new ProcessShader(new Identifier("canvas:shaders/pipeline/post/simple_full_frame.vert"), new Identifier("canvas:shaders/pipeline/post/copy_lod.frag"), "_cvu_input");
		Pipeline.defaultFbo.bind();
		CanvasTextureState.bindTexture(0);
		assert CanvasGlHelper.checkError();

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

	private static void tearDown() {
		if (debugShader != null) {
			debugShader.unload();
		}

		if (drawBuffer != null) {
			drawBuffer.close();
			drawBuffer = null;
		}
	}
}
