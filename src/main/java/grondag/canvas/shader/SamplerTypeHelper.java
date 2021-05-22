package grondag.canvas.shader;

import grondag.canvas.shader.GlProgram;
import grondag.frex.api.material.UniformRefreshFrequency;

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
		for (String type:samplerTypes) {
			if (program.containsUniformSpec(type, samplerName)) {
				return type;
			}
		}

		return "sampler2D";
	}
}
