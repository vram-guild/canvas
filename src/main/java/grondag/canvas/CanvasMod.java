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

package grondag.canvas;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;

import grondag.canvas.apiimpl.Canvas;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;

//TODO: allow for shaders to be defined based on Block materials. Apply only to default material.

//FIX: lighting on dodecs (again)
//FIX: depth for campfire smoke / water
//FIX: track lightmap occupancy
//TODO: #include preprocessor directive for glsl
//TODO: separate all uniform/variant-dependent elements to separate glsl libs
//TODO: configurable disable chunk matrix
//TODO: configurable compact vertex formats - GPU side  white, face, unlit, pixel-aligned, etc.
//TODO: configurable cutout single pass, separate pass may give early cull in solid
//FEAT: complete item rendering
//FEAT: custom samplers
//FEAT: GLSL pre-processor
//FEAT: fancy water
//FEAT: fancy lava
//PERF: disable lava/water texture animation (configurable)
//FEAT: GLSL library and docs
//PERF: improve light smoothing performance
//PERF: manage buffers to avoid heap fragmentation
//FEAT: colored lights
//FEAT: gen purpose tessellator
//FEAT: configurable compressed vertex formats - CPU side (maybe wait for Brocade Mesh)
//FEAT: per chunk occlusion mesh - for sky shadow mask
//FEAT: per chunk depth mesh - addendum to occlusion mesh to render for depth pass - includes translucent cutout
//FEAT: first person dynamic light
//FEAT: weather/season/biome(?) uniforms/attributes

public class CanvasMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Configurator.init();
        RendererAccess.INSTANCE.registerRenderer(Canvas.INSTANCE);
        InvalidateRenderStateCallback.EVENT.register(Canvas.INSTANCE::reload);
        if(Configurator.debugNativeMemoryAllocation) {
            LOG.warn("Canvas is configured to enable native memory debug. This WILL cause slow performance and other issues.  Debug output will print at game exit.");
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
        }
    }

    public static final String MODID = "canvas";
    
    public static final Logger LOG = LogManager.getLogger("Canvas");
}
