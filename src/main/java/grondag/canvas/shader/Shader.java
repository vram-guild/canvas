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

import net.minecraft.util.Identifier;

public interface Shader {
	/**
	 * Forces the shader to be reloaded the next time it is attached.
	 */
	void forceReload();

	/**
	 * Binds this shader.
	 *
	 * @param program The program object to which this shader object will be attached
	 * @return Was this successful
	 */
	boolean attach(int program);

	/**
	 * @param type Uniform type
	 * @param name Uniform name
	 * @return Does this shader contain the provided uniform
	 */
	boolean containsUniformSpec(String type, String name);

	/**
	 * @return The shader source location, typically for debugging
	 */
	Identifier getShaderSourceId();
}
