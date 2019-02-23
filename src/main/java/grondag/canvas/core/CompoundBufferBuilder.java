package grondag.acuity.core;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import grondag.acuity.Acuity;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.buffering.DrawableChunk;
import grondag.acuity.buffering.UploadableChunk;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundBufferBuilder extends BufferBuilder
{
    
    /**
     * Left is solid, right is translucent.
     */
    static final ThreadLocal<Pair<VertexCollectorList, VertexCollectorList>> collectors = new ThreadLocal<Pair<VertexCollectorList, VertexCollectorList>>()
    {
        @Override
        protected Pair<VertexCollectorList, VertexCollectorList> initialValue()
        {
            return Pair.of(new VertexCollectorList(), new VertexCollectorList());
        }
    };
    
    /**
     * Holds vertex data and packing data for next upload if we have it.
     * Buffer is obtained from BufferStore and will be released back to store by upload.
     */
    private AtomicReference<UploadableChunk<?>>  uploadState  = new AtomicReference<>();
    
    
    /**
     * Tells us which block layer we are buffering.
     * Can be used with {@link #owner} to find peer buffers for other layers.
     * Could also be handy for other purposes.
     */
    private @Nullable BlockRenderLayer layer;

    private @Nullable CompoundBufferBuilder proxy;
    
    private class CompoundState extends State
    {
        private int[][] collectorState;
        
        public CompoundState(int[] buffer, VertexFormat format, int[][] collectorState)
        {
            super(buffer, format);
            this.collectorState = collectorState;
        }
    }
    
    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(limitBufferSize(bufferSizeIn));
    }
    
    /**
     * Called at end of RegionRenderCacheBuilder init via ASM.
     */
    public void setupLinks(RegionRenderCacheBuilder owner, BlockRenderLayer layer)
    {
        this.layer = layer;
        
        if(this.layer == BlockRenderLayer.CUTOUT || this.layer == BlockRenderLayer.CUTOUT_MIPPED)
        {
            this.proxy = (CompoundBufferBuilder) owner.getWorldRendererByLayer(BlockRenderLayer.SOLID);
        }
    }
    
    /**
     * The RegionRenderCacheBuilder instantiates this with significant sizes
     * but in most cases the super instance won't be used when the mod is enabled
     * so don't honor these when mod is enabled to reduce memory footprint.
     */
    static final int limitBufferSize(int bufferSizeIn)
    {
        if(Acuity.isModEnabled())
        {
            if(bufferSizeIn == 2097152 || bufferSizeIn == 131072 || bufferSizeIn == 262144)
            {
                return 2048;
            }
        }
        return bufferSizeIn;
    }
    
    private @Nullable CompoundState loadedState;
    
    /**
     * Used to retrieve and save collector state for later resorting of translucency.<p>
     * 
     * Temporarily means we have a reference to state in two places but reference in 
     * this instance will be removed during {@link #finishDrawingIfNotAlreadyFinished()}.
     */
    @Override
    public State getVertexState()
    {
        if(Acuity.isModEnabled())
        {
            assert this.proxy == null;
            assert this.layer == BlockRenderLayer.TRANSLUCENT;
            
            State inner = super.getVertexState();
            CompoundState result = loadedState;
            if(result == null)
            {
                result = new CompoundState(inner.getRawBuffer(), inner.getVertexFormat(), 
                    collectors.get().getRight().getCollectorState(null));
            }
            else
            {
                result.collectorState = collectors.get().getRight().getCollectorState(result.collectorState);
                loadedState = null;
            }
            return result;
        }
        else
            return super.getVertexState();
    }

    @SuppressWarnings("null")
    @Override
    public void setVertexState(State state)
    {
        super.setVertexState(state);
        if(Acuity.isModEnabled())
        {
            assert this.proxy == null;
            assert this.layer == BlockRenderLayer.TRANSLUCENT;
            assert loadedState == null;
            loadedState = (CompoundState)state;
            collectors.get().getRight().loadCollectorState(loadedState.collectorState);
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        if(Acuity.isModEnabled())
        {
            assert this.layer == BlockRenderLayer.SOLID || this.layer == BlockRenderLayer.TRANSLUCENT;
            
            if(this.layer == BlockRenderLayer.SOLID)
                collectors.get().getLeft().clear();
            else
                collectors.get().getRight().clear();
        }
    }
    
    public VertexCollector getVertexCollector(RenderPipeline pipeline)
    {
        if(Acuity.isModEnabled() && this.proxy != null)
            return this.proxy.getVertexCollector(pipeline);
        
        return this.layer == BlockRenderLayer.SOLID
                ? collectors.get().getLeft().get(pipeline)
                : collectors.get().getRight().get(pipeline);
    }
    
    public void beginIfNotAlreadyDrawing(int glMode, VertexFormat format)
    {
        if(!this.isDrawing)
        {
            assert this.layer == BlockRenderLayer.SOLID || this.layer == BlockRenderLayer.TRANSLUCENT || !Acuity.isModEnabled();
            
            // NB: this calls reset which initializes collector list
            super.begin(glMode, format);
        }
    }
    
    @Override
    public void begin(int glMode, VertexFormat format)
    {
        if(Acuity.isModEnabled() && proxy != null)
            proxy.beginIfNotAlreadyDrawing(glMode, format);
        else
            beginIfNotAlreadyDrawing(glMode, format);
    }
    
    @SuppressWarnings("null")
    public void finishDrawingIfNotAlreadyFinished()
    {
        if(this.isDrawing)
        {
            super.finishDrawing();
            
            if(Acuity.isModEnabled())
            {
                switch(this.layer)
                {
                    case SOLID:
                    {
                        UploadableChunk<?> abandoned = this.uploadState.getAndSet(collectors.get().getLeft().packUploadSolid());
                        if(abandoned != null)
                            abandoned.cancel();
                        return;
                    }
                    
                    case TRANSLUCENT:
                    {
                        UploadableChunk<?> abandoned = this.uploadState.getAndSet(collectors.get().getRight().packUploadTranslucent());
                        if(abandoned != null)
                            abandoned.cancel();
                        return;
                    }
                    
                    case CUTOUT:
                    case CUTOUT_MIPPED:
                    default:
                        assert false : "Bad render layer in compound buffer builder finish";
                        break;
                
                }
            }
        }
    }
    
    @Override
    public void finishDrawing()
    {
        if(Acuity.isModEnabled() && proxy != null)
        {
            proxy.finishDrawingIfNotAlreadyFinished();
            return;
        }
        else
            this.finishDrawingIfNotAlreadyFinished();
       
    }

    /**
     * Must be called on thread - handles any portion of GL buffering that must be done in context.
     */
    public @Nullable DrawableChunk produceDrawable()
    {   
        assert this.layer == BlockRenderLayer.SOLID || this.layer == BlockRenderLayer.TRANSLUCENT;
        
        UploadableChunk<?> uploadBuffer = this.uploadState.getAndSet(null);
        if(uploadBuffer == null)
        {
//            System.out.println(Integer.toHexString(CompoundBufferBuilder.this.hashCode()) + " Ignoring upload request due to missing upload state in Compound Vertex Buffer (" + layer.toString() + ") - must have been loaded earlier");
            return null;
        }

        return uploadBuffer.produceDrawable();
    }
    
//    public static final ConcurrentHashMap<BlockPos, Long> SORTS = new ConcurrentHashMap<>();
//    private @Nullable BlockPos chunkOriginPos;
    
//    @Override
//    public void setTranslation(double x, double y, double z)
//    {
//        super.setTranslation(x, y, z);
//        chunkOriginPos = new BlockPos((MathHelper.fastFloor(-x) >> 4) << 4, (MathHelper.fastFloor(-y) >> 4) << 4, (MathHelper.fastFloor(-z) >> 4) << 4);
//    }
    
    @Override
    public void sortVertexData(float x, float y, float z)
    {
//        SORTS.put(chunkOriginPos, System.nanoTime());
        
        if(Acuity.isModEnabled())
            // save sort perspective coordinate for use during packing.  Actual sort occurs then.
            collectors.get().getRight().setViewCoordinates(x, y, z);
        else
            super.sortVertexData(x, y, z);
    }
}
