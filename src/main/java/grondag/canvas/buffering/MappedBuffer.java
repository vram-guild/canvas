package grondag.canvas.buffering;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL15;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.Canvas;
import grondag.canvas.core.RenderPipeline;
import grondag.canvas.opengl.CanvasGlHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;

/**
 * Concurrency Notes<br>
 * Client thread must handle all operations that require GL context. This
 * include map, flush, bind, etc.
 * <p>
 * 
 * Allocation operations will occur on chunk build thread. An external
 * "check-in/out" mechanism will ensure only a single thread is ever calling
 * these operations.
 * <p>
 * 
 * Some allocation operations may interact with tracking data needed on the
 * client thread. For these, Atomic variable are preferred over synchronization.
 *
 */
public class MappedBuffer extends AbstractBuffer {
    public static final int CAPACITY_BYTES = 512 * 1024;
    private static final int HALF_CAPACITY = CAPACITY_BYTES / 2;

    public static final ObjectArrayList<MappedBuffer> inUse = new ObjectArrayList<>();

    /**
     * Max byte used for buffer flush. Incremented by allocation on chunk build
     * threads and decreased by flush on client threads.
     */
    private final AtomicInteger currentMaxOffset = new AtomicInteger();

    /**
     * Max byte flushed to GPU. Changed by client thread only.
     */
    private int lastFlushedOffset = 0;

    /**
     * Mapped GL buffer if currently mapped. Set by client thread only but access by
     * other threads is the point of having it.
     * <p>
     * 
     * MAY BE NON-NULL IF NOT MAPPED. Reuse of buffer instance is an optimization
     * feature of GL.
     */
    private ByteBuffer mapped = null;

    /**
     * True if buffer is actually mapped. If false, and {@link #mapped} is non-null,
     * the buffer instance is only meant for remap calls. Modified by client thread
     * only but may be read by buffering thread (assertions) at any time, thus
     * marked volatile.
     * 
     */
    private volatile boolean isMapped = false;

    /**
     * If true, buffer is fully allocated and does not need to be re-mapped after
     * flush. Changed by allocation thread - which will always be a single thread.
     */
    private volatile boolean isFinal = false;

    private boolean isMappedReadonly = false;

    /**
     * Bytes currently being used for render.
     */
    private final AtomicInteger retainedBytes = new AtomicInteger();

    /**
     * If true, release (or defrag) has been requested because buffer is final and
     * usage has dropped below 50%. *Probably* only updated on client thread but
     * handled as atomic just be sure.
     */
    private final AtomicBoolean isReleaseRequested = new AtomicBoolean(false);



    /**
     * Buffering threads hold a read lock on this while they are uploading. Client
     * thread will block during flush/remap until it can get a write lock.
     */
//    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Buffering threads hold a read lock on this while they are uploading. Client
     * thread will block during flush/remap until it can get a write lock.
     */
//    public final Lock bufferLock = lock.readLock();

    /**
     * Efficient access to write lock for client thread.
     */
//    private final Lock flushAndMapLock = lock.writeLock();

//    final ArrayList<String> traceLog = new ArrayList<>();

    MappedBuffer() {
        super();
        bind();
        orphan();
        map(true);
        unbind();
        inUse.add(this);
    }

    @Override
    public ByteBuffer byteBuffer() {
        assert mapped != null;
        return mapped;
    }

    void map(boolean writeFlag) {
//        traceLog.add("map(" + writeFlag + ")");
        if (isDisposed)
            return;

        // don't try to remap writeable buffer as read only
        if (!writeFlag && mapped != null)
            mapped = null;

        assert !isMapped;

        mapped = CanvasGlHelper.mapBufferAsynch(mapped, CAPACITY_BYTES, writeFlag);
        if (mapped == null) {
            int i = GlStateManager.getError();
            if (i != 0) {
                Canvas.INSTANCE.getLog().error("########## GL ERROR ON BUFFER REMAP ##########");
                Canvas.INSTANCE.getLog().error(GLX.getErrorString(i) + " (" + i + ")");
            }
            assert false;
        }
        isMapped = true;
        isMappedReadonly = !writeFlag;
    }

    public int unallocatedBytes() {
        return isFinal ? 0 : (CAPACITY_BYTES - currentMaxOffset.get());
    }

    /**
     * Won't break stride.
     */
    public MappedBufferDelegate requestBytes(int byteCount, int stride) {
//        traceLog.add(String.format("requestBytes(%d, %d)", byteCount, stride));
        assert !isDisposed;
        assert !isFinal;

        int oldOffset, newOffset, filled;
        while (true) {
            oldOffset = currentMaxOffset.get();

            filled = ((CAPACITY_BYTES - oldOffset) / stride) * stride;

            if (filled <= 0)
                return null;

            if (filled > byteCount)
                filled = byteCount;

            newOffset = oldOffset + filled;

            if (currentMaxOffset.compareAndSet(oldOffset, newOffset))
                return new MappedBufferDelegate(this, oldOffset, filled);
        }
    }

    /** assumes buffer is bound */
    private void orphan() {
//        traceLog.add("orphan()");
        assert MinecraftClient.getInstance().isMainThread();
        mapped = null;
        CanvasGlHelper.glBufferData(GLX.GL_ARRAY_BUFFER, CAPACITY_BYTES, GL15.GL_STATIC_DRAW);
        int i = GlStateManager.getError();
        if (i != 0) {
            Canvas.INSTANCE.getLog().error("########## GL ERROR ON BUFFER REMAP ##########");
            Canvas.INSTANCE.getLog().error(GLX.getErrorString(i) + " (" + i + ")");
        }
    }

    public boolean isFlushPending() {
        return isMapped && lastFlushedOffset != currentMaxOffset.get();
    }

    /**
     * Discards any pending flush and unmaps buffer.
     */
    public void discardFlushAndUnmap() {
//        traceLog.add("unmap()");

        assert !isDisposed;

        if (isDisposed)
            return;

        assert MinecraftClient.getInstance().isMainThread();

        assert isMapped;

        if (isMapped) {
            assert !isMappedReadonly;
            lastFlushedOffset = currentMaxOffset.get();
            isMapped = false;
            bind();
            CanvasGlHelper.unmapBuffer();
            unbind();
        }
    }

    /**
     * Called each tick to send updates to GPU.
     */
    @Override
    protected void flush() {
        if (isDisposed)
            return;

        assert MinecraftClient.getInstance().isMainThread();

        final int currentMax = currentMaxOffset.get();
        final int bytes = currentMax - lastFlushedOffset;

        if (bytes == 0)
            return;

        // TODO: remove this once fixed
        if (!isMapped) {
            Canvas.INSTANCE.getLog().warn("Skipped buffer flush due to unmapped buffer.");
            return;
        }

        assert isMapped;
        assert !isMappedReadonly;

//        traceLog.add("flush()");

        bind();

//        flushAndMapLock.lock();

        CanvasGlHelper.flushBuffer(lastFlushedOffset, bytes);
        CanvasGlHelper.unmapBuffer();
        lastFlushedOffset = currentMax;
        isMapped = false;

        if (!isFinal)
            map(true);

//        flushAndMapLock.unlock();

        unbind();
    }

    /**
     * called to leave buffer unmapped on next synch when will no longer be adding
     */
    public void setFinal() {
        this.isFinal = true;
    }

    /**
     * Called implicitly when bytes are allocated. Store calls explicitly to retain
     * while this buffer is being filled.
     */
    @Override
    public void retain(DrawableChunkDelegate drawable) {
        super.retain(drawable);
//        traceLog.add(String.format("retain(%s)", drawable.toString()));
        retainedBytes.addAndGet(drawable.bufferDelegate().byteCount());
    }

    @Override
    public void release(DrawableChunkDelegate drawable) {
        
        // UGLY: doesn't call super - fix up if decide to keep mapped
        
//        traceLog.add(String.format("release(%s)", drawable.toString()));
        // retainer won't be found if release was already scheduled and collection has
        // been cleared
        if (retainers.remove(drawable)) {
            final int bytes = drawable.bufferDelegate().byteCount();
            final int newRetained = retainedBytes.addAndGet(-bytes);
            if (newRetained < HALF_CAPACITY && isFinal && isReleaseRequested.compareAndSet(false, true)) {
                MappedBufferStore.scheduleRelease(this);
            }
        } else {
            // if not found, then should have been cleared after requesting release
            assert isReleaseRequested.get();
            assert retainers.isEmpty();
        }
    }

    public void reportStats() {
        String status = "" + (this.isFinal ? "FINAL" : "OPEN") + (this.isMapped ? "-MAPPED" : "")
                + (this.isReleaseRequested.get() ? "-RELEASING" : "");
        Canvas.INSTANCE.getLog()
                .info(String.format("Buffer %d: Count=%d  Bytes=%d (%d) Status=%s", this.id, this.retainers.size(),
                        this.retainedBytes.get(), this.retainedBytes.get() * 100 / CAPACITY_BYTES, status));
    }

    @Override
    protected void dispose() {
//        traceLog.add("dispose()");
        if (isMapped) {
            bind();
            CanvasGlHelper.unmapBuffer();
            unbind();
            isMapped = false;
            mapped = null;
        }
        super.dispose();
    }

    public void reset() {
//        traceLog.add("reset()");
        assert retainedBytes.get() == 0;
        assert retainers.isEmpty();

        bind();
        if (isMapped)
            CanvasGlHelper.unmapBuffer();

        isMapped = false;
        mapped = null;
        isMappedReadonly = false;
        orphan();
        isFinal = false;
        isReleaseRequested.set(false);
        currentMaxOffset.set(0);
        lastFlushedOffset = 0;
    }

    private static final ThreadLocal<int[]> transferArray = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[CAPACITY_BYTES / 4];
        }
    };

    public ObjectArrayList<Pair<DrawableChunkDelegate, AbstractBufferDelegate<?>>> rebufferRetainers() {
//        traceLog.add("rebufferRetainers()");
        if (isDisposed)
            return null;

        if (retainers.isEmpty())
            return null;

        assert isMapped;
        assert isMappedReadonly;
        assert mapped != null;

        final IntBuffer fromBuffer = mapped.asIntBuffer();
        final int[] transfer = transferArray.get();
        ObjectArrayList<Pair<DrawableChunkDelegate, AbstractBufferDelegate<?>>> swaps = new ObjectArrayList<>();

        final MappedAllocationManager allocator = (MappedAllocationManager) BufferManager.ALLOCATION_MANAGER;
        
        retainers.forEach(delegate -> {
            final RenderPipeline pipeline = delegate.getPipeline();
            final int fromByteCount = delegate.bufferDelegate().byteCount();
            final int fromIntCount = fromByteCount / 4;
            final int fromIntOffset = delegate.bufferDelegate().byteOffset() / 4;

            fromBuffer.position(fromIntOffset);
            fromBuffer.get(transfer, 0, fromIntCount);

            allocator.claimAllocation(pipeline, fromByteCount, ref -> {
                final int byteOffset = ref.byteOffset();
                final int byteCount = ref.byteCount();
                final int intLength = byteCount / 4;
                // no splitting, need 1:1
                assert byteCount == fromByteCount;

                ref.lockForUpload();
                final IntBuffer intBuffer = ref.intBuffer();
                intBuffer.position(byteOffset / 4);
                intBuffer.put(transfer, 0, intLength);
                ref.unlockForUpload();

                ref.retain(delegate);
                swaps.add(Pair.of(delegate, ref));
            });
        });
        return swaps;
    }

    /**
     * Called after rebuffered retainers are flushed and swapped with ours.
     */
    void clearRetainers() {
//        traceLog.add("clearRetainers()");
        this.retainedBytes.set(0);
        this.retainers.clear();
    }

    public boolean isFinal() {
        return this.isFinal;
    }

    public boolean isMapped() {
        return this.isMapped;
    }

    public boolean isMappedReadOnly() {
        return this.isMappedReadonly;
    }
}
