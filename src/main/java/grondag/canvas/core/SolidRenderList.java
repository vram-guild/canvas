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

package grondag.canvas.core;

import java.util.ArrayDeque;
import java.util.function.Consumer;

import grondag.canvas.RenderConditionImpl;
import grondag.canvas.buffer.DrawableDelegate;
import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Accumulates and renders delegates in pipeline, buffer order.<p>
 * 
 * Note there is no translucent version of this, because translucent
 * must always be rendered in quad-sort order and thus we don't accumulate
 * multiple chunks or models into a single collection.
 */
public class SolidRenderList implements Consumer<ObjectArrayList<DrawableDelegate>> {
    private static final ArrayDeque<SolidRenderList> POOL = new ArrayDeque<>();
    
    public static SolidRenderList claim() {
        SolidRenderList result = POOL.poll();
        if (result == null)
            result = new SolidRenderList();
        return result;
    }
    
    private final ObjectArrayList<DrawableDelegate>[] pipelineLists;

    private SolidRenderList() {
        final int size = PipelineManager.INSTANCE.pipelineCount() * ConditionalPipeline.MAX_CONDITIONAL_PIPELINES;
        // PERF: probably need something more compact now with conditional pipelines
        @SuppressWarnings("unchecked")
        ObjectArrayList<DrawableDelegate>[] buffers = new ObjectArrayList[size];
        for (int i = 0; i < size; i++) {
            buffers[i] = new ObjectArrayList<DrawableDelegate>();
        }
        this.pipelineLists = buffers;
    }

    public void draw() {
        for (ObjectArrayList<DrawableDelegate> list : this.pipelineLists) {
            renderListInBufferOrder(list);
        }
    }
    
    @SuppressWarnings("serial")
    private static class BufferSorter extends AbstractIntComparator implements Swapper {
        Object[] delegates;

        @Override
        public int compare(int a, int b) {
            return Integer.compare(((DrawableDelegate) delegates[a]).bufferId(),
                    ((DrawableDelegate) delegates[b]).bufferId());
        }

        @Override
        public void swap(int a, int b) {
            Object swap = delegates[a];
            delegates[a] = delegates[b];
            delegates[b] = swap;
        }
    };

    private static final ThreadLocal<BufferSorter> SORTERS = ThreadLocal.withInitial(BufferSorter::new);

    /**
     * Renders delegates in buffer order to minimize bind calls. 
     * Assumes all delegates in the list share the same pipeline.
     */
    private void renderListInBufferOrder(ObjectArrayList<DrawableDelegate> list) {
        final int limit = list.size();

        if (limit == 0)
            return;

        final Object[] delegates = list.elements();

        final BufferSorter sorter = SORTERS.get();
        sorter.delegates = delegates;
        Arrays.quickSort(0, limit, sorter, sorter);

        ((DrawableDelegate) delegates[0]).getPipeline().pipeline.activate(true);

        int lastBufferId = -1;
        final int frameIndex = PipelineManager.INSTANCE.frameIndex();

        for (int i = 0; i < limit; i++) {
            final DrawableDelegate b = (DrawableDelegate) delegates[i];
            final RenderConditionImpl condition = b.getPipeline().condition;
            if(!condition.affectBlocks || condition.compute(frameIndex)) {
                lastBufferId = b.bind(lastBufferId);
                b.draw();
            }
            //PERF: release delegates
        }
        list.clear();
    }
    
    public void release() {
        POOL.offer(this);
    }
    
    public void drawAndRelease() {
        draw();
        release();
    }
    
    @Override
    public void accept(ObjectArrayList<DrawableDelegate> delegates) {
        final int limit = delegates.size();
        for (int i = 0; i < limit; i++) {
            DrawableDelegate d =  delegates.get(i);
            pipelineLists[d.getPipeline().index].add(d);
        }
    }
}
