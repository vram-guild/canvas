/*******************************************************************************
 * Copyright 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.shader;

import grondag.canvas.material.MaterialVertexFormats;
import grondag.frex.api.material.UniformRefreshFrequency;

public class ProcessShaders {
	private static GlProgram copy;

	public static GlProgram copy() {
		return copy;
	}

	static {
		reload();
	}

	public static void reload() {
		if (copy != null) {
			copy.unload();
			copy = null;
		}

		final GlShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(ShaderData.COPY_VERTEX, ShaderContext.PROCESS);
		final GlShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(ShaderData.COPY_FRAGMENT, ShaderContext.PROCESS);
		copy = new GlProgram(vs, fs, MaterialVertexFormats.PROCESS_VERTEX_UV, ShaderContext.PROCESS);
		copy.uniform1i("_cvu_textures", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));
		copy.load();
	}
}
