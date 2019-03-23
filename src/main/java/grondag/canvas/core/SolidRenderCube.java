package grondag.canvas.core;

import java.util.function.Consumer;

import grondag.canvas.buffering.DrawableDelegate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SolidRenderCube implements Consumer<DrawableDelegate> {
    public final ObjectArrayList<DrawableDelegate>[] pipelineLists;

    public SolidRenderCube() {
        final int size = PipelineManager.INSTANCE.pipelineCount();
        @SuppressWarnings("unchecked")
        ObjectArrayList<DrawableDelegate>[] buffers = new ObjectArrayList[size];
        for (int i = 0; i < size; i++) {
            buffers[i] = new ObjectArrayList<DrawableDelegate>();
        }
        this.pipelineLists = buffers;
    }

    @Override
    public void accept(DrawableDelegate d) {
        pipelineLists[d.getPipeline().getIndex()].add(d);
    }
}
