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

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.shader.data.ShaderStrings;

// PERF: emit switch statements on non-Mac
public class GlMaterialShader extends GlShader {
	GlMaterialShader(Identifier shaderSource, int shaderType, ProgramType programType) {
		super(shaderSource, shaderType, programType);
	}

	// all material shaders use the same source so only append extension to keep debug source file names of reasonable length
	@Override
	protected String debugSourceString() {
		return programType.name + "-" + (shaderType == GL21.GL_FRAGMENT_SHADER ? ".frag" : ".vert");
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
			impl = loadShaderSource(resourceManager, MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(shaders[0]));

			if (impl.contains("frx_startFragment")) {
				starts = "\tfrx_startFragment(data);";
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

				String src = loadShaderSource(resourceManager, MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(index));

				if (src.contains("frx_startFragment")) {
					startsBuilder.append("frx_startFragment");
					startsBuilder.append(index);
					startsBuilder.append("(data); break;\n");

					src = StringUtils.replace(src, "frx_startFragment", "frx_startFragment" + index);
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

		final Identifier sourceId = programType.isDepth && Pipeline.config().skyShadow != null
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

		final Identifier sourceId = programType.isDepth && Pipeline.config().skyShadow != null
				? Pipeline.config().skyShadow.vertexSource
				: Pipeline.config().materialProgram.vertexSource;

		final String pipelineSource = PreReleaseShaderCompat.compatifyPipelineVertex(loadShaderSource(resourceManager, sourceId), sourceId);
		baseSource = StringUtils.replace(baseSource, ShaderStrings.API_TARGET, impl + pipelineSource);
		baseSource = StringUtils.replace(baseSource, ShaderStrings.VERTEX_START, starts);
		return baseSource;
	}

	private static String loadMaterialVertexShader(ResourceManager resourceManager, Identifier shaderSourceId) {
		return PreReleaseShaderCompat.compatifyMaterialVertex(loadShaderSource(resourceManager, shaderSourceId), shaderSourceId);
	}
}
