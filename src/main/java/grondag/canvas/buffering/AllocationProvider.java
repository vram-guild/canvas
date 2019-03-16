package grondag.canvas.buffering;

import java.util.function.Consumer;

import grondag.canvas.core.RenderPipelineImpl;

@FunctionalInterface
public interface AllocationProvider {
    /**
     * If byteCount is larger than a single buffer will give consumer more than one
     * buffer w/ offsets able to contain the given byte count. Otherwise will always
     * call consumer 1X with an allocation that contains the entire byte count. If
     * more than one buffer is needed, break(s) will be at a boundary compatible
     * with all vertex formats. All vertices in the buffer(s) will share the same
     * pipeline (and thus vertex format).
     */
    void claimAllocation(RenderPipelineImpl pipeline, int byteCount, Consumer<AbstractBufferDelegate<?>> consumer);
}
