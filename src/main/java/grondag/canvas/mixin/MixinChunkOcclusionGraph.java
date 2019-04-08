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

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;

import grondag.canvas.hooks.VisibilityMap;
import grondag.canvas.mixinext.ChunkVisibility;
import net.minecraft.client.render.chunk.ChunkOcclusionGraph;

@Mixin(ChunkOcclusionGraph.class)
public abstract class MixinChunkOcclusionGraph implements ChunkVisibility {
    private Object visibilityData = null;

    @Override
    public Object canvas_visibilityData() {
        return visibilityData;
    }

    @Override
    public void canvas_visibilityData(Object data) {
        canvas_releaseVisibilityData();
        visibilityData = data;
    }

    /** reuse arrays to prevent garbage build up */
    @Override
    public void canvas_releaseVisibilityData() {
        Object prior = visibilityData;
        if (prior != null && prior instanceof VisibilityMap) {
            VisibilityMap.release((VisibilityMap) prior);
            visibilityData = null;
        }
    }

    @Override
    protected void finalize() {
        canvas_releaseVisibilityData();
    }
}
