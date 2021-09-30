/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.pipeline.pass;

import com.mojang.math.Matrix4f;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.ProgramTextureData;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.shader.ProcessShader;
import grondag.canvas.varia.GFX;

class ProgramPass extends Pass {
	final ProgramTextureData textures;

	ProcessShader shader;

	ProgramPass(PassConfig config) {
		super(config);

		shader = Pipeline.getShader(config.program.name);
		textures = new ProgramTextureData(config.samplerImages);
	}

	@Override
	public void run(int width, int height) {
		if (fbo == null) {
			return;
		}

		fbo.bind();

		final int lod = config.lod;

		if (lod > 0) {
			width >>= lod;
			height >>= lod;
		}

		final Matrix4f orthoMatrix = Matrix4f.orthographic(width, -height, 1000.0F, 3000.0F);
		GFX.viewport(0, 0, width, height);

		final int slimit = textures.texIds.length;

		for (int i = 0; i < slimit; ++i) {
			CanvasTextureState.activeTextureUnit(GFX.GL_TEXTURE0 + i);
			CanvasTextureState.bindTexture(textures.texTargets[i], textures.texIds[i]);
		}

		shader.activate().lod(config.lod).size(width, height).projection(orthoMatrix);

		GFX.drawArrays(GFX.GL_TRIANGLES, 0, 6);
	}

	@Override
	public void close() {
		// NOOP
	}
}
