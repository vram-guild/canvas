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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.buffer.StaticDrawBuffer;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.mixinterface.WorldRendererExt;
import grondag.canvas.perf.Timekeeper;
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

	static StaticDrawBuffer drawBuffer;
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
			Timekeeper.instance.swap(Timekeeper.ProfilerGroup.BeforeWorld, pass.getName());
			pass.run(w, h);
		}

		Timekeeper.instance.completePass();

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
		GFX.bindVertexArray(0);
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
			Timekeeper.instance.swap(Timekeeper.ProfilerGroup.AfterHand, pass.getName());
			pass.run(w, h);
		}

		Timekeeper.instance.completePass();

		endFullFrameRender();
	}

	public static void beFabulous() {
		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : Pipeline.fabulous) {
			Timekeeper.instance.swap(Timekeeper.ProfilerGroup.Fabulous, pass.getName());
			pass.run(w, h);
		}

		Timekeeper.instance.completePass();

		endFullFrameRender();
	}

	static void renderDebug(int glId, int lod, int layer, boolean depth, boolean array) {
		beginFullFrameRender();

		drawBuffer.bind();
		final Matrix4f orthoMatrix = Matrix4f.projectionMatrix(w, -h, 1000.0F, 3000.0F);
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
				debugDepthArrayShader.activate().size(w, h).lod(lod).layer(layer).projection(orthoMatrix);
			} else {
				debugDepthShader.activate().size(w, h).lod(0).projection(orthoMatrix);
			}
		} else {
			debugShader.activate().size(w, h).lod(lod).projection(orthoMatrix);
		}

		GFX.drawArrays(GFX.GL_TRIANGLES, 0, 6);

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

		final ArrayVertexCollector collector = new ArrayVertexCollector(RenderState.MISSING, false);
		final int k = collector.allocate(30);
		final int[] v = collector.data();
		addVertex(0f, 0f, 0.2f, 0f, 1f, v, k);
		addVertex(1f, 0f, 0.2f, 1f, 1f, v, k + 5);
		addVertex(1f, 1f, 0.2f, 1f, 0f, v, k + 10);
		addVertex(1f, 1f, 0.2f, 1f, 0f, v, k + 15);
		addVertex(0f, 1f, 0.2f, 0f, 0f, v, k + 20);
		addVertex(0f, 0f, 0.2f, 0f, 1f, v, k + 25);

		drawBuffer = new StaticDrawBuffer(collector.byteSize(), CanvasVertexFormats.PROCESS_VERTEX_UV);
		collector.toBuffer(drawBuffer.intBuffer(), 0);
		drawBuffer.upload();

		collector.clear(); // releases storage
	}

	private static void addVertex(float x, float y, float z, float u, float v, int[] target, int index) {
		target[index] = Float.floatToRawIntBits(x);
		target[++index] = Float.floatToRawIntBits(y);
		target[++index] = Float.floatToRawIntBits(z);
		target[++index] = Float.floatToRawIntBits(u);
		target[++index] = Float.floatToRawIntBits(v);
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
