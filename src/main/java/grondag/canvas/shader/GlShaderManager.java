/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.shader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.util.Identifier;

public final class GlShaderManager {
	public final static GlShaderManager INSTANCE = new GlShaderManager();
	private final Object2ObjectOpenHashMap<String, GlVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectOpenHashMap<String, GlFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

	String vertexLibrarySource;
	String fragmentLibrarySource;

	public static final Identifier DEFAULT_VERTEX_SOURCE = new Identifier("canvas", "shaders/default.vert");
	public static final Identifier DEFAULT_FRAGMENT_SOURCE = new Identifier("canvas", "shaders/default.frag");
	public static final Identifier WATER_VERTEX_SOURCE = new Identifier("canvas", "shaders/water.vert");
	public static final Identifier WATER_FRAGMENT_SOURCE = new Identifier("canvas", "shaders/water.frag");
	public static final Identifier LAVA_VERTEX_SOURCE = new Identifier("canvas", "shaders/lava.vert");
	public static final Identifier LAVA_FRAGMENT_SOURCE = new Identifier("canvas", "shaders/lava.frag");

	public static final Identifier COMMON_SOURCE = new Identifier("canvas", "shaders/common_lib.glsl");
	public static final Identifier COMMON_VERTEX_SOURCE = new Identifier("canvas", "shaders/vertex_lib.glsl");
	public static final Identifier COMMON_FRAGMENT_SOURCE = new Identifier("canvas", "shaders/fragment_lib.glsl");

	private void loadLibrarySources() {
		final String commonSource = AbstractGlShader.getShaderSource(COMMON_SOURCE);
		vertexLibrarySource = commonSource + AbstractGlShader.getShaderSource(COMMON_VERTEX_SOURCE);
		fragmentLibrarySource = commonSource + AbstractGlShader.getShaderSource(COMMON_FRAGMENT_SOURCE);
	}

	public static String shaderKey(Identifier shaderSource, ShaderContext context) {
		return String.format("%s.%s", shaderSource.toString(), context.name);
	}

	public GlVertexShader getOrCreateVertexShader(Identifier shaderSource, ShaderContext context) {
		final String shaderKey = shaderKey(shaderSource, context);

		synchronized (vertexShaders) {
			GlVertexShader result = vertexShaders.get(shaderKey);
			if (result == null) {
				result = new GlVertexShader(shaderSource, context);
				vertexShaders.put(shaderKey, result);
			}
			return result;
		}
	}

	public GlFragmentShader getOrCreateFragmentShader(Identifier shaderSourceId, ShaderContext context) {
		final String shaderKey = shaderKey(shaderSourceId, context);

		synchronized (fragmentShaders) {
			GlFragmentShader result = fragmentShaders.get(shaderKey);
			if (result == null) {
				result = new GlFragmentShader(shaderSourceId, context);
				fragmentShaders.put(shaderKey, result);
			}
			return result;
		}
	}

	public void forceReload() {
		AbstractGlShader.forceReloadErrors();
		loadLibrarySources();
		fragmentShaders.values().forEach(s -> s.forceReload());
		vertexShaders.values().forEach(s -> s.forceReload());
	}
}
