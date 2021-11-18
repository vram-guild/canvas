/*
 * Copyright © Original Authors
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
