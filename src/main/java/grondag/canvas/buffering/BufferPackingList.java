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

package grondag.canvas.buffering;

import java.util.function.Consumer;

import grondag.canvas.core.ConditionalPipeline;

/**
 * Tracks number of vertices, pipeline and sequence thereof within a buffer.
 */
public class BufferPackingList {
    private int[] starts = new int[16];
    private int[] counts = new int[16];
    private ConditionalPipeline[] pipelines = new ConditionalPipeline[16];
    
    private int size = 0;
    private int totalBytes = 0;
    
    BufferPackingList() {
        
    }
    
    public void clear() {
        this.size = 0;
        this.totalBytes = 0;
    }

    public int size() {
        return this.size;
    }

    /**
     * For performance testing.
     */
    public int quadCount() {
        if (this.size == 0)
            return 0;

        int quads = 0;

        for (int i = 0; i < this.size; i++) {
            quads += this.counts[i] / 4;
        }
        return quads;
    }

    public int totalBytes() {
        return this.totalBytes;
    }

    public void addPacking(ConditionalPipeline conditionalPipeline, int startVertex, int vertexCount) {
        if (size == this.pipelines.length) {
            final int cCopy[] = new int[size * 2];
            System.arraycopy(this.counts, 0, cCopy, 0, size);
            this.counts = cCopy;

            final int sCopy[] = new int[size * 2];
            System.arraycopy(this.starts, 0, sCopy, 0, size);
            this.starts = sCopy;
            
            final ConditionalPipeline pCopy[] = new ConditionalPipeline[size * 2];
            System.arraycopy(this.pipelines, 0, pCopy, 0, size);
            this.pipelines = pCopy;
        }
        this.pipelines[size] = conditionalPipeline;
        this.starts[size] = startVertex;
        this.counts[size] = vertexCount;
        this.totalBytes += conditionalPipeline.pipeline.piplineVertexFormat().vertexStrideBytes * vertexCount;
        this.size++;
    }

    public final void forEach(BufferPacker consumer) {
        final int size = this.size;
        for (int i = 0; i < size; i++) {
            consumer.accept(this.pipelines[i], this.starts[i], this.counts[i]);
        }
    }

    public final void forEachPipeline(Consumer<ConditionalPipeline> consumer) {
        final int size = this.size;
        for (int i = 0; i < size; i++)
            consumer.accept(this.pipelines[i]);
    }

    public final ConditionalPipeline getPipeline(int index) {
        return this.pipelines[index];
    }

    public final int getCount(int index) {
        return this.counts[index];
    }
    
    public final int getStart(int index) {
        return this.starts[index];
    }
}
