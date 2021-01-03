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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;

public enum GlShaderManager {
	INSTANCE;

	{
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: GlShaderManager init");
		}
	}

	private final Object2ObjectOpenHashMap<String, Shader> vertexShaders = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectOpenHashMap<String, Shader> fragmentShaders = new Object2ObjectOpenHashMap<>();

	public static String shaderKey(Identifier shaderSource, ProgramType programType) {
		return String.format("%s.%s", shaderSource.toString(), programType.name);
	}

	public Shader getOrCreateVertexShader(Identifier shaderSource, ProgramType programType) {
		final String shaderKey = shaderKey(shaderSource, programType);

		synchronized (vertexShaders) {
			Shader result = vertexShaders.get(shaderKey);

			if (result == null) {
				result = new GlShader(shaderSource, GL21.GL_VERTEX_SHADER, programType);
				vertexShaders.put(shaderKey, result);
			}

			return result;
		}
	}

	public Shader getOrCreateFragmentShader(Identifier shaderSourceId, ProgramType programType) {
		final String shaderKey = shaderKey(shaderSourceId, programType);

		synchronized (fragmentShaders) {
			Shader result = fragmentShaders.get(shaderKey);

			if (result == null) {
				result = new GlShader(shaderSourceId, GL21.GL_FRAGMENT_SHADER, programType);
				fragmentShaders.put(shaderKey, result);
			}

			return result;
		}
	}

	public void reload() {
		fragmentShaders.values().forEach(s -> s.forceReload());
		vertexShaders.values().forEach(s -> s.forceReload());
	}
}
