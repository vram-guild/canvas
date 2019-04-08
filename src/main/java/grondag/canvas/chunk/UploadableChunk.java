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

package grondag.canvas.chunk;

import grondag.canvas.buffer.BufferPacker;
import grondag.canvas.buffer.BufferPackingList;
import grondag.canvas.buffer.DrawableDelegate;
import grondag.canvas.buffer.VertexCollectorList;
import grondag.canvas.buffer.allocation.VboBufferManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class UploadableChunk<V extends DrawableChunk> {
    protected final ObjectArrayList<DrawableDelegate> delegates;

    /** Does not retain packing list reference */
    protected UploadableChunk(BufferPackingList packingList, VertexCollectorList collectorList) {
        delegates = BufferPacker.pack(packingList, collectorList, VboBufferManager.ALLOCATION_MANAGER.getAllocator(packingList.totalBytes()));
    }

    /**
     * Will be called from client thread - is where flush/unmap needs to happen.
     */
    public abstract V produceDrawable();

    /**
     * Called if {@link #produceDrawable()} will not be called, so can release
     * MappedBuffer(s).
     */
    public final void cancel() {
        final int limit = delegates.size();
        for (int i = 0; i < limit; i++)
            delegates.get(i).release();

        delegates.clear();
    }

    public static class Solid extends UploadableChunk<DrawableChunk.Solid> {
        public Solid(BufferPackingList packing, VertexCollectorList collectorList) {
            super(packing, collectorList);
        }

        @Override
        public DrawableChunk.Solid produceDrawable() {
            final int limit = delegates.size();
            for (int i = 0; i < limit; i++)
                delegates.get(i).flush();
            return new DrawableChunk.Solid(delegates);
        }
    }

    public static class Translucent extends UploadableChunk<DrawableChunk.Translucent> {
        public Translucent(BufferPackingList packing, VertexCollectorList collectorList) {
            super(packing, collectorList);
        }

        @Override
        public DrawableChunk.Translucent produceDrawable() {
            final int limit = delegates.size();
            for (int i = 0; i < limit; i++) {
                delegates.get(i).flush();
            }
            return new DrawableChunk.Translucent(delegates);
        }
    }
}
