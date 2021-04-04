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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.IndexBuffer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.VertexFormat.DrawMode;
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
import grondag.canvas.render.PrimaryFrameBuffer;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.ProcessShader;
import grondag.canvas.varia.GFX;

//PERF: handle VAO properly here before re-enabling VAO
public class PipelineManager {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: PipelineManager static init");
		}
	}

	static ProcessShader debugShader;
	static ProcessShader debugDepthShader;
	static ProcessShader debugDepthArrayShader;

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
			init((PrimaryFrameBuffer) MinecraftClient.getInstance().getFramebuffer(), w, h);
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
		CanvasTextureState.activeTextureUnit(GFX.GL_TEXTURE1);
		oldTex1 = CanvasTextureState.getActiveBoundTexture();
		CanvasTextureState.activeTextureUnit(GFX.GL_TEXTURE0);
		oldTex0 = CanvasTextureState.getActiveBoundTexture();

		GFX.depthMask(false);
		GFX.disableBlend();
		GFX.disableCull();
		GFX.disableDepthTest();
		GFX.backupProjectionMatrix();
	}

	static void endFullFrameRender() {
		GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		CanvasTextureState.activeTextureUnit(GFX.GL_TEXTURE1);
		CanvasTextureState.bindTexture(oldTex1);
		//GlStateManager.disableTexture();
		CanvasTextureState.activeTextureUnit(GFX.GL_TEXTURE0);
		CanvasTextureState.bindTexture(oldTex0);
		GlProgram.deactivate();
		GFX.restoreProjectionMatrix();
		GFX.depthMask(true);
		GFX.enableDepthTest();
		GFX.enableCull();
		GFX.viewport(0, 0, w, h);
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

	static void renderDebug(int glId, int lod, int layer, boolean depth, boolean array) {
		beginFullFrameRender();

		drawBuffer.bind();
		GFX.viewport(0, 0, w, h);
		Pipeline.defaultFbo.bind();
		CanvasTextureState.activeTextureUnit(GFX.GL_TEXTURE0);
		//GlStateManager.disableTexture();

		if (array) {
			CanvasTextureState.bindTexture(GFX.GL_TEXTURE_2D_ARRAY, glId);
		} else {
			CanvasTextureState.bindTexture(glId);
		}

		if (depth) {
			if (array) {
				debugDepthArrayShader.activate().size(w, h).lod(lod).layer(layer);
			} else {
				debugDepthShader.activate().size(w, h).lod(0);
			}
		} else {
			debugShader.activate().size(w, h).lod(lod);
		}

		// WIP2: draw tris directly here
		final IndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS, 4 / 4 * 6);
		final int elementCount = indexBuffer.getVertexFormat().field_27374;
		GFX.drawElements(DrawMode.QUADS.mode, 0, elementCount, 0L);
		//GlStateManager.drawArrays(GL11.GL_QUADS, 0, 4);

		endFullFrameRender();
	}

	public static void init(PrimaryFrameBuffer primary, int width, int height) {
		Pipeline.close();
		tearDown();

		w = width;
		h = height;

		Pipeline.activate(primary, w, h);

		final MinecraftClient mc = MinecraftClient.getInstance();

		if (mc.worldRenderer != null) {
			((WorldRendererExt) mc.worldRenderer).canvas_setupFabulousBuffers();
		}

		mc.options.graphicsMode = Pipeline.isFabulous() ? GraphicsMode.FABULOUS : GraphicsMode.FANCY;

		debugShader = new ProcessShader(new Identifier("canvas:shaders/pipeline/post/simple_full_frame.vert"), new Identifier("canvas:shaders/pipeline/post/copy_lod.frag"), "_cvu_input");
		debugDepthShader = new ProcessShader(new Identifier("canvas:shaders/pipeline/post/simple_full_frame.vert"), new Identifier("canvas:shaders/pipeline/post/visualize_depth.frag"), "_cvu_input");
		debugDepthArrayShader = new ProcessShader(new Identifier("canvas:shaders/pipeline/post/simple_full_frame.vert"), new Identifier("canvas:shaders/pipeline/post/visualize_depth_array.frag"), "_cvu_input");
		Pipeline.defaultFbo.bind();
		CanvasTextureState.bindTexture(0);

		final VertexCollectorImpl collector = new VertexCollectorImpl();
		collector.add(0f, 0f, 0.2f, 0, 1f);
		collector.add(1f, 0f, 0.2f, 1f, 1f);
		collector.add(1f, 1f, 0.2f, 1f, 0f);
		collector.add(1f, 1f, 0.2f, 1f, 0f);
		collector.add(0f, 1f, 0.2f, 0f, 0f);
		collector.add(0f, 0f, 0.2f, 0, 1f);

		drawBuffer = new VboBuffer(collector.byteSize(), CanvasVertexFormats.PROCESS_VERTEX_UV);
		collector.toBuffer(drawBuffer.intBuffer());
		drawBuffer.upload();

		collector.clear(); // releases storage
	}

	private static void tearDown() {
		debugShader = ProcessShader.unload(debugShader);
		debugDepthShader = ProcessShader.unload(debugDepthShader);
		debugDepthArrayShader = ProcessShader.unload(debugDepthArrayShader);

		if (drawBuffer != null) {
			drawBuffer.close();
			drawBuffer = null;
		}
	}
}
