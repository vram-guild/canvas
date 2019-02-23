package grondag.acuity.buffering;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import grondag.acuity.Acuity;
import grondag.acuity.opengl.OpenGlHelperExt;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.crash.CrashReport;

public class MappedBufferStore
{
    
    private static final int MIN_CAPACITY = 32;
    private static final int TARGET_BUFFERS = 512;
    
    private static final ArrayBlockingQueue<MappedBuffer> emptyMapped = new ArrayBlockingQueue<MappedBuffer>(TARGET_BUFFERS);
    private static final ArrayBlockingQueue<MappedBuffer> emptyUnmapped = new ArrayBlockingQueue<>(4096);
    
    /**
     * Buffers that may need defrag and thus need to be mapped.
     */
    private static final ArrayBlockingQueue<MappedBuffer> releaseRemapQueue = new ArrayBlockingQueue<>(4096);
    
    /**
     * Buffers that have been mapped and are awaiting defrag.
     */
    private static final ArrayBlockingQueue<MappedBuffer> releaseRebufferQueue = new ArrayBlockingQueue<MappedBuffer>(64);
    
    /**
     * Buffers that have been defragged and thus need to be unmapped and reset.
     * New buffers also need to be flushed and swapped for old.
     */
    private static final ArrayBlockingQueue<Pair<MappedBuffer, ObjectArrayList<Pair<DrawableChunkDelegate, MappedBufferDelegate>>>> releaseResetQueue = new ArrayBlockingQueue<>(4096);
    
    private static final Thread DEFRAG_THREAD;
    private static final Runnable DEFRAGGER = new Runnable()
    {
        @Override
        public void run()
        {
            while(true)
            {
                try
                {
                    MappedBuffer buff = releaseRebufferQueue.poll(27, TimeUnit.DAYS);
                    
                    if(buff.isDisposed())
                        continue;
                    
                    ObjectArrayList<Pair<DrawableChunkDelegate, MappedBufferDelegate>> swaps = buff.rebufferRetainers();
                    if(swaps != null)
                        releaseResetQueue.offer(Pair.of(buff, swaps));
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                catch (Exception e)
                {
                    Acuity.INSTANCE.getLog().error("Unexpected error detected during vertex buffer defrag.", e);;
                }
            }
        }
    };
    
    static
    {
        DEFRAG_THREAD = new Thread(DEFRAGGER, "Acuity Vertex Buffer Defrag Thread");
        DEFRAG_THREAD.setDaemon(true);
        DEFRAG_THREAD.start();
    }
    
    static @Nullable MappedBuffer getEmptyMapped()
    {
        try
        {
            return emptyMapped.poll(27, TimeUnit.DAYS);
        }
        catch (Exception e)
        {
            Minecraft.getMinecraft().crashed(new CrashReport("Unable to allocate empty GL buffer", e));
            return null;
        }
    }
    
    private static void processReleaseRemapQueue()
    {
        if(!releaseRemapQueue.isEmpty())
        {
            while(!releaseRemapQueue.isEmpty() && releaseRebufferQueue.size() < 64)
            {
                MappedBuffer buff = releaseRemapQueue.poll();
                
                if(buff.isDisposed())
                    continue;
                
                assert buff.isFinal();
                
                if(buff.retainers.isEmpty())
                {
                    if(buff.isMapped())
                        buff.discardFlushAndUnmap();
                    releaseResetQueue.offer(Pair.of(buff, null));
                }
                else
                {
                    if(buff.isFlushPending())
                        buff.flush();
                    buff.bind();
                    buff.map(false);
                    assert buff.isMapped();
                    releaseRebufferQueue.offer(buff);
                }
            }
            OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        }
    }
    
    private static void processReleaseResetQueue()
    {
        if(!releaseResetQueue.isEmpty())
        {
            Pair<MappedBuffer, ObjectArrayList<Pair<DrawableChunkDelegate, MappedBufferDelegate>>> pair = releaseResetQueue.poll();
            
            while(pair != null)
            {
                MappedBuffer buff = pair.getLeft();
                
                if(buff.isDisposed())
                    continue;

                assert !buff.isMapped() || buff.isMappedReadOnly();
                
                ObjectArrayList<Pair<DrawableChunkDelegate, MappedBufferDelegate>> list = pair.getRight();
                if(list != null && !list.isEmpty())
                {
                    final int limit = list.size();
                    for(int i = 0; i < limit; i++)
                    {
                        Pair<DrawableChunkDelegate, MappedBufferDelegate> swap = list.get(i);
                        MappedBufferDelegate bufferDelegate = swap.getRight();
                        bufferDelegate.flush();
                        swap.getLeft().replaceBufferDelegate(bufferDelegate);
                    }
                    buff.clearRetainers();
                }
                
                buff.reset();
                emptyUnmapped.offer(buff);
                
                pair = releaseResetQueue.poll();
            }
            OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        }
    }
    
    /**
     * Called at start of each render tick from client thread to  
     * maintain a pool of mapped buffers ready for off-thread loading.
     */
    public static void prepareEmpties()
    {
        assert Minecraft.getMinecraft().isCallingFromMinecraftThread();
        
        processReleaseRemapQueue();
       
        processReleaseResetQueue();
        
        final int targetBuffers = Math.max(MIN_CAPACITY, TARGET_BUFFERS - MappedBuffer.inUse.size());
        
        while(emptyMapped.size() < targetBuffers)
        {
            MappedBuffer empty =  emptyUnmapped.poll();

            if(empty == null)
                empty = new MappedBuffer();
            else
            {
                assert !empty.isDisposed();
                empty.bind();
                empty.map(true);
                empty.unbind();
            }
            emptyMapped.offer(empty);
        }
        
        //doStats();
    }
    
    //TODO: disable
    
    static int statCounter = 0;
    
//    private static void doStats()
//    {
//        if(statCounter++ == 24000)
//        {
//            statCounter = 0;
//            final int extantCount = MappedBuffer.inUse.size();
//            MappedBuffer.inUse.forEach(b -> b.reportStats());
//            Acuity.INSTANCE.getLog().info("Extant Mapped Buffers: " + extantCount);
//            Acuity.INSTANCE.getLog().info("Extant Mapped Capacity (MB): " + extantCount * MappedBuffer.CAPACITY_BYTES / 0x100000);
//            Acuity.INSTANCE.getLog().info("Ready Buffers: " + emptyMapped.size());
//            Acuity.INSTANCE.getLog().info("Idle Buffers: " + emptyUnmapped.size());
//            Acuity.INSTANCE.getLog().info("");
//        }
//    }
 
    /**
     * Called by mapped buffers when they are released off thread.
     * Prevents GL calls outside client thread.
     */
    public static void scheduleRelease(MappedBuffer mappedBuffer)
    {
        releaseRemapQueue.offer(mappedBuffer);
    }

    public static void forceReload()
    {
        emptyMapped.clear();
        AllocationManager.forceReload();
        emptyUnmapped.clear();
        releaseRebufferQueue.clear();
        releaseRemapQueue.clear();
        releaseResetQueue.clear();
        MappedBuffer.inUse.forEach(b -> b.dispose());
        MappedBuffer.inUse.clear();
        statCounter = 0;
    }
    
}
