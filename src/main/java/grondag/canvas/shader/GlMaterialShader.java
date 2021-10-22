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

package grondag.canvas.shader;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.shader.data.ShaderStrings;

// PERF: emit switch statements on non-Mac
public class GlMaterialShader extends GlShader {
	GlMaterialShader(ResourceLocation shaderSource, int shaderType, ProgramType programType) {
		super(shaderSource, shaderType, programType);
	}

	// all material shaders use the same source so only append extension to keep debug source file names of reasonable length
	@Override
	protected String debugSourceString() {
		return programType.name + (shaderType == GL21.GL_FRAGMENT_SHADER ? ".frag" : ".vert");
	}

	@Override
	protected String preprocessSource(ResourceManager resourceManager, String baseSource) {
		if (shaderType == GL21.GL_FRAGMENT_SHADER) {
			baseSource = preprocessFragmentSource(resourceManager, baseSource);
		} else {
			baseSource = preprocessVertexSource(resourceManager, baseSource);
		}

		return super.preprocessSource(resourceManager, baseSource);
	}

	private String preprocessFragmentSource(ResourceManager resourceManager, String baseSource) {
		String starts;
		String impl;

		final int[] shaders = MaterialShaderManager.fragmentIds(programType);
		final int limit = shaders.length;

		if (limit == 0) {
			starts = "\t// NOOP";
			impl = "";
		} else if (limit == 1) {
			impl = loadMaterialFragmentShader(resourceManager, MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(shaders[0]));

			if (impl.contains("frx_startFragment")) {
				starts = "\tfrx_startFragment(compatData);";
			} else if (impl.contains("frx_materialFragment")) {
				starts = "\tfrx_materialFragment();";
			} else {
				starts = "\t// NOOP";
			}
		} else {
			final StringBuilder startsBuilder = new StringBuilder();
			final StringBuilder implBuilder = new StringBuilder();

			startsBuilder.append("\tswitch (cv_programId) {\n");

			for (int i = 0; i < limit; ++i) {
				final int index = shaders[i];

				startsBuilder.append("\tcase ");
				startsBuilder.append(index);
				startsBuilder.append(": ");

				String src = loadMaterialFragmentShader(resourceManager, MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(index));

				// UGLY: some pre-release compat handling here - should eventually be removed
				if (src.contains("frx_startFragment")) {
					startsBuilder.append("frx_startFragment");
					startsBuilder.append(index);
					startsBuilder.append("(compatData); break;\n");

					src = StringUtils.replace(src, "frx_startFragment", "frx_startFragment" + index);
					implBuilder.append(src);
					implBuilder.append("\n");
				} else if (src.contains("frx_materialFragment")) {
					startsBuilder.append("frx_materialFragment");
					startsBuilder.append(index);
					startsBuilder.append("(); break;\n");

					src = StringUtils.replace(src, "frx_materialFragment", "frx_materialFragment" + index);
					implBuilder.append(src);
					implBuilder.append("\n");
				} else {
					startsBuilder.append("break;\n");
				}
			}

			startsBuilder.append("\tdefault: break;\n");
			startsBuilder.append("\t}\n");

			impl = implBuilder.toString();
			starts = startsBuilder.toString();
		}

		final ResourceLocation sourceId = programType.isDepth && Pipeline.config().skyShadow != null
			? Pipeline.config().skyShadow.fragmentSource
			: Pipeline.config().materialProgram.fragmentSource;

		final String pipelineSource = loadShaderSource(resourceManager, sourceId);

		baseSource = StringUtils.replace(baseSource, ShaderStrings.API_TARGET, impl + pipelineSource);
		baseSource = StringUtils.replace(baseSource, ShaderStrings.FRAGMENT_START, starts);
		return baseSource;
	}

	private String preprocessVertexSource(ResourceManager resourceManager, String baseSource) {
		String starts;
		String impl;

		final int[] shaders = MaterialShaderManager.vertexIds(programType);
		final int limit = shaders.length;

		if (limit == 0) {
			starts = "\t// NOOP";
			impl = "\t// NOOP";
		} else if (limit == 1) {
			impl = loadMaterialVertexShader(resourceManager, MaterialShaderManager.VERTEX_INDEXER.fromHandle(shaders[0]));

			// prevent abandoned endVertex calls from conflicting
			impl = StringUtils.replace(impl, "frx_endVertex", "frx_endVertex_UNUSED");

			starts = impl.contains("frx_materialVertex") ? "\tfrx_materialVertex();" : "\t// NOOP";
		} else {
			final StringBuilder startsBuilder = new StringBuilder();
			final StringBuilder implBuilder = new StringBuilder();

			startsBuilder.append("\tswitch (cv_programId) {\n");

			for (int i = 0; i < limit; ++i) {
				final int index = shaders[i];

				startsBuilder.append("\tcase ");
				startsBuilder.append(index);
				startsBuilder.append(": ");

				String src = loadMaterialVertexShader(resourceManager, MaterialShaderManager.VERTEX_INDEXER.fromHandle(index));

				// prevent abandoned endVertex calls from conflicting
				src = StringUtils.replace(src, "frx_endVertex", "frx_endVertex" + i + "_UNUSED");

				if (src.contains("frx_materialVertex")) {
					startsBuilder.append("frx_materialVertex");
					startsBuilder.append(index);
					startsBuilder.append("(); break;\n");
					src = StringUtils.replace(src, "frx_materialVertex", "frx_materialVertex" + index);
				} else {
					startsBuilder.append("break;\n");
				}

				implBuilder.append(src);
				implBuilder.append("\n");
			}

			startsBuilder.append("\tdefault: break;\n");
			startsBuilder.append("\t}\n");

			impl = implBuilder.toString();
			starts = startsBuilder.toString();
		}

		final ResourceLocation sourceId = programType.isDepth && Pipeline.config().skyShadow != null
				? Pipeline.config().skyShadow.vertexSource
				: Pipeline.config().materialProgram.vertexSource;

		final String pipelineSource = PreReleaseShaderCompat.compatifyPipelineVertex(loadShaderSource(resourceManager, sourceId), sourceId);
		baseSource = StringUtils.replace(baseSource, ShaderStrings.API_TARGET, impl + pipelineSource);
		baseSource = StringUtils.replace(baseSource, ShaderStrings.VERTEX_START, starts);
		return baseSource;
	}

	private static String loadMaterialVertexShader(ResourceManager resourceManager, ResourceLocation shaderSourceId) {
		return PreReleaseShaderCompat.compatifyMaterialVertex(loadShaderSource(resourceManager, shaderSourceId), shaderSourceId);
	}

	private static String loadMaterialFragmentShader(ResourceManager resourceManager, ResourceLocation shaderSourceId) {
		return PreReleaseShaderCompat.compatify(loadShaderSource(resourceManager, shaderSourceId), shaderSourceId);
	}
}
