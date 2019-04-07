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

package grondag.canvas.core;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.Identifier;

public final class PipelineShaderManager {
    public final static PipelineShaderManager INSTANCE = new PipelineShaderManager();
    private Object2ObjectOpenHashMap<String, PipelineVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<String, PipelineFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

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
        String commonSource = AbstractPipelineShader.getShaderSource(COMMON_SOURCE);
        this.vertexLibrarySource = commonSource + AbstractPipelineShader.getShaderSource(COMMON_VERTEX_SOURCE);
        this.fragmentLibrarySource = commonSource + AbstractPipelineShader.getShaderSource(COMMON_FRAGMENT_SOURCE);
    }

    private String shaderKey(Identifier shaderSource, int spriteDepth, boolean isSolidLayer) {
        return String.format("%s.%s.%s", shaderSource.toString(), spriteDepth, isSolidLayer);
    }

    public PipelineVertexShader getOrCreateVertexShader(Identifier shaderSource, int spriteDepth, boolean isSolidLayer) {
        final String shaderKey = shaderKey(shaderSource, spriteDepth, isSolidLayer);

        synchronized (vertexShaders) {
            PipelineVertexShader result = vertexShaders.get(shaderKey);
            if (result == null) {
                result = new PipelineVertexShader(shaderSource, spriteDepth, isSolidLayer);
                vertexShaders.put(shaderKey, result);
            }
            return result;
        }
    }

    public PipelineFragmentShader getOrCreateFragmentShader(Identifier shaderSourceId, int spriteDepth,
            boolean isSolidLayer) {
        final String shaderKey = shaderKey(shaderSourceId, spriteDepth, isSolidLayer);

        synchronized (fragmentShaders) {
            PipelineFragmentShader result = fragmentShaders.get(shaderKey);
            if (result == null) {
                result = new PipelineFragmentShader(shaderSourceId, spriteDepth, isSolidLayer);
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
