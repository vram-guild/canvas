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

import grondag.canvas.wip.state.WipProgramType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class WipGlMaterialShader extends WipGlShader{
	private final ObjectOpenHashSet<WipMaterialShaderImpl> materials;

	WipGlMaterialShader(Identifier shaderSource, int shaderType, WipProgramType programType, ObjectOpenHashSet<WipMaterialShaderImpl> materials) {
		super(shaderSource, shaderType, programType);
		this.materials = materials;
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

		if (materials.isEmpty()) {
			starts = "\t// NOOP";
			impl = "\t// NOOP";
		} else if (materials.size() == 1) {
			starts = "\tfrx_startFragment(data);";
			final int index = materials.iterator().next().fragmentShaderIndex;
			impl = loadShaderSource(resourceManager, WipMaterialShaderManager.FRAGMENT_INDEXER.fromHandle(index));
		} else {
			final IntOpenHashSet ids = new IntOpenHashSet();
			int counter = 1;
			final StringBuilder startsBuilder = new StringBuilder();
			final StringBuilder implBuilder = new StringBuilder();


			for (final WipMaterialShaderImpl mat : materials) {
				final int index = mat.fragmentShaderIndex;

				if (ids.add(index)) {
					if (counter > 1) {
						startsBuilder.append("\telse ");
					}

					if (counter < materials.size()) {
						startsBuilder.append("\tif (cv_programId == ");
						startsBuilder.append(index);
						startsBuilder.append(") ");
					}

					++ counter;

					startsBuilder.append("frx_startFragment");
					startsBuilder.append(index);
					startsBuilder.append("(data);\n");

					String src = loadShaderSource(resourceManager, WipMaterialShaderManager.FRAGMENT_INDEXER.fromHandle(index));
					src = StringUtils.replace(src, "frx_startFragment", "frx_startFragment" + index);
					implBuilder.append(src);
					implBuilder.append("\n");
				}
			}

			impl = implBuilder.toString();
			starts = startsBuilder.toString();
		}

		baseSource = StringUtils.replace(baseSource, WipShaderData.API_TARGET, impl);
		baseSource = StringUtils.replace(baseSource, WipShaderData.FRAGMENT_START, starts);
		return baseSource;
	}

	private String preprocessVertexSource(ResourceManager resourceManager, String baseSource) {
		String starts;
		String ends;
		String impl;

		if (materials.isEmpty()) {
			starts = "\t// NOOP";
			ends = "\t// NOOP";
			impl = "\t// NOOP";
		} else if (materials.size() == 1) {
			final int index = materials.iterator().next().vertexShaderIndex;
			impl = loadShaderSource(resourceManager, WipMaterialShaderManager.VERTEX_INDEXER.fromHandle(index));
			starts = impl.contains("frx_startVertex") ? "\tfrx_startVertex(data);" : "\t// NOOP";
			ends = impl.contains("frx_endVertex") ? "\tfrx_endVertex(data);" : "\t// NOOP";
		} else {
			final IntOpenHashSet ids = new IntOpenHashSet();
			int counter = 1;
			final StringBuilder startsBuilder = new StringBuilder();
			final StringBuilder endsBuilder = new StringBuilder();
			final StringBuilder implBuilder = new StringBuilder();


			for (final WipMaterialShaderImpl mat : materials) {
				final int index = mat.vertexShaderIndex;

				if (ids.add(index)) {
					if (counter > 1) {
						startsBuilder.append("\telse ");
						endsBuilder.append("\telse ");
					}

					if (counter < materials.size()) {
						startsBuilder.append("\tif (cv_programId == ");
						startsBuilder.append(index);
						startsBuilder.append(") ");

						endsBuilder.append("\tif (cv_programId == ");
						endsBuilder.append(index);
						endsBuilder.append(") ");
					}

					++ counter;

					String src = loadShaderSource(resourceManager, WipMaterialShaderManager.VERTEX_INDEXER.fromHandle(index));

					if (src.contains("frx_startVertex")) {
						startsBuilder.append("frx_startVertex");
						startsBuilder.append(index);
						startsBuilder.append("(data);\n");
						src = StringUtils.replace(src, "frx_startVertex", "frx_startVertex" + index);
					} else {
						startsBuilder.append("{ \\NOOP }");
					}

					if (src.contains("frx_endVertex")) {
						endsBuilder.append("frx_endVertex");
						endsBuilder.append(index);
						endsBuilder.append("(data);\n");
						src = StringUtils.replace(src, "frx_endVertex", "frx_endVertex" + index);
					} else {
						endsBuilder.append("{ \\NOOP }");
					}

					implBuilder.append(src);
					implBuilder.append("\n");
				}
			}

			impl = implBuilder.toString();
			starts = startsBuilder.toString();
			ends = endsBuilder.toString();
		}

		baseSource = StringUtils.replace(baseSource, WipShaderData.API_TARGET, impl);
		baseSource = StringUtils.replace(baseSource, WipShaderData.VERTEX_START, starts);
		baseSource = StringUtils.replace(baseSource, WipShaderData.VEREX_END, ends);
		return baseSource;
	}
}
