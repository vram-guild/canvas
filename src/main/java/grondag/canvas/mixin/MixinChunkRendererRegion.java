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
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.apiimpl.util.ChunkRendererRegionExt;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

@Mixin(ChunkRendererRegion.class)
public abstract class MixinChunkRendererRegion implements ChunkRendererRegionExt {
    
    @Shadow protected World world;
    
    private TerrainRenderContext fabric_renderer;

    @Override
    public TerrainRenderContext canvas_renderer() {
        return fabric_renderer;
    }

    @Override
    public void canvas_renderer(TerrainRenderContext renderer) {
        fabric_renderer = renderer;
    }

    @Override
    public BlockView canvas_worldHack() {
        return world;
    }
}
