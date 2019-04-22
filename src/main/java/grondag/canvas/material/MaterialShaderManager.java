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

import org.joml.Vector3f;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.varia.FogStateExtHolder;
import grondag.frex.api.material.UniformRefreshFrequency;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class MaterialShaderManager implements ClientTickCallback {
    public static final MaterialVertexFormat[] FORMATS = MaterialVertexFormat.values();
    
    /**
     * Will always be 1, defined to clarify intent in code.
     */
    public static final int FIRST_CUSTOM_PIPELINE_INDEX = 1;

    /**
     * Will always be 0, defined to clarify intent in code.
     */
    public static final int VANILLA_MC_PIPELINE_INDEX = 0;

    public static final int MAX_PIPELINES = Configurator.maxPipelines;

    public static final MaterialShaderManager INSTANCE = new MaterialShaderManager();

    private final MaterialShaderImpl[] pipelines = new MaterialShaderImpl[MaterialShaderManager.MAX_PIPELINES];

    private int pipelineCount = 0;

    private final MaterialShaderImpl[] defaultPipelines = new MaterialShaderImpl[RenderMaterialImpl.MAX_SPRITE_DEPTH];
    private final MaterialShaderImpl waterPipeline;
    private final MaterialShaderImpl lavaPipeline;
    public final MaterialShaderImpl defaultSinglePipeline;

    /**
     * The number of seconds this world has been rendering since the last render
     * reload, including fractional seconds.
     * <p>
     * 
     * Based on total world time, but shifted to originate from start of this game
     * session.
     */
    private float renderSeconds;

    /**
     * World time ticks at last render reload..
     */
    private long baseWorldTime;

    /**
     * Frames are (hopefully) shorter than a client tick. This is the fraction of a
     * tick that has elapsed since the last complete client tick.
     */
    private float fractionalTicks;

    /**
     * Count of client ticks observed by renderer since last restart.
     */
    private int tickIndex = 0;
    
    /**
     * Count of frames observed by renderer since last restart.
     */
    private int frameIndex = 0;
    
    /**
     * Gamma-corrected max light from lightmap texture.  
     * Updated whenever lightmap texture is updated.
     */
    private Vector3f emissiveColor = new Vector3f(1f, 1f, 1f);
    
    private MaterialShaderManager() {
        super();

        ClientTickCallback.EVENT.register(this);
        
        // add default pipelines
        for (int i = 0; i < RenderMaterialImpl.MAX_SPRITE_DEPTH; i++) {
            defaultPipelines[i] = (MaterialShaderImpl) this
                    .create(i + 1, GlShaderManager.DEFAULT_VERTEX_SOURCE,
                            GlShaderManager.DEFAULT_FRAGMENT_SOURCE);
        }
        this.waterPipeline = this.create(1, GlShaderManager.WATER_VERTEX_SOURCE, GlShaderManager.WATER_FRAGMENT_SOURCE);
        this.lavaPipeline = this.create(1, GlShaderManager.LAVA_VERTEX_SOURCE, GlShaderManager.LAVA_FRAGMENT_SOURCE);
        this.defaultSinglePipeline = defaultPipelines[0];
    }

    public void updateEmissiveColor(int color) {
        emissiveColor.set((color & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, ((color >> 16) & 0xFF) / 255f);
    }
    
    public void forceReload() {
        GlShaderManager.INSTANCE.forceReload();
        for (int i = 0; i < this.pipelineCount; i++) {
            this.pipelines[i].forceReload();
        }
    }

    public final synchronized MaterialShaderImpl create(int spriteDepth, Identifier vertexShaderSource,
            Identifier fragmentShaderSource) {

        if(vertexShaderSource == null) {
            vertexShaderSource = GlShaderManager.DEFAULT_VERTEX_SOURCE;
        }
        
        if(fragmentShaderSource == null) {
            fragmentShaderSource = GlShaderManager.DEFAULT_FRAGMENT_SOURCE;
        }
        
        if (this.pipelineCount >= MaterialShaderManager.MAX_PIPELINES) {
            throw new IndexOutOfBoundsException(I18n.translate("error.canvas.max_materials_exceeded"));
        }

        MaterialShaderImpl result = new MaterialShaderImpl(this.pipelineCount++, vertexShaderSource, fragmentShaderSource, spriteDepth);
        this.pipelines[result.getIndex()] = result;

        addStandardUniforms(result);

        return result;
    }

    public final MaterialShaderImpl getPipeline(int pipelineIndex) {
        return pipelines[pipelineIndex];
    }

    public final MaterialShaderImpl getDefaultPipeline(int spriteDepth) {
        return pipelines[spriteDepth - 1];
    }

    public final MaterialShaderImpl getWaterPipeline() {
        return Configurator.fancyFluids ? this.waterPipeline : this.defaultSinglePipeline;
    }

    public final MaterialShaderImpl getLavaPipeline() {
        return Configurator.fancyFluids ? this.lavaPipeline : this.defaultSinglePipeline;
    }

    public MaterialShaderImpl getPipelineByIndex(int index) {
        return this.pipelines[index];
    }

    /**
     * The number of pipelines currently registered.
     */
    public final int pipelineCount() {
        return this.pipelineCount;
    }

    public final int tickIndex() {
        return tickIndex;
    }
    
    public final int frameIndex() {
        return frameIndex;
    }
    
    private void addStandardUniforms(MaterialShaderImpl pipeline) {
        pipeline.uniform1f("u_time", UniformRefreshFrequency.PER_FRAME, u -> u.set(renderSeconds));

        pipeline.uniformSampler2d("u_textures", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));

        pipeline.uniformSampler2d("u_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(1));

        pipeline.uniform4f("u_emissiveColor", UniformRefreshFrequency.PER_FRAME, u -> {
            u.set(emissiveColor.x, emissiveColor.y, emissiveColor.z, 1f);
        });
        
        pipeline.uniform3f("u_eye_position", UniformRefreshFrequency.PER_FRAME, u -> {
            Vec3d eyePos = MinecraftClient.getInstance().player.getCameraPosVec(fractionalTicks);
            u.set((float) eyePos.x, (float) eyePos.y, (float) eyePos.z);
        });
        
        pipeline.uniform1i("u_fogMode", UniformRefreshFrequency.PER_FRAME, u -> {
            u.set(FogStateExtHolder.INSTANCE.getMode());
        });
    }

    /**
     * Called just before terrain setup each frame after camera, fog and projection
     * matrix are set up,
     */
    public void prepareForFrame(Camera camera) {
        this.fractionalTicks = MinecraftClient.getInstance().getTickDelta();

        Entity cameraEntity = camera.getFocusedEntity();
        assert cameraEntity != null;
        assert cameraEntity.getEntityWorld() != null;

        if (cameraEntity == null || cameraEntity.getEntityWorld() == null)
            return;

        computeRenderSeconds(cameraEntity);
        
        onRenderTick();
    }

    private void computeRenderSeconds(Entity cameraEntity) {
        renderSeconds = (float) ((cameraEntity.getEntityWorld().getTime() - baseWorldTime + fractionalTicks) / 20);
    }

    @Override
    public void tick(MinecraftClient client) {
        tickIndex++;
        for (int i = 0; i < this.pipelineCount; i++) {
            pipelines[i].onGameTick();
        }
    }
    
    public void onRenderTick() {
        frameIndex++;
        for (int i = 0; i < this.pipelineCount; i++) {
            pipelines[i].onRenderTick();
        }
    }

    public float renderSeconds() {
        return this.renderSeconds;
    }
}
