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

package grondag.canvas.shader;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class GlMaterialShader extends GlShader{
	GlMaterialShader(Identifier shaderSource, int shaderType, ProgramType programType) {
		super(shaderSource, shaderType, programType);
	}

	// all material shaders use the same source so only append extension to keep debug source file names of reasonable length
	@Override
	protected String debugSourceString() {
		return shaderType == GL21.GL_FRAGMENT_SHADER ? ".frag" : ".vert";
	}

	@Override
	protected String preprocessSource(ResourceManager resourceManager, String baseSource) {

		if (shaderType == GL21.GL_FRAGMENT_SHADER) {
			return preprocessFragmentSource(resourceManager, baseSource);
		} else {
			return preprocessVertexSource(resourceManager, baseSource);
		}
	}

	private String preprocessFragmentSource(ResourceManager resourceManager, String baseSource) {
		String starts;
		String impl;

		final int[] shaders =  MaterialShaderManager.FRAGMENT_INDEXES.toIntArray();
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

			for (int i = 0; i < limit; ++i) {
				final int index = shaders[i];

				if (i > 0) {
					startsBuilder.append("\telse ");
				}

				if (i < limit - 1) {
					startsBuilder.append("\tif (cv_programId == ");
					startsBuilder.append(index);
					startsBuilder.append(") ");
				}

				String src = loadShaderSource(resourceManager, MaterialShaderManager.FRAGMENT_INDEXER.fromHandle(index));

				if (src.contains("frx_startFragment")) {
					startsBuilder.append("frx_startFragment");
					startsBuilder.append(index);
					startsBuilder.append("(data);\n");

					src = StringUtils.replace(src, "frx_startFragment", "frx_startFragment" + index);
					implBuilder.append(src);
					implBuilder.append("\n");
				} else {
					startsBuilder.append("{ }\n");
				}
			}

			impl = implBuilder.toString();
			starts = startsBuilder.toString();
		}

		baseSource = StringUtils.replace(baseSource, ShaderData.API_TARGET, impl);
		baseSource = StringUtils.replace(baseSource, ShaderData.FRAGMENT_START, starts);
		return baseSource;
	}

	private String preprocessVertexSource(ResourceManager resourceManager, String baseSource) {
		String starts;
		String ends;
		String impl;

		final int[] shaders =  MaterialShaderManager.VERTEX_INDEXES.toIntArray();
		final int limit = shaders.length;

		if (limit == 0) {
			starts = "\t// NOOP";
			ends = "\t// NOOP";
			impl = "\t// NOOP";
		} else if (limit == 1) {
			impl = loadShaderSource(resourceManager, MaterialShaderManager.VERTEX_INDEXER.fromHandle(shaders[0]));
			starts = impl.contains("frx_startVertex") ? "\tfrx_startVertex(data);" : "\t// NOOP";
			ends = impl.contains("frx_endVertex") ? "\tfrx_endVertex(data);" : "\t// NOOP";
		} else {
			final StringBuilder startsBuilder = new StringBuilder();
			final StringBuilder endsBuilder = new StringBuilder();
			final StringBuilder implBuilder = new StringBuilder();

			for (int i = 0; i < limit; ++i) {
				final int index = shaders[i];
				String src = loadShaderSource(resourceManager, MaterialShaderManager.VERTEX_INDEXER.fromHandle(index));

				if (i > 0) {
					startsBuilder.append("\telse ");
					endsBuilder.append("\telse ");
				}

				if (i < limit - 1) {
					startsBuilder.append("\tif (cv_programId == ");
					startsBuilder.append(index);
					startsBuilder.append(") ");

					endsBuilder.append("\tif (cv_programId == ");
					endsBuilder.append(index);
					endsBuilder.append(") ");
				}


				if (src.contains("frx_startVertex")) {
					startsBuilder.append("{ frx_startVertex");
					startsBuilder.append(index);
					startsBuilder.append("(data); }\n");
					src = StringUtils.replace(src, "frx_startVertex", "frx_startVertex" + index);
				} else {
					startsBuilder.append("{ }\n");
				}

				if (src.contains("frx_endVertex")) {
					endsBuilder.append("{ frx_endVertex");
					endsBuilder.append(index);
					endsBuilder.append("(data); }\n");
					src = StringUtils.replace(src, "frx_endVertex", "frx_endVertex" + index);
				} else {
					endsBuilder.append("{ }\n");
				}

				implBuilder.append(src);
				implBuilder.append("\n");
			}

			impl = implBuilder.toString();
			starts = startsBuilder.toString();
			ends = endsBuilder.toString();
		}

		baseSource = StringUtils.replace(baseSource, ShaderData.API_TARGET, impl);
		baseSource = StringUtils.replace(baseSource, ShaderData.VERTEX_START, starts);
		baseSource = StringUtils.replace(baseSource, ShaderData.VEREX_END, ends);
		return baseSource;
	}
}
