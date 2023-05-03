/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.pipeline;

import org.joml.Matrix4f;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.input.DrawableVertexCollector;
import grondag.canvas.buffer.input.SimpleVertexCollector;
import grondag.canvas.buffer.render.StaticDrawBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.pass.Pass;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.shader.ProcessProgram;
import grondag.canvas.varia.GFX;

//PERF: handle VAO properly here before re-enabling VAO
public class PipelineManager {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: PipelineManager static init");
		}
	}

	static ProcessProgram debugProgram;
	static ProcessProgram debugArrayProgram;
	static ProcessProgram debugDepthProgram;
	static ProcessProgram debugDepthArrayProgram;
	static ProcessProgram debugCubeMapProgram;

	static StaticDrawBuffer drawBuffer;
	private static int oldTex0;
	private static int oldTex1;

	public static void reload() {
		init();
	}

	public static void init() {
		Pipeline.close();
		tearDown();

		Pipeline.activate();

		debugProgram = new ProcessProgram("debug", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/copy_lod.frag"), "_cvu_input");
		debugArrayProgram = new ProcessProgram("debug_array", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/copy_lod_array.frag"), "_cvu_input");
		debugDepthProgram = new ProcessProgram("debug_depth", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/visualize_depth.frag"), "_cvu_input");
		debugDepthArrayProgram = new ProcessProgram("debug_depth_array", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/visualize_depth_array.frag"), "_cvu_input");
		debugCubeMapProgram = new ProcessProgram("debug_cube_map", new ResourceLocation("canvas:shaders/pipeline/post/simple_full_frame.vert"), new ResourceLocation("canvas:shaders/pipeline/post/visualize_cube_map.frag"), "_cvu_input");

		final DrawableVertexCollector collector = new SimpleVertexCollector(RenderState.missing(), new int[64]);

		final int[] v = collector.target();
		addVertex(0f, 0f, 0.2f, 0f, 1f, v, 0);
		addVertex(1f, 0f, 0.2f, 1f, 1f, v, 5);
		addVertex(1f, 1f, 0.2f, 1f, 0f, v, 10);
		addVertex(1f, 1f, 0.2f, 1f, 0f, v, 15);
		addVertex(0f, 1f, 0.2f, 0f, 0f, v, 20);
		addVertex(0f, 0f, 0.2f, 0f, 1f, v, 25);
		collector.commit(30);

		final TransferBuffer transfer = TransferBuffers.claim(collector.byteSize());
		collector.toBuffer(transfer, 0);
		drawBuffer = new StaticDrawBuffer(CanvasVertexFormats.PROCESS_VERTEX_UV, transfer);
		drawBuffer.upload();

		collector.clear(); // releases storage

		renderFullFramePasses(Pipeline.onInit);
		renderFullFramePasses(Pipeline.onResize);
	}

	private static void addVertex(float x, float y, float z, float u, float v, int[] target, int index) {
		target[index] = Float.floatToRawIntBits(x);
		target[++index] = Float.floatToRawIntBits(y);
		target[++index] = Float.floatToRawIntBits(z);
		target[++index] = Float.floatToRawIntBits(u);
		target[++index] = Float.floatToRawIntBits(v);
	}

	public static void onResize() {
		Pipeline.onResize();

		renderFullFramePasses(Pipeline.onResize);
	}

	public static void beforeWorldRender() {
		renderFullFramePasses(Pipeline.onWorldRenderStart, Timekeeper.ProfilerGroup.BeforeWorld);
		Pipeline.defaultFbo.bind();
	}

	public static void beFabulous() {
		renderFullFramePasses(Pipeline.fabulous, Timekeeper.ProfilerGroup.Fabulous);
	}

	public static void afterRenderHand() {
		renderFullFramePasses(Pipeline.afterRenderHand, Timekeeper.ProfilerGroup.AfterHand);
	}

	static void renderFullFramePasses(Pass[] passes, Timekeeper.ProfilerGroup profilerGroup) {
		beginFullFrameRender();

		drawBuffer.bind();

		for (final Pass pass : passes) {
			if (pass.isEnabled()) {
				if (profilerGroup != null) {
					Timekeeper.instance.swap(profilerGroup, pass.getName());
				}

				pass.run();
			}
		}

		if (profilerGroup != null) {
			Timekeeper.instance.completePass();
		}

		endFullFrameRender();
	}

	static void renderFullFramePasses(Pass[] passes) {
		renderFullFramePasses(passes, null);
	}

	static void renderDebug(int glId, int lod, int layer, boolean depth, int target) {
		beginFullFrameRender();

		int width = Pipeline.width();
		int height = Pipeline.height();

		drawBuffer.bind();
		//TODO: validate
		final Matrix4f orthoMatrix = new Matrix4f().setOrtho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F);
		GFX.viewport(0, 0, width, height);
		Pipeline.defaultFbo.bind();

		CanvasTextureState.ensureTextureOfTextureUnit(GFX.GL_TEXTURE0, target, glId);

		final boolean isLayered = target == GFX.GL_TEXTURE_2D_ARRAY;

		if (target == GFX.GL_TEXTURE_CUBE_MAP) {
			debugCubeMapProgram.activate();
			debugCubeMapProgram.size(width, height).lod(lod).projection(orthoMatrix);
		} else if (depth) {
			if (isLayered) {
				debugDepthArrayProgram.activate();
				debugDepthArrayProgram.size(width, height).lod(lod).layer(layer).projection(orthoMatrix);
			} else {
				debugDepthProgram.activate();
				debugDepthProgram.size(width, height).lod(0).projection(orthoMatrix);
			}
		} else {
			if (isLayered) {
				debugArrayProgram.activate();
				debugArrayProgram.size(width, height).lod(lod).layer(layer).projection(orthoMatrix);
			} else {
				debugProgram.activate();
				debugProgram.size(width, height).lod(lod).projection(orthoMatrix);
			}
		}

		GFX.drawArrays(GFX.GL_TRIANGLES, 0, 6);

		endFullFrameRender();
	}

	static void beginFullFrameRender() {
		oldTex1 = CanvasTextureState.getBoundTexture(GFX.GL_TEXTURE1);
		oldTex0 = CanvasTextureState.getBoundTexture(GFX.GL_TEXTURE0);

		GFX.depthMask(false);
		GFX.disableBlend();
		GFX.disableCull();
		GFX.disableDepthTest();
		GFX.backupProjectionMatrix();
	}

	static void endFullFrameRender() {
		GFX.bindVertexArray(0);

		CanvasTextureState.ensureTextureOfTextureUnit(GFX.GL_TEXTURE0, GFX.GL_TEXTURE_2D, oldTex0);
		CanvasTextureState.ensureTextureOfTextureUnit(GFX.GL_TEXTURE1, GFX.GL_TEXTURE_2D, oldTex1);

		GFX.useProgram(0);
		GFX.restoreProjectionMatrix();
		GFX.depthMask(true);
		GFX.enableDepthTest();
		GFX.enableCull();
		GFX.viewport(0, 0, Pipeline.width(), Pipeline.height());
	}

	private static void tearDown() {
		debugProgram = ProcessProgram.unload(debugProgram);
		debugArrayProgram = ProcessProgram.unload(debugArrayProgram);
		debugDepthProgram = ProcessProgram.unload(debugDepthProgram);
		debugDepthArrayProgram = ProcessProgram.unload(debugDepthArrayProgram);
		debugCubeMapProgram = ProcessProgram.unload(debugCubeMapProgram);

		if (drawBuffer != null) {
			drawBuffer.release();
			drawBuffer = null;
		}
	}
}
