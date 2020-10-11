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

	// PERF: combine passes somehow vs building new set for start/end/impl

	private String preprocessFragmentSource(ResourceManager resourceManager, String baseSource) {
		String starts;
		String impl;

		if (materials.isEmpty()) {
			starts = "// NOOP";
			impl = "// NOOP";
		} else if (materials.size() == 1) {
			starts = "\tfrx_startFragment(fragData);";
			final int index = materials.iterator().next().fragmentShaderIndex;
			impl = loadShaderSource(resourceManager, WipMaterialShaderManager.fragmentIndex.fromHandle(index));
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
					startsBuilder.append("(fragData);\n");

					String src = loadShaderSource(resourceManager, WipMaterialShaderManager.fragmentIndex.fromHandle(index));
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
		baseSource = StringUtils.replace(baseSource, WipShaderData.API_TARGET, "");
		baseSource = StringUtils.replace(baseSource, WipShaderData.VERTEX_START, "");
		baseSource = StringUtils.replace(baseSource, WipShaderData.VEREX_END,  "");
		return baseSource;
	}

	private String vertexStartSource() {
		final IntOpenHashSet ids = new IntOpenHashSet();

		// WIP generate switch/if-else from materials

		//		System.out.println();
		//		System.out.println("start");
		//		for (final WipMaterialShaderImpl mat : materials) {
		//			ids.add(mat.vertexShaderIndex);
		//			System.out.println(mat.vertexShaderIndex);
		//		}
		//		System.out.println();

		/* example

		if (cv_programId == 0) { frx_startVertex0(data) }
		else if (cv_programId == 1) { frx_startVertex1(data)}
		else if (cv_programId == 2) { frx_startVertex2(data)};

		 */

		return "";
	}

	private String vertexEndSource() {
		// WIP generate switch/if-else from materials
		return "";
	}

	private String implementationSources() {
		// WIP generate relabeled methods from materials

		/* example

		void frx_startVertex0(inout frx_VertexData data) {

		}

		 */

		return "";
	}
}
