package grondag.canvas.core;

import org.joml.Vector3f;

import grondag.canvas.Configurator;
import grondag.canvas.RenderMaterialImpl;
import grondag.canvas.buffering.VboBufferManager;
import grondag.canvas.mixinext.FogStateHolder;
import grondag.frex.api.extended.UniformRefreshFrequency;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class PipelineManager implements ClientTickCallback {
    static final PipelineVertexFormat[] FORMATS = PipelineVertexFormat.values();
    
    /**
     * Will always be 1, defined to clarify intent in code.
     */
    public static final int FIRST_CUSTOM_PIPELINE_INDEX = 1;

    /**
     * Will always be 0, defined to clarify intent in code.
     */
    public static final int VANILLA_MC_PIPELINE_INDEX = 0;

    public static final int MAX_PIPELINES = Configurator.maxPipelines;

    public static final PipelineManager INSTANCE = new PipelineManager();

    private final RenderPipelineImpl[] pipelines = new RenderPipelineImpl[PipelineManager.MAX_PIPELINES];

    private int pipelineCount = 0;

    private final RenderPipelineImpl[] defaultPipelines = new RenderPipelineImpl[RenderMaterialImpl.MAX_SPRITE_DEPTH];
    private final RenderPipelineImpl waterPipeline;
    private final RenderPipelineImpl lavaPipeline;
    public final RenderPipelineImpl defaultSinglePipeline;

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
     * Gamma-corrected max light from lightmap texture.  
     * Updated whenever lightmap texture is updated.
     */
    private Vector3f emissiveColor = new Vector3f(1f, 1f, 1f);
    
    private PipelineManager() {
        super();

        ClientTickCallback.EVENT.register(this);
        
        // add default pipelines
        for (int i = 0; i < RenderMaterialImpl.MAX_SPRITE_DEPTH; i++) {
            defaultPipelines[i] = (RenderPipelineImpl) this
                    .createPipeline(i + 1, PipelineShaderManager.DEFAULT_VERTEX_SOURCE,
                            PipelineShaderManager.DEFAULT_FRAGMENT_SOURCE);
        }
        this.waterPipeline = this.createPipeline(1, PipelineShaderManager.WATER_VERTEX_SOURCE, PipelineShaderManager.WATER_FRAGMENT_SOURCE);
        this.lavaPipeline = this.createPipeline(1, PipelineShaderManager.LAVA_VERTEX_SOURCE, PipelineShaderManager.LAVA_FRAGMENT_SOURCE);
        this.defaultSinglePipeline = defaultPipelines[0];
    }

    public void updateEmissiveColor(int color) {
        emissiveColor.set((color & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, ((color >> 16) & 0xFF) / 255f);
    }
    
    public void forceReload() {
        PipelineShaderManager.INSTANCE.forceReload();
        for (int i = 0; i < this.pipelineCount; i++) {
            this.pipelines[i].forceReload();
        }
    }

    public final synchronized RenderPipelineImpl createPipeline(int spriteDepth, Identifier vertexShaderSource,
            Identifier fragmentShaderSource) {

        if(vertexShaderSource == null) {
            vertexShaderSource = PipelineShaderManager.DEFAULT_VERTEX_SOURCE;
        }
        
        if(fragmentShaderSource == null) {
            fragmentShaderSource = PipelineShaderManager.DEFAULT_FRAGMENT_SOURCE;
        }
        
        if (this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;

        if (this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;
        RenderPipelineImpl result = new RenderPipelineImpl(this.pipelineCount++, vertexShaderSource, fragmentShaderSource, spriteDepth);
        this.pipelines[result.getIndex()] = result;

        addStandardUniforms(result);

        return result;
    }

    public final RenderPipelineImpl getPipeline(int pipelineIndex) {
        return pipelines[pipelineIndex];
    }

    public final RenderPipelineImpl getDefaultPipeline(int spriteDepth) {
        return pipelines[spriteDepth - 1];
    }

    public final RenderPipelineImpl getWaterPipeline() {
        return Configurator.fancyFluids ? this.waterPipeline : this.defaultSinglePipeline;
    }

    public final RenderPipelineImpl getLavaPipeline() {
        return Configurator.fancyFluids ? this.lavaPipeline : this.defaultSinglePipeline;
    }

    public RenderPipelineImpl getPipelineByIndex(int index) {
        return this.pipelines[index];
    }

    /**
     * The number of pipelines currently registered.
     */
    public final int pipelineCount() {
        return this.pipelineCount;
    }

    private void addStandardUniforms(RenderPipelineImpl pipeline) {
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
            u.set(FogStateHolder.INSTANCE.getMode());
        });
    }

    /**
     * Called just before terrain setup each frame after camera, fog and projection
     * matrix are set up,
     */
    public void prepareForFrame(Camera camera) {
        this.fractionalTicks = MinecraftClient.getInstance().getTickDelta();

        VboBufferManager.prepareForFrame();

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
        for (int i = 0; i < this.pipelineCount; i++) {
            pipelines[i].onGameTick();
        }
    }
    
    public void onRenderTick() {
        for (int i = 0; i < this.pipelineCount; i++) {
            pipelines[i].onRenderTick();
        }
    }

    public float renderSeconds() {
        return this.renderSeconds;
    }
}
