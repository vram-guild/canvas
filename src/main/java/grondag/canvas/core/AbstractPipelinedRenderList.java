package grondag.canvas.core;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.boson.org.joml.Matrix4f;
import grondag.canvas.Canvas;
import grondag.canvas.RendererImpl;
import grondag.canvas.api.CanvasListener;
import grondag.canvas.buffering.DrawableChunk;
import grondag.canvas.buffering.DrawableChunkDelegate;
import grondag.canvas.hooks.IRenderChunk;
import grondag.canvas.opengl.CanvasGlHelper;
import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;

public class AbstractPipelinedRenderList implements CanvasListener
{
    public boolean isAcuityEnabled = Canvas.isModEnabled();
    
    protected final ObjectArrayList<ChunkRenderer> chunks = new ObjectArrayList<ChunkRenderer>();
    
    /**
     * Each entry is for a single 256^3 render cube.<br>
     * The entry is an array of CompoundVertexBuffers.<br>
     * The array is as long as the highest pipeline index.<br> 
     * Null values mean that pipeline isn't part of the render cube.<br>
     * Non-null values are lists of buffer in that cube with the given pipeline.<br>
     */
    private final Long2ObjectOpenHashMap<SolidRenderCube> solidCubes = new Long2ObjectOpenHashMap<>();
    
    /**
     * Cache and reuse cube data stuctures.
     */
    private final ArrayDeque<SolidRenderCube> cubeStore = new ArrayDeque<>();
    
    /**
     * Will hold the modelViewMatrix that was in GL context before first call to block render layer this pass.
     */
    protected final Matrix4f mvMatrix = new Matrix4f();
    protected final Matrix4f mvPos = new Matrix4f();
    protected final Matrix4f xlatMatrix = new Matrix4f();
    
    protected final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    private int originX = Integer.MIN_VALUE;
    private int originY = Integer.MIN_VALUE;
    private int originZ = Integer.MIN_VALUE;
  
    private double viewEntityX;
    private double viewEntityY;
    private double viewEntityZ;
    
    public AbstractPipelinedRenderList()
    {
        xlatMatrix.identity();
        RendererImpl.INSTANCE.registerListener(this);
    }

    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn)
    {
        this.viewEntityX = viewEntityXIn;
        this.viewEntityY = viewEntityYIn;
        this.viewEntityZ = viewEntityZIn;
    }
    
    public void addChunkRenderer(ChunkRenderer renderChunkIn, BlockRenderLayer layer)
    {
        if(layer == BlockRenderLayer.TRANSLUCENT)
            this.chunks.add(renderChunkIn);
        else
            this.addSolidChunk(renderChunkIn);
    }
    
    private SolidRenderCube getSolidRenderCube()
    {
        SolidRenderCube result = cubeStore.poll();
        if(result == null)
            result = new SolidRenderCube();
        return result;
    }
    
    private void addSolidChunk(ChunkRenderer renderChunkIn)
    {
        final long cubeKey = RenderCube.getPackedOrigin(renderChunkIn.getOrigin());
        
        SolidRenderCube buffers = solidCubes.get(cubeKey);
        if(buffers == null)
        {
            buffers = getSolidRenderCube();
            solidCubes.put(cubeKey, buffers);
        }
        addSolidChunkToBufferArray(renderChunkIn, buffers);
    }
    
    private void addSolidChunkToBufferArray(ChunkRenderer renderChunkIn, SolidRenderCube buffers)
    {
        final DrawableChunk.Solid vertexbuffer = ((IRenderChunk)renderChunkIn).getSolidDrawable();
        if(vertexbuffer != null)
            vertexbuffer.prepareSolidRender(buffers);
    }
    
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if(layer == BlockRenderLayer.SOLID)
            renderChunkLayerSolid();
        else
            renderChunkLayerTranslucent();
    }
    
    private final void updateViewMatrix(long packedRenderCubeKey)
    {
        updateViewMatrix(
                RenderCube.getPackedKeyOriginX(packedRenderCubeKey),
                RenderCube.getPackedKeyOriginY(packedRenderCubeKey),
                RenderCube.getPackedKeyOriginZ(packedRenderCubeKey));
    }
    
    private final void updateViewMatrix(BlockPos renderChunkOrigin)
    {
        updateViewMatrix(
                RenderCube.renderCubeOrigin(renderChunkOrigin.getX()),
                RenderCube.renderCubeOrigin(renderChunkOrigin.getY()),
                RenderCube.renderCubeOrigin(renderChunkOrigin.getZ()));
    }
    
    private final void updateViewMatrix(final int ox, final int oy, final int oz)
    {
        if(ox == originX && oz == originZ && oy == originY)
            return;

        originX = ox;
        originY = oy;
        originZ = oz;
        updateViewMatrixInner(ox, oy, oz);
    }
    
    private final void updateViewMatrixInner(final int ox, final int oy, final int oz)
    {
        final Matrix4f mvPos = this.mvPos;
        
        // note row-major order in the matrix library we are using
        xlatMatrix._m03((float)(ox - viewEntityX));
        xlatMatrix._m13((float)(oy - viewEntityY));
        xlatMatrix._m23((float)(oz - viewEntityZ));

        xlatMatrix.mul(mvMatrix, mvPos);
        
        // vanilla applies a per-chunk scaling matrix, but does not seem to be essential - probably a hack to prevent seams/holes due to FP error
        // If really is necessary, would want to handle some other way.  Per-chunk matrix not initialized when Acuity enabled.
//        Matrix4f.mul(mvChunk, mvPos, mvPos);

        PipelineManager.setModelViewMatrix(mvPos);
    }
    
    private final void preRenderSetup()
    {
        originX = Integer.MIN_VALUE;
        originZ = Integer.MIN_VALUE;
        
        // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
        // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.
        // We don't use these except for GL_VERTEX so disable them now unless we
        // are using VAOs, which will cause them to be ignored.
        // Not a problem to disable them because MC disables the when we return.
        if(!CanvasGlHelper.isVaoEnabled())
            disableUnusedAttributes();
    }
    
    private final void disableUnusedAttributes()
    {
        GlStateManager.disableClientState(GL11.GL_COLOR_ARRAY);
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        GlStateManager.disableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.activeTexture(GLX.GL_TEXTURE1);
        GlStateManager.disableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    private final void downloadModelViewMatrix()
    {
        final FloatBuffer modelViewMatrixBuffer = this.modelViewMatrixBuffer;
        modelViewMatrixBuffer.position(0);
        GlStateManager.getMatrix(GL11.GL_MODELVIEW_MATRIX, modelViewMatrixBuffer);
        mvMatrix.set(modelViewMatrixBuffer);
    }
    
    protected final void renderChunkLayerSolid()
    {
        // UGLY: add hook for this
        // Forge didn't give us a hook in the render loop that comes
        // after camera transform is set up - so call out event handler
        // here as a workaround. Our event handler will only act 1x/frame.
        // Do this even if solid is empty, in case translucent layer needs it.
        if(PipelineManager.INSTANCE.beforeRenderChunks())
            downloadModelViewMatrix();
        
        if (this.solidCubes.isEmpty()) 
            return; 
        
        preRenderSetup();
        
        ObjectIterator<Entry<SolidRenderCube>> it = solidCubes.long2ObjectEntrySet().fastIterator();
        while(it.hasNext())
        {
            Entry<SolidRenderCube> e = it.next();
            updateViewMatrix(e.getLongKey());
            renderSolidArray(e.getValue());
        }
        
        solidCubes.clear();
        postRenderCleanup();
    }
    
    private void renderSolidArray(SolidRenderCube rendercube)
    {
        for(ObjectArrayList<DrawableChunkDelegate> list : rendercube.pipelineLists)
            renderSolidList(list);
        cubeStore.offer(rendercube);
    }
    
    @SuppressWarnings("serial")
    private static class SortThingy extends AbstractIntComparator implements Swapper
    {
        Object[] delegates;
        
        @Override
        public int compare(int a, int b)
        {
            return Integer.compare(((DrawableChunkDelegate)delegates[a]).bufferId(), ((DrawableChunkDelegate)delegates[b]).bufferId());
        }
        
        @Override
        public void swap(int a, int b)
        {
            Object swap = delegates[a];
            delegates[a] = delegates[b];
            delegates[b] = swap;
        }
    };
    
    private static final SortThingy SORT_THINGY = new SortThingy();
    
    /**
     * Renders solid chunks in vertex buffer order to minimize bind calls.
     * Assumes all chunks in the list share the same pipeline.
     */
    private void renderSolidList(ObjectArrayList<DrawableChunkDelegate> list)
    {
        final int limit = list.size();
        
        if(limit == 0)
            return;
        
        Object[] delegates = list.elements();
        
        SORT_THINGY.delegates = delegates;
        Arrays.quickSort(0, limit, SORT_THINGY, SORT_THINGY);
        
        ((DrawableChunkDelegate)delegates[0]).getPipeline().activate(true);

        int lastBufferId = -1;
        
        // using conventional loop here to prevent iterator garbage in hot loop
        // profiling shows it matters
        for(int i = 0; i < limit; i++)
        {
            final DrawableChunkDelegate b = (DrawableChunkDelegate)delegates[i];
            lastBufferId = b.bind(lastBufferId);
            b.draw();
        }
        list.clear();
    }
    
    protected final void renderChunkLayerTranslucent()
    {
        final ObjectArrayList<ChunkRenderer> chunks = this.chunks;
        final int chunkCount = chunks.size();

        if (chunkCount == 0) 
            return;  
        
        preRenderSetup();
        
        for (int i = 0; i < chunkCount; i++)
        {
            final ChunkRenderer renderchunk =  chunks.get(i);
            final DrawableChunk.Translucent drawable = ((IRenderChunk)renderchunk).getTranslucentDrawable();
            if(drawable == null)
                continue;
            updateViewMatrix(renderchunk.getOrigin());
            drawable.renderChunkTranslucent();
        }

        chunks.clear();
        postRenderCleanup();
    }
    
    private final void postRenderCleanup()
    {
        if(CanvasGlHelper.isVaoEnabled())
            GL30.glBindVertexArray(0);
        
        CanvasGlHelper.resetAttributes();
        GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, 0);
        Program.deactivate();
        GlStateManager.clearCurrentColor();
    }

    @Override
    public final void onStatusChange(boolean newEnabledStatus)
    {
        this.isAcuityEnabled = newEnabledStatus;
    }
}
