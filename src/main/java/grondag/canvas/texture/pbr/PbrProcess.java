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

package grondag.canvas.texture.pbr;

import org.joml.Matrix4f;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.input.DrawableVertexCollector;
import grondag.canvas.buffer.input.SimpleVertexCollector;
import grondag.canvas.buffer.render.StaticDrawBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.pipeline.GlSymbolLookup;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.shader.ProcessProgram;
import grondag.canvas.varia.GFX;

public class PbrProcess {
	private static final String[] SAMPLERS;
	private static final int INPUT_COUNT = 8;

	static {
		SAMPLERS = new String[INPUT_COUNT];

		for (int i = 0; i < INPUT_COUNT; i++) {
			SAMPLERS[i] = "frx_source" + i;
		}
	}

	private final ProcessProgram program;

	public PbrProcess(ResourceLocation fragSource) {
		this.program = new ProcessProgram(fragSource.toString(), new ResourceLocation("canvas:shaders/internal/pbr.vert"), fragSource, SAMPLERS);
	}

	public PbrProcess() {
		this.program = new ProcessProgram("pbr_process", new ResourceLocation("canvas:shaders/internal/pbr.vert"), new ResourceLocation("canvas:shaders/internal/pbr.frag"), SAMPLERS);
	}

	boolean process(String debugLabel, int width, int height, InputTextureManager.InputTexture[] inputTexture, int outputTextureId) {
		if (inputTexture.length > INPUT_COUNT - 1) {
			throw new IllegalStateException("PBR process is made to accept more than " + INPUT_COUNT + " image sources");
		}

		int fboGlId = GFX.genFramebuffer();
		GFX.bindFramebuffer(GFX.GL_FRAMEBUFFER, fboGlId);
		GFX.objectLabel(GFX.GL_FRAMEBUFFER, fboGlId, "FBO " + debugLabel);
		GFX.glDrawBuffers(new int[]{GFX.GL_COLOR_ATTACHMENT0});
		GFX.glFramebufferTexture2D(GFX.GL_FRAMEBUFFER, GFX.GL_COLOR_ATTACHMENT0, GFX.GL_TEXTURE_2D, outputTextureId, 0);
		final int check = GFX.checkFramebufferStatus(GFX.GL_FRAMEBUFFER);

		if (check != GFX.GL_FRAMEBUFFER_COMPLETE) {
			CanvasMod.LOG.warn("Failed PBR processing: Framebuffer " + debugLabel + " has invalid status " + check + " " + GlSymbolLookup.reverseLookup(check));
			return false;
		}

		final Matrix4f orthoMatrix = new Matrix4f().setOrtho(0, width, height, 0, 1000.0F, 3000.0F);
		GFX.viewport(0, 0, width, height);

		for (int i = 0; i < inputTexture.length; ++i) {
			CanvasTextureState.ensureTextureOfTextureUnit(GFX.GL_TEXTURE0 + i, GFX.GL_TEXTURE_2D, inputTexture[i].getTexId());
		}

		program.activate();
		program.lod(0).layer(0).size(width, height).projection(orthoMatrix);

		GFX.drawArrays(GFX.GL_TRIANGLES, 0, 6);

		GFX.deleteFramebuffer(fboGlId);

		return true;
	}

	void close() {
		program.unload();
	}

	static class DrawContext {
		final StaticDrawBuffer drawBuffer;

		{
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
		}

		void close() {
			drawBuffer.release();
		}

		private static void addVertex(float x, float y, float z, float u, float v, int[] target, int index) {
			target[index] = Float.floatToRawIntBits(x);
			target[++index] = Float.floatToRawIntBits(y);
			target[++index] = Float.floatToRawIntBits(z);
			target[++index] = Float.floatToRawIntBits(u);
			target[++index] = Float.floatToRawIntBits(v);
		}
	}
}
