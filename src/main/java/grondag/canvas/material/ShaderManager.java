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
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.apiimpl.util.ShadeFaceData;
import grondag.canvas.varia.FogStateExtHolder;
import grondag.canvas.varia.LightmapHD;
import grondag.canvas.varia.SmoothLightmapTexture;
import grondag.canvas.varia.WorldDataManager;
import grondag.frex.api.material.UniformRefreshFrequency;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;


public final class ShaderManager implements ClientTickCallback {
    public static final int MAX_SHADERS = Configurator.maxShaders;

    public static final ShaderManager INSTANCE = new ShaderManager();

    private final MaterialShaderImpl[] shaders = new MaterialShaderImpl[ShaderManager.MAX_SHADERS];

    private int shaderCount = 0;

    private final MaterialShaderImpl defaultShader;
    private final MaterialShaderImpl waterShader;
    private final MaterialShaderImpl lavaShader;

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
    
    private ShaderManager() {
        super();

        ClientTickCallback.EVENT.register(this);
        
        // add default shaders
        defaultShader= this.create(GlShaderManager.DEFAULT_VERTEX_SOURCE, GlShaderManager.DEFAULT_FRAGMENT_SOURCE);
        waterShader = this.create(GlShaderManager.WATER_VERTEX_SOURCE, GlShaderManager.WATER_FRAGMENT_SOURCE);
        lavaShader = this.create(GlShaderManager.LAVA_VERTEX_SOURCE, GlShaderManager.LAVA_FRAGMENT_SOURCE);
    }

    public void updateEmissiveColor(int color) {
        emissiveColor.set((color & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, ((color >> 16) & 0xFF) / 255f);
    }
    
    public void forceReload() {
        GlShaderManager.INSTANCE.forceReload();
        VertexEncoder.forceReload();
        SmoothLightmapTexture.instance().forceReload();
        ShadeFaceData.forceReload();
        LightmapHD.forceReload();
        for (int i = 0; i < this.shaderCount; i++) {
            this.shaders[i].forceReload();
        }
    }

    public final synchronized MaterialShaderImpl create(Identifier vertexShaderSource, Identifier fragmentShaderSource) {

        if(vertexShaderSource == null) {
            vertexShaderSource = GlShaderManager.DEFAULT_VERTEX_SOURCE;
        }
        
        if(fragmentShaderSource == null) {
            fragmentShaderSource = GlShaderManager.DEFAULT_FRAGMENT_SOURCE;
        }
        
        if (this.shaderCount >= ShaderManager.MAX_SHADERS) {
            throw new IndexOutOfBoundsException(I18n.translate("error.canvas.max_materials_exceeded"));
        }

        MaterialShaderImpl result = new MaterialShaderImpl(this.shaderCount++, vertexShaderSource, fragmentShaderSource);
        this.shaders[result.getIndex()] = result;

        addStandardUniforms(result);

        return result;
    }

    public final MaterialShaderImpl get(int index) {
        return shaders[index];
    }

    public final MaterialShaderImpl getDefault() {
        return defaultShader;
    }

    public final MaterialShaderImpl getWater() {
        return Configurator.fancyFluids ? this.waterShader : this.defaultShader;
    }

    public final MaterialShaderImpl getLava() {
        return Configurator.fancyFluids ? this.lavaShader : this.defaultShader;
    }

    /**
     * The number of shaders currently registered.
     */
    public final int shaderCount() {
        return this.shaderCount;
    }

    public final int tickIndex() {
        return tickIndex;
    }
    
    public final int frameIndex() {
        return frameIndex;
    }
    
    private void addStandardUniforms(MaterialShaderImpl shader) {
        shader.uniformArrayf("u_world", UniformRefreshFrequency.PER_TICK, u -> u.set(WorldDataManager.uniformData()), WorldDataManager.LENGTH);
        
        shader.uniform1f("u_time", UniformRefreshFrequency.PER_FRAME, u -> u.set(renderSeconds));

        shader.uniformSampler2d("u_textures", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));

        shader.uniformSampler2d("u_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(1));
        
        shader.uniformSampler2d("u_dither", UniformRefreshFrequency.ON_LOAD, u -> u.set(2));

        //UGLY: needs a better GLSL name
        shader.uniformSampler2d("u_utility", UniformRefreshFrequency.ON_LOAD, u -> u.set(2));
        
        shader.uniform4f("u_emissiveColor", UniformRefreshFrequency.PER_FRAME, u -> {
            u.set(emissiveColor.x, emissiveColor.y, emissiveColor.z, 1f);
        });
        
        shader.uniform3f("u_eye_position", UniformRefreshFrequency.PER_FRAME, u -> {
            Vec3d eyePos = MinecraftClient.getInstance().player.getCameraPosVec(fractionalTicks);
            u.set((float) eyePos.x, (float) eyePos.y, (float) eyePos.z);
        });
        
        shader.uniform1i("u_fogMode", UniformRefreshFrequency.PER_FRAME, u -> {
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
        for (int i = 0; i < this.shaderCount; i++) {
            shaders[i].onGameTick();
        }
        
        //UGLY: put this somwhere else? Central tick handler?
        SmoothLightmapTexture.instance().tick();
    }
    
    public void onRenderTick() {
        frameIndex++;
        for (int i = 0; i < this.shaderCount; i++) {
            shaders[i].onRenderTick();
        }
    }

    public float renderSeconds() {
        return this.renderSeconds;
    }
}
