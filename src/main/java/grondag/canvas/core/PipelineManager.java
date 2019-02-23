package grondag.acuity.api;

import java.nio.FloatBuffer;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Configurator;
import grondag.acuity.buffering.MappedBufferStore;
import grondag.acuity.core.PipelineShaderManager;
import grondag.acuity.opengl.OpenGlHelperExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PipelineManager implements IPipelineManager
{
    /**
     * Will always be 1, defined to clarify intent in code.
     */
    public static final int FIRST_CUSTOM_PIPELINE_INDEX = 1;

    /**
     * Will always be 0, defined to clarify intent in code.
     */
    public static final int VANILLA_MC_PIPELINE_INDEX = 0;

    public static final int MAX_PIPELINES = Configurator.maxPipelines;
    
    
    // NB: initialization sequence works better if these are static (may also prevent a pointer chase)
    
    /**
     * Current projection matrix. Refreshed from GL state each frame after camera setup
     * in {@link #beforeRenderChunks()}. Unfortunately not immutable so use caution.
     */
    public static final Matrix4f projMatrix = new Matrix4f();
    
    /**
     * Used to retrieve and store project matrix from GLState. Avoids re-instantiating each frame.
     */
    public static final FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    /**
     * Current mv matrix - set at program activation
     */
    public static final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    private static final long modelViewMatrixBufferAddress = MemoryUtil.getAddress(modelViewMatrixBuffer);
    
    /**
     * Current mvp matrix - set at program activation
     */
    public static final FloatBuffer modelViewProjectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    private static final long modelViewProjectionMatrixBufferAddress = MemoryUtil.getAddress(modelViewProjectionMatrixBuffer);
    
    public static final PipelineManager INSTANCE = new PipelineManager();
    
    /**
     * Incremented whenever view matrix changes. Used by programs to know if they must update.
     */
    public static int viewMatrixVersionCounter = Integer.MIN_VALUE;
    
    private final RenderPipeline[] pipelines = new RenderPipeline[PipelineManager.MAX_PIPELINES];
    
    private int pipelineCount = 0;
    
    private final RenderPipeline[] defaultPipelines = new RenderPipeline[TextureFormat.values().length];
    private final RenderPipeline waterPipeline;
    private final RenderPipeline lavaPipeline;
    public final RenderPipeline defaultSinglePipeline;
    
    private float worldTime;
    private float partialTicks;
    
    
    /**
     * See {@link #onRenderTick(RenderTickEvent)}
     */
    private boolean didUpdatePipelinesThisFrame = false;
    
    @SuppressWarnings("null")
    private PipelineManager()
    {
        super();
        
        // add default pipelines
        for(TextureFormat textureFormat : TextureFormat.values())
        {
            defaultPipelines[textureFormat.ordinal()] = (RenderPipeline) this.createPipeline(
                    textureFormat, 
                    PipelineShaderManager.INSTANCE.DEFAULT_VERTEX_SOURCE,
                    PipelineShaderManager.INSTANCE.DEFAULT_FRAGMENT_SOURCE).finish();
        }
        this.waterPipeline = this.createPipeline(TextureFormat.SINGLE, "/assets/acuity/shader/water.vert", "/assets/acuity/shader/water.frag");
        this.lavaPipeline = this.createPipeline(TextureFormat.SINGLE, "/assets/acuity/shader/lava.vert", "/assets/acuity/shader/lava.frag");
        this.defaultSinglePipeline = defaultPipelines[0];
    }
    
    public void forceReload()
    {
        for(int i = 0; i < this.pipelineCount; i++)
        {
            this.pipelines[i].forceReload();
        }
    }
    
    
    @Nullable
    @Override
    public final synchronized RenderPipeline createPipeline(
            TextureFormat textureFormat, 
            String vertexShader, 
            String fragmentShader)
    {
        
        if(this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;
        
        if(this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;
        RenderPipeline result = new RenderPipeline(this.pipelineCount++, vertexShader, fragmentShader, textureFormat);
        this.pipelines[result.getIndex()] = result;
        
        addStandardUniforms(result);
        
        return result;
    }
    
    public final RenderPipeline getPipeline(int pipelineIndex)
    {
        return pipelines[pipelineIndex];
    }

    @Override
    public final IRenderPipeline getDefaultPipeline(TextureFormat textureFormat)
    {
        return pipelines[textureFormat.ordinal()];
    }
    
    @Override
    public final RenderPipeline getWaterPipeline()
    {
        return Configurator.fancyFluids ? this.waterPipeline : this.defaultSinglePipeline;
    }
    
    @Override
    public final RenderPipeline getLavaPipeline()
    {
        return Configurator.fancyFluids ? this.lavaPipeline : this.defaultSinglePipeline;
    }

    @Override
    public IRenderPipeline getPipelineByIndex(int index)
    {
        return this.pipelines[index];
    }
    
    /**
     * The number of pipelines currently registered.
     */
    public final int pipelineCount()
    {
        return this.pipelineCount;
    }
    
    private void addStandardUniforms(RenderPipeline pipeline)
    {
        pipeline.uniform1f("u_time", UniformUpdateFrequency.PER_FRAME, u -> u.set(this.worldTime));
        
        pipeline.uniformSampler2d("u_textures", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.defaultTexUnit - GL13.GL_TEXTURE0));
        
        pipeline.uniformSampler2d("u_lightmap", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.lightmapTexUnit - GL13.GL_TEXTURE0));
        
        pipeline.uniform3f("u_eye_position", UniformUpdateFrequency.PER_FRAME, u -> 
        {
            Vec3d eyePos = Minecraft.getMinecraft().player.getPositionEyes(partialTicks);
            u.set((float)eyePos.x, (float)eyePos.y, (float)eyePos.z);
        });
        
        pipeline.uniform3f("u_fogAttributes", UniformUpdateFrequency.PER_TICK, u -> 
        {
            GlStateManager.FogState fogState = GlStateManager.fogState;
            u.set(fogState.end, fogState.end - fogState.start, 
                    // zero signals shader to use linear fog
                    fogState.mode == GlStateManager.FogMode.LINEAR.capabilityId ? 0f : fogState.density);
        });
        
        pipeline.uniform3f("u_fogColor", UniformUpdateFrequency.PER_TICK, u -> 
        {
            EntityRenderer er = Minecraft.getMinecraft().entityRenderer;
            u.set(er.fogColorRed, er.fogColorGreen, er.fogColorBlue);
        });
        
        pipeline.setupModelViewUniforms();
    }
            
    /**
     * Called at start of each frame but does not update pipelines immediately
     * because camera has not yet been set up and we need the projection matrix.
     * So, captures state it can and sets a flag that will used to update
     * pipelines before any chunks are rendered.   Our render list will call
     * us right before it render chunks.
     */
    public void onRenderTick(RenderTickEvent event)
    {
        MappedBufferStore.prepareEmpties();
        
        didUpdatePipelinesThisFrame = false;

        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;

        final float partialTicks = event.renderTickTime;
        this.partialTicks = partialTicks;
        if(entity.world != null)
            worldTime = Animation.getWorldTime(entity.world, partialTicks);
    }
    
    /**
     * Called by our chunk render list before each round of chunk renders.
     * Can be called multiple times per frame but we only update once per frame.
     * Necessary because Forge doesn't provide a hook that happens after camera setup
     * but before block rendering.<p>
     * 
     * Returns true if this was first pass so caller can handle 1x actions.
     */
    public boolean beforeRenderChunks()
    {
        if(didUpdatePipelinesThisFrame)
            return false;
        
        didUpdatePipelinesThisFrame = true;
        
        projectionMatrixBuffer.position(0);
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrixBuffer);
        OpenGlHelperExt.loadTransposeQuickly(projectionMatrixBuffer, projMatrix);
        
        for(int i = 0; i < this.pipelineCount; i++)
        {
            this.pipelines[i].onRenderTick();
        }
        
        return true;
    }

    public void onGameTick(ClientTickEvent event)
    {
        for(int i = 0; i < this.pipelineCount; i++)
        {
            pipelines[i].onGameTick();
        }
    }
    
    @Override
    public float worldTime()
    {
        return this.worldTime;
    }

    private static final float[] transferArray = new float[16];
    
    private static void loadTransferArray(Matrix4f m)
    {
        final float[] transferArray = PipelineManager.transferArray;
        
        transferArray[0] = m.m00;
        transferArray[1] = m.m01;
        transferArray[2] = m.m02;
        transferArray[3] = m.m03;
        transferArray[4] = m.m10;
        transferArray[5] = m.m11;
        transferArray[6] = m.m12;
        transferArray[7] = m.m13;
        transferArray[8] = m.m20;
        transferArray[9] = m.m21;
        transferArray[10] = m.m22;
        transferArray[11] = m.m23;
        transferArray[12] = m.m30;
        transferArray[13] = m.m31;
        transferArray[14] = m.m32;
        transferArray[15] = m.m33;
    }
    
    private static final void loadMVPMatrix(final Matrix4f mvMatrix)
    {
        final Matrix4f p = PipelineManager.projMatrix;
        
        transferArray[0] = mvMatrix.m00 * p.m00 + mvMatrix.m10 * p.m01 + mvMatrix.m20 * p.m02 + mvMatrix.m30 * p.m03;
        transferArray[1] = mvMatrix.m01 * p.m00 + mvMatrix.m11 * p.m01 + mvMatrix.m21 * p.m02 + mvMatrix.m31 * p.m03;
        transferArray[2] = mvMatrix.m02 * p.m00 + mvMatrix.m12 * p.m01 + mvMatrix.m22 * p.m02 + mvMatrix.m32 * p.m03;
        transferArray[3] = mvMatrix.m03 * p.m00 + mvMatrix.m13 * p.m01 + mvMatrix.m23 * p.m02 + mvMatrix.m33 * p.m03;
        
        transferArray[4] = mvMatrix.m00 * p.m10 + mvMatrix.m10 * p.m11 + mvMatrix.m20 * p.m12 + mvMatrix.m30 * p.m13;
        transferArray[5] = mvMatrix.m01 * p.m10 + mvMatrix.m11 * p.m11 + mvMatrix.m21 * p.m12 + mvMatrix.m31 * p.m13;
        transferArray[6] = mvMatrix.m02 * p.m10 + mvMatrix.m12 * p.m11 + mvMatrix.m22 * p.m12 + mvMatrix.m32 * p.m13;
        transferArray[7] = mvMatrix.m03 * p.m10 + mvMatrix.m13 * p.m11 + mvMatrix.m23 * p.m12 + mvMatrix.m33 * p.m13;
        
        transferArray[8] = mvMatrix.m00 * p.m20 + mvMatrix.m10 * p.m21 + mvMatrix.m20 * p.m22 + mvMatrix.m30 * p.m23;
        transferArray[9] = mvMatrix.m01 * p.m20 + mvMatrix.m11 * p.m21 + mvMatrix.m21 * p.m22 + mvMatrix.m31 * p.m23;
        transferArray[10] = mvMatrix.m02 * p.m20 + mvMatrix.m12 * p.m21 + mvMatrix.m22 * p.m22 + mvMatrix.m32 * p.m23;
        transferArray[11] = mvMatrix.m03 * p.m20 + mvMatrix.m13 * p.m21 + mvMatrix.m23 * p.m22 + mvMatrix.m33 * p.m23;
        
        transferArray[12] = mvMatrix.m00 * p.m30 + mvMatrix.m10 * p.m31 + mvMatrix.m20 * p.m32 + mvMatrix.m30 * p.m33;
        transferArray[13] = mvMatrix.m01 * p.m30 + mvMatrix.m11 * p.m31 + mvMatrix.m21 * p.m32 + mvMatrix.m31 * p.m33;
        transferArray[14] = mvMatrix.m02 * p.m30 + mvMatrix.m12 * p.m31 + mvMatrix.m22 * p.m32 + mvMatrix.m32 * p.m33;
        transferArray[15] = mvMatrix.m03 * p.m30 + mvMatrix.m13 * p.m31 + mvMatrix.m23 * p.m32 + mvMatrix.m33 * p.m33;
    }
    
    public static final void setModelViewMatrix(Matrix4f mvMatrix)
    {
        updateModelViewMatrix(mvMatrix);
        
        updateModelViewProjectionMatrix(mvMatrix);
        
        viewMatrixVersionCounter++;
    }
    
    private static final void updateModelViewMatrix(Matrix4f mvMatrix)
    {
        loadTransferArray(mvMatrix);
        
        // avoid NIO overhead
        OpenGlHelperExt.fastMatrix4fBufferCopy(transferArray, PipelineManager.modelViewMatrixBufferAddress);
    }
    
    private static final void updateModelViewProjectionMatrix(Matrix4f mvMatrix)
    {
        loadMVPMatrix(mvMatrix);
        
        // avoid NIO overhead
        OpenGlHelperExt.fastMatrix4fBufferCopy(transferArray, PipelineManager.modelViewProjectionMatrixBufferAddress);
    }
}
