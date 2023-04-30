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

package grondag.canvas.pipeline.pass;

import org.joml.Matrix4f;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.ProgramTextureData;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.shader.ProcessProgram;
import grondag.canvas.varia.GFX;

class ProgramPass extends Pass {
	final ProgramTextureData textures;

	ProcessProgram program;

	ProgramPass(PassConfig config) {
		super(config);

		program = Pipeline.getProgram(config.program.name);
		textures = new ProgramTextureData(config.samplerImages);
	}

	@Override
	public void run(int width, int height) {
		if (fbo == null) {
			return;
		}

		width = config.width > 0 ? config.width : width;
		height = config.height > 0 ? config.height : height;

		fbo.bind();

		final int lod = config.lod;

		if (lod > 0) {
			width >>= lod;
			height >>= lod;
		}

		final Matrix4f orthoMatrix = new Matrix4f().setOrtho(0, width, height, 0, 1000.0F, 3000.0F);
		GFX.viewport(0, 0, width, height);

		final int slimit = textures.texIds.length;

		for (int i = 0; i < slimit; ++i) {
			CanvasTextureState.ensureTextureOfTextureUnit(GFX.GL_TEXTURE0 + i, textures.texTargets[i], textures.texIds[i]);
		}

		program.activate();
		program.lod(config.lod).layer(config.layer).size(width, height).projection(orthoMatrix);

		GFX.drawArrays(GFX.GL_TRIANGLES, 0, 6);
	}

	@Override
	public void close() {
		// NOOP
	}
}
