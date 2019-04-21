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

package grondag.canvas.material;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.Identifier;

public final class GlShaderManager {
    public final static GlShaderManager INSTANCE = new GlShaderManager();
    private Object2ObjectOpenHashMap<String, GlVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<String, GlFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

    String vertexLibrarySource;
    String fragmentLibrarySource;

    public static final Identifier DEFAULT_VERTEX_SOURCE = new Identifier("canvas", "shader/default.vert");
    public static final Identifier DEFAULT_FRAGMENT_SOURCE = new Identifier("canvas", "shader/default.frag");
    public static final Identifier WATER_VERTEX_SOURCE = new Identifier("canvas", "shader/water.vert");
    public static final Identifier WATER_FRAGMENT_SOURCE = new Identifier("canvas", "shader/water.frag");
    public static final Identifier LAVA_VERTEX_SOURCE = new Identifier("canvas", "shader/lava.vert");
    public static final Identifier LAVA_FRAGMENT_SOURCE = new Identifier("canvas", "shader/lava.frag");
    
    public static final Identifier COMMON_SOURCE = new Identifier("canvas", "shader/common_lib.glsl");
    public static final Identifier COMMON_VERTEX_SOURCE = new Identifier("canvas", "shader/vertex_lib.glsl");
    public static final Identifier COMMON_FRAGMENT_SOURCE = new Identifier("canvas", "shader/fragment_lib.glsl");
    
    private void loadLibrarySources() {
        String commonSource = AbstractGlShader.getShaderSource(COMMON_SOURCE);
        this.vertexLibrarySource = commonSource + AbstractGlShader.getShaderSource(COMMON_VERTEX_SOURCE);
        this.fragmentLibrarySource = commonSource + AbstractGlShader.getShaderSource(COMMON_FRAGMENT_SOURCE);
    }

    public static String shaderKey(Identifier shaderSource, int spriteDepth, ShaderContext context) {
        return String.format("%s.%s.%s", shaderSource.toString(), spriteDepth, context.ordinal());
    }

    public GlVertexShader getOrCreateVertexShader(Identifier shaderSource, int spriteDepth, ShaderContext context) {
        final String shaderKey = shaderKey(shaderSource, spriteDepth, context);

        synchronized (vertexShaders) {
            GlVertexShader result = vertexShaders.get(shaderKey);
            if (result == null) {
                result = new GlVertexShader(shaderSource, spriteDepth, context);
                vertexShaders.put(shaderKey, result);
            }
            return result;
        }
    }

    public GlFragmentShader getOrCreateFragmentShader(Identifier shaderSourceId, int spriteDepth,
            ShaderContext context) {
        final String shaderKey = shaderKey(shaderSourceId, spriteDepth, context);

        synchronized (fragmentShaders) {
            GlFragmentShader result = fragmentShaders.get(shaderKey);
            if (result == null) {
                result = new GlFragmentShader(shaderSourceId, spriteDepth, context);
                fragmentShaders.put(shaderKey, result);
            }
            return result;
        }
    }

    public void forceReload() {
        this.loadLibrarySources();
        this.fragmentShaders.values().forEach(s -> s.forceReload());
        this.vertexShaders.values().forEach(s -> s.forceReload());
    }
}
