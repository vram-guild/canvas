package grondag.canvas.core;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import org.joml.Matrix4f;
import grondag.canvas.Configurator;
import grondag.canvas.RenderMaterialImpl;
import grondag.frex.api.UniformRefreshFrequency;
import grondag.canvas.buffering.BufferManager;
import grondag.canvas.mixin.AccessBackgroundRenderer;
import grondag.canvas.mixinext.AccessFogState;
import grondag.canvas.mixinext.FogStateHolder;
import grondag.canvas.mixinext.GameRendererExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public final class PipelineManager {
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

    // NB: initialization sequence works better if these are static (may also
    // prevent a pointer chase)

    /**
     * Current projection matrix. Refreshed from GL state each frame after camera
     * setup in {@link #beforeRenderChunks()}. Unfortunately not immutable so use
     * caution.
     */
    public static final Matrix4f projMatrix = new Matrix4f();

    /**
     * Used to retrieve and store project matrix from GLState. Avoids
     * re-instantiating each frame.
     */
    public static final FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Current mv matrix - set at program activation
     */
    public static final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    /**
     * Current mvp matrix - set at program activation
     */
    public static final Matrix4f mvpMatrix = new Matrix4f();

    /**
     * Current mvp matrix - set at program activation
     */
    public static final FloatBuffer modelViewProjectionMatrixBuffer = BufferUtils.createFloatBuffer(16);

    public static final PipelineManager INSTANCE = new PipelineManager();

    /**
     * Incremented whenever view matrix changes. Used by programs to know if they
     * must update.
     */
    public static int viewMatrixVersionCounter = Integer.MIN_VALUE;

    private final RenderPipeline[] pipelines = new RenderPipeline[PipelineManager.MAX_PIPELINES];

    private int pipelineCount = 0;

    private final RenderPipeline[] defaultPipelines = new RenderPipeline[RenderMaterialImpl.MAX_SPRITE_DEPTH];
    private final RenderPipeline waterPipeline;
    private final RenderPipeline lavaPipeline;
    public final RenderPipeline defaultSinglePipeline;

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

    private PipelineManager() {
        super();

        // add default pipelines
        for (int i = 0; i < RenderMaterialImpl.MAX_SPRITE_DEPTH; i++) {
            defaultPipelines[i] = (RenderPipeline) this
                    .createPipeline(i + 1, PipelineShaderManager.INSTANCE.DEFAULT_VERTEX_SOURCE,
                            PipelineShaderManager.INSTANCE.DEFAULT_FRAGMENT_SOURCE);
        }
        this.waterPipeline = this.createPipeline(1, "/assets/canvas/shader/water.vert",
                "/assets/canvas/shader/water.frag");
        this.lavaPipeline = this.createPipeline(1, "/assets/canvas/shader/lava.vert",
                "/assets/canvas/shader/lava.frag");
        this.defaultSinglePipeline = defaultPipelines[0];
    }

    public void forceReload() {
        for (int i = 0; i < this.pipelineCount; i++) {
            this.pipelines[i].forceReload();
        }
    }

    public final synchronized RenderPipeline createPipeline(int spriteDepth, String vertexShader,
            String fragmentShader) {

        if (this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;

        if (this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;
        RenderPipeline result = new RenderPipeline(this.pipelineCount++, vertexShader, fragmentShader, spriteDepth);
        this.pipelines[result.getIndex()] = result;

        addStandardUniforms(result);

        return result;
    }

    public final RenderPipeline getPipeline(int pipelineIndex) {
        return pipelines[pipelineIndex];
    }

    public final RenderPipeline getDefaultPipeline(int spriteDepth) {
        return pipelines[spriteDepth - 1];
    }

    public final RenderPipeline getWaterPipeline() {
        return Configurator.fancyFluids ? this.waterPipeline : this.defaultSinglePipeline;
    }

    public final RenderPipeline getLavaPipeline() {
        return Configurator.fancyFluids ? this.lavaPipeline : this.defaultSinglePipeline;
    }

    public RenderPipeline getPipelineByIndex(int index) {
        return this.pipelines[index];
    }

    /**
     * The number of pipelines currently registered.
     */
    public final int pipelineCount() {
        return this.pipelineCount;
    }

    private void addStandardUniforms(RenderPipeline pipeline) {
        pipeline.uniform1f("u_time", UniformRefreshFrequency.PER_FRAME, u -> u.set(renderSeconds));

        pipeline.uniformSampler2d("u_textures", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));

        pipeline.uniformSampler2d("u_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(1));

        pipeline.uniform3f("u_eye_position", UniformRefreshFrequency.PER_FRAME, u -> {
            Vec3d eyePos = MinecraftClient.getInstance().player.getCameraPosVec(fractionalTicks);
            u.set((float) eyePos.x, (float) eyePos.y, (float) eyePos.z);
        });

        pipeline.uniform3f("u_fogAttributes", UniformRefreshFrequency.PER_TICK, u -> {
            AccessFogState fogState = FogStateHolder.INSTANCE;
            u.set(fogState.getEnd(), fogState.getEnd() - fogState.getStart(),
                    // zero signals shader to use linear fog
                    fogState.getMode() == GlStateManager.FogMode.LINEAR.glValue ? 0f : fogState.getDensity());
        });

        pipeline.uniform3f("u_fogColor", UniformRefreshFrequency.PER_TICK, u -> {
            AccessBackgroundRenderer fh = (AccessBackgroundRenderer) ((GameRendererExt) MinecraftClient
                    .getInstance().gameRenderer).canvas_fogHelper();
            u.set(fh.getRed(), fh.getGreen(), fh.getBlue());
        });

        pipeline.setupModelViewUniforms();
    }

    /**
     * Called just before terrain setup each frame after camera, fog and projection
     * matrix are set up,
     */
    public void prepareForFrame(Entity cameraEntity, float fractionalTicks) {
        this.fractionalTicks = fractionalTicks;

        BufferManager.prepareForFrame();

        projectionMatrixBuffer.position(0);
        GlStateManager.getMatrix(GL11.GL_PROJECTION_MATRIX, projectionMatrixBuffer);
        projMatrix.set(projectionMatrixBuffer);
        //CanvasGlHelper.loadTransposeQuickly(projectionMatrixBuffer, projMatrix);

        assert cameraEntity != null;
        assert cameraEntity.getEntityWorld() != null;

        if (cameraEntity == null || cameraEntity.getEntityWorld() == null)
            return;

        computeRenderSeconds(cameraEntity);
    }

    private void computeRenderSeconds(Entity cameraEntity) {
        renderSeconds = (float) ((cameraEntity.getEntityWorld().getTime() - baseWorldTime + fractionalTicks) / 20);
    }

    public void onGameTick(MinecraftClient mc) {
        for (int i = 0; i < this.pipelineCount; i++) {
            pipelines[i].onGameTick();
        }
    }

    public float renderSeconds() {
        return this.renderSeconds;
    }

    public static final void setModelViewMatrix(Matrix4f mvMatrix) {
        updateModelViewMatrix(mvMatrix);

        updateModelViewProjectionMatrix(mvMatrix);

        viewMatrixVersionCounter++;
    }

    private static final void updateModelViewMatrix(Matrix4f mvMatrix) {
        mvMatrix.get(modelViewMatrixBuffer);
    }

    private static final void updateModelViewProjectionMatrix(Matrix4f mvMatrix) {
        projMatrix.mul(mvMatrix, mvpMatrix);
        mvpMatrix.get(modelViewProjectionMatrixBuffer);
    }
}
