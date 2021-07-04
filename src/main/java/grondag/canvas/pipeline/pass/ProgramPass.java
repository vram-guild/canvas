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

package grondag.canvas.pipeline.pass;

import net.minecraft.util.math.Matrix4f;

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

		final Matrix4f orthoMatrix = Matrix4f.projectionMatrix(width, -height, 1000.0F, 3000.0F);
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
