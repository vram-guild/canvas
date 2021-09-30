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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.varia.GFX;

public enum GlShaderManager {
	INSTANCE;

	{
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: GlShaderManager init");
		}
	}

	private final Object2ObjectOpenHashMap<String, Shader> vertexShaders = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectOpenHashMap<String, Shader> fragmentShaders = new Object2ObjectOpenHashMap<>();

	public static String shaderKey(ResourceLocation shaderSource, ProgramType programType) {
		return String.format("%s.%s", shaderSource.toString(), programType.name);
	}

	public Shader getOrCreateVertexShader(ResourceLocation shaderSource, ProgramType programType) {
		final String shaderKey = shaderKey(shaderSource, programType);

		synchronized (vertexShaders) {
			Shader result = vertexShaders.get(shaderKey);

			if (result == null) {
				result = new GlShader(shaderSource, GFX.GL_VERTEX_SHADER, programType);
				vertexShaders.put(shaderKey, result);
			}

			return result;
		}
	}

	public Shader getOrCreateFragmentShader(ResourceLocation shaderSourceId, ProgramType programType) {
		final String shaderKey = shaderKey(shaderSourceId, programType);

		synchronized (fragmentShaders) {
			Shader result = fragmentShaders.get(shaderKey);

			if (result == null) {
				result = new GlShader(shaderSourceId, GFX.GL_FRAGMENT_SHADER, programType);
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
