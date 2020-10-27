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

import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import net.minecraft.util.Identifier;

public class ProcessShaders {
	private static final SimpleUnorderedArrayList<ProcessShader> ALL = new SimpleUnorderedArrayList<>();

	public static ProcessShader create(String baseName, String... samplers) {
		final ProcessShader result = new ProcessShader(new Identifier(baseName + ".vert"), new Identifier(baseName + ".frag"), samplers);
		ALL.add(result);
		return result;
	}

	public static void reload() {
		for (final ProcessShader shader : ALL) {
			shader.unload();
		}
	}
}
