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

package grondag.canvas.buffer.packing;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.chunk.UploadableChunk;
import grondag.canvas.material.MaterialState;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.MathHelper;

public class VertexCollectorList {
    private static final Comparator<VertexCollector> translucentComparator = new Comparator<VertexCollector>() {
        @Override
        public int compare(VertexCollector o1, VertexCollector o2) {
            // note reverse order - take most distant first
            return Double.compare(o2.firstUnpackedDistance(), o1.firstUnpackedDistance());
        }
    };
    
    private static final Comparator<VertexCollector> solidComparator = new Comparator<VertexCollector>() {
        @Override
        public int compare(VertexCollector o1, VertexCollector o2) {
            return Integer.compare(o1.pipeline().index, o2.pipeline().index);
        }
    };

    private static final ThreadLocal<PriorityQueue<VertexCollector>> sorters = new ThreadLocal<PriorityQueue<VertexCollector>>() {
        @Override
        protected PriorityQueue<VertexCollector> initialValue() {
            return new PriorityQueue<VertexCollector>(translucentComparator);
        }

        @Override
        public PriorityQueue<VertexCollector> get() {
            PriorityQueue<VertexCollector> result = super.get();
            result.clear();
            return result;
        }
    };

    /**
     * Fast lookup of buffers by pipeline index. Null in CUTOUT layer buffers.
     */
    private VertexCollector[] vertexCollectors = new VertexCollector[MaterialState.MAX_MATERIAL_STATES];

    private final BufferPackingList packingList = new BufferPackingList();
    
    private final ObjectArrayList<VertexCollector> emptyCollectors = new ObjectArrayList<>();
    private final ObjectArrayList<VertexCollector> usedCollectors = new ObjectArrayList<>();
    
    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewX;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewY;
    /** used in transparency layer sorting - updated with player eye coordinates */
    private double viewZ;

    /** used in transparency layer sorting - updated with origin of render cube */
    double renderOriginX = 0;
    /** used in transparency layer sorting - updated with origin of render cube */
    double renderOriginY = 0;
    /** used in transparency layer sorting - updated with origin of render cube */
    double renderOriginZ = 0;

    /**
     * Releases any held vertex collectors and resets state
     */
    public void clear() {
        renderOriginX = 0;
        renderOriginY = 0;
        renderOriginZ = 0;

        while(!usedCollectors.isEmpty()) {
            VertexCollector vc = usedCollectors.pop();
            assert vc == vertexCollectors[vc.pipeline().index] : "Mismatch between VCL used list and lookup array";
            vertexCollectors[vc.pipeline().index] = null;
            vc.clear();
            emptyCollectors.push(vc);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        clear();
    }

    public final boolean isEmpty() {
        return usedCollectors.isEmpty();
    }

    /**
     * Saves player eye coordinates for vertex sorting. Normally these will be the
     * same values for every list but save in instance to stay consistent with
     * Vanilla and possibly to support mods that do strange things with view entity
     * perspective. (PortalGun?)
     */
    public void setViewCoordinates(double x, double y, double z) {
        viewX = x;
        viewY = y;
        viewZ = z;
    }

    /**
     * Called when a render chunk initializes buffer builders with offset.
     */
    public void setRelativeRenderOrigin(double x, double y, double z) {
        renderOriginX = RenderCube.renderCubeOrigin(MathHelper.fastFloor(x));
        renderOriginY = RenderCube.renderCubeOrigin(MathHelper.fastFloor(y));
        renderOriginZ = RenderCube.renderCubeOrigin(MathHelper.fastFloor(z));
    }
    
    public void setAbsoluteRenderOrigin(double x, double y, double z) {
        renderOriginX = x;
        renderOriginY = y;
        renderOriginZ = z;
    }

    public final VertexCollector get(RenderMaterialImpl.Value material) {
        return get(MaterialState.get(material.shader, material.condition));
    }
    
    public final VertexCollector get(MaterialState renderState) {
        final int renderIndex = renderState.index;
        VertexCollector result = vertexCollectors[renderIndex];
        if(result == null) {
            result = emptyCollector().prepare(renderState);
            vertexCollectors[renderIndex] = result;
            usedCollectors.add(result);
        }
        return result;
    }
    
    private VertexCollector emptyCollector() {
        return emptyCollectors.isEmpty() ? new VertexCollector(this) : emptyCollectors.pop();
    }
    
    public final void forEachExisting(Consumer<VertexCollector> consumer) {
        final int limit = usedCollectors.size();
        if (limit == 0)
            return;

        for (int i = 0; i < limit; i++) {
            consumer.accept(usedCollectors.get(i));
        }
    }

    /** 
     * Sorts pipelines by pipeline index numerical order.
     * DO NOT RETAIN A REFERENCE
     */
    public final BufferPackingList packingListSolid() {
        final BufferPackingList packing = this.packingList;
        packing.clear();
        
        // NB: for solid render, relying on pipelines being added to packing in
        // numerical order so that
        // all chunks can iterate pipelines independently while maintaining same
        // pipeline order within chunk
        this.usedCollectors.sort(solidComparator);
        
        forEachExisting(vertexCollector -> {
            final int vertexCount = vertexCollector.vertexCount();
            if (vertexCount != 0)
                packing.addPacking(vertexCollector.pipeline(), 0, vertexCount);
        });
        return packing;
    }
    
    public final UploadableChunk.Solid packUploadSolid() {
        final BufferPackingList packing = packingListSolid();

        // NB: for solid render, relying on pipelines being added to packing in
        // numerical order so that
        // all chunks can iterate pipelines independently while maintaining same
        // pipeline order within chunk
        return packing.size() == 0 ? null : new UploadableChunk.Solid(packing, this);
    }

    /** 
     * Sorts pipelines from camera - more costly to produce and render.
     * DO NOT RETAIN A REFERENCE
     */
    public final BufferPackingList packingListTranslucent() {
        final BufferPackingList packing = this.packingList;
        packing.clear();
        final PriorityQueue<VertexCollector> sorter = sorters.get();

        final double x = viewX - renderOriginX;
        final double y = viewY - renderOriginY;
        final double z = viewZ - renderOriginZ;

        // Sort quads within each pipeline, while accumulating in priority queue
        forEachExisting(vertexCollector -> {
            if (vertexCollector.vertexCount() != 0) {
                vertexCollector.sortQuads(x, y, z);
                sorter.add(vertexCollector);
            }
        });

        // exploit special case when only one transparent pipeline in this render chunk
        if (sorter.size() == 1) {
            VertexCollector only = sorter.poll();
            packing.addPacking(only.pipeline(), 0, only.vertexCount());
        } else if (sorter.size() != 0) {
            VertexCollector first = sorter.poll();
            VertexCollector second = sorter.poll();
            do {
                // x4 because packing is vertices vs quads
                final int startVertex = first.sortReadIndex() * 4;
                packing.addPacking(first.pipeline(), startVertex, 4 * first.unpackUntilDistance(second.firstUnpackedDistance()));

                if (first.hasUnpackedSortedQuads())
                    sorter.add(first);

                first = second;
                second = sorter.poll();

            } while (second != null);

            final int startVertex = first.sortReadIndex() * 4;
            packing.addPacking(first.pipeline(), startVertex, 4 * first.unpackUntilDistance(Double.MIN_VALUE));
        }
        return packing;
    }
    
    public final UploadableChunk.Translucent packUploadTranslucent() {
        final BufferPackingList packing = packingListTranslucent();
        return packing.size() == 0 ? null : new UploadableChunk.Translucent(packing, this);
    }

    public int[][] getCollectorState(int[][] priorState) {
        int[][] result = priorState;

        final int limit = usedCollectors.size();
        if (result == null || result.length != limit) {
            result = new int[limit][0];
        }
        
        for (int i = 0; i < limit; i++) {
            VertexCollector vc = this.usedCollectors.get(i);
            result[i] = vc == null ? null : vc.saveState(result[i]);
        }
        
        return result;
    }

    public void loadCollectorState(int[][] stateData) {
        clear();
        for (int[] data : stateData) {
            VertexCollector vc = emptyCollector().loadState(data);
            usedCollectors.add(vc);
            vertexCollectors[vc.pipeline().index] = vc;
        }
    }
}
