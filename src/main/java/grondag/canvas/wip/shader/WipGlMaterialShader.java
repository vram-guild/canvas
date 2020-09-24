/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.wip.shader;

import grondag.canvas.wip.encoding.WipVertexFormat;
import grondag.canvas.wip.state.WipProgramType;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

public class WipGlMaterialShader extends WipGlShader{
	private final ObjectOpenHashSet<WipMaterialShaderImpl> materials;

	WipGlMaterialShader(Identifier shaderSource, int shaderType, WipProgramType programType, WipVertexFormat format, ObjectOpenHashSet<WipMaterialShaderImpl> materials) {
		super(shaderSource, shaderType, programType, format);
		this.materials = materials;
	}

	// all material shaders use the same source so only append extension to keep debug source file names of reasonable length
	@Override
	protected String debugSourceString() {
		return shaderType == GL21.GL_FRAGMENT_SHADER ? ".frag" : ".vert";
	}
}
