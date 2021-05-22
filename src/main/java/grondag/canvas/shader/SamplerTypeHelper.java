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

package grondag.canvas.shader;

public class SamplerTypeHelper {
	public static final String[] samplerTypes = new String[]{
		// float samplers
		"sampler1D",
		"sampler2D",
		"sampler3D",
		"samplerCube",
		"sampler2DRect",
		"sampler1DArray",
		"sampler2DArray",
		"samplerCubeArray",
		"samplerBuffer",
		"sampler2DMS",
		"sampler2DMSArray",

		// integer samplers
		"isampler1D",
		"isampler2D",
		"isampler3D",
		"isamplerCube",
		"isampler2DRect",
		"isampler1DArray",
		"isampler2DArray",
		"isamplerCubeArray",
		"isamplerBuffer",
		"isampler2DMS",
		"isampler2DMSArray",

		// unsigned integer samplers
		"usampler1D",
		"usampler2D",
		"usampler3D",
		"usamplerCube",
		"usampler2DRect",
		"usampler1DArray",
		"usampler2DArray",
		"usamplerCubeArray",
		"usamplerBuffer",
		"usampler2DMS",
		"usampler2DMSArray",

		// shadow samplers
		"sampler1DShadow",
		"sampler2DShadow",
		"samplerCubeShadow",
		"sampler2DRectShadow",
		"sampler1DArrayShadow",
		"sampler2DArrayShadow",
		"samplerCubeArrayShadow",
	};

	public static String getSamplerType(GlProgram program, String samplerName) {
		for (final String type:samplerTypes) {
			if (program.containsUniformSpec(type, samplerName)) {
				return type;
			}
		}

		return "sampler2D";
	}
}
