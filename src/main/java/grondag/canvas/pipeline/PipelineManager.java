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

import com.mojang.math.Matrix4f;
import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.render.StaticDrawBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.TransferBuffers;
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
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

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

	public static void reloadIfNeeded(boolean forceRecompile) {
		if (Pipeline.needsReload()) {
			init((PrimaryFrameBuffer) Minecraft.getInstance().getMainRenderTarget(), w, h);
		}

		handleRecompile(forceRecompile);
	}

	public static void beforeWorldRender() {
		assert !active;

		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : Pipeline.onWorldRenderStart) {
			if (pass.isEnabled()) {
				Timekeeper.instance.swap(Timekeeper.ProfilerGroup.BeforeWorld, pass.getName());
				pass.run(w, h);
			}
		}

		Timekeeper.instance.completePass();

		endFullFrameRender();

		Pipeline.defaultFbo.bind();
	}

	public static void handleRecompile(boolean forceRecompile) {
		while (CanvasMod.RECOMPILE.consumeClick()) {
			forceRecompile = true;
		}

		if (forceRecompile) {
			CanvasMod.LOG.info(I18n.get("info.canvas.reloading"));
			Canvas.instance().recompile();
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
			if (pass.isEnabled()) {
				Timekeeper.instance.swap(Timekeeper.ProfilerGroup.AfterHand, pass.getName());
				pass.run(w, h);
			}
		}

		Timekeeper.instance.completePass();

		endFullFrameRender();
	}

	public static void beFabulous() {
		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : Pipeline.fabulous) {
			if (pass.isEnabled()) {
				Timekeeper.instance.swap(Timekeeper.ProfilerGroup.Fabulous, pass.getName());
				pass.run(w, h);
			}
		}

		Timekeeper.instance.completePass();

		endFullFrameRender();
	}

	static void renderDebug(int glId, int lod, int layer, boolean depth, boolean array) {
		beginFullFrameRender();

		drawBuffer.bind();
		final Matrix4f orthoMatrix = Matrix4f.orthographic(w, -h, 1000.0F, 3000.0F);
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

		final Minecraft mc = Minecraft.getInstance();

		if (mc.levelRenderer != null) {
			((WorldRendererExt) mc.levelRenderer).canvas_setupFabulousBuffers();
		}

		mc.options.graphicsMode = Pipeline.isFabulous() ? GraphicsStatus.FABULOUS : GraphicsStatus.FANCY;

		debugShader = new ProcessShader("debug", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/copy_lod.frag"), "_cvu_input");
		debugDepthShader = new ProcessShader("debug_depth", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/visualize_depth.frag"), "_cvu_input");
		debugDepthArrayShader = new ProcessShader("debug_depth_array", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/visualize_depth_array.frag"), "_cvu_input");
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

		final TransferBuffer transfer = TransferBuffers.claim(collector.byteSize());
		collector.toBuffer(0, transfer, 0);
		drawBuffer = new StaticDrawBuffer(CanvasVertexFormats.PROCESS_VERTEX_UV, transfer);
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
			drawBuffer.release();
			drawBuffer = null;
		}
	}
}
