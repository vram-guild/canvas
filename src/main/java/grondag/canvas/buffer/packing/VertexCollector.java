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

import com.google.common.primitives.Doubles;

import grondag.canvas.pipeline.RenderState;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import net.minecraft.util.math.BlockPos;

public class VertexCollector {
    private int[] data;
    private int integerSize = 0;
    private RenderState renderState;
    public final VertexCollectorList parent;

    /**
     * Holds per-quad distance after {@link #sortQuads(double, double, double)} is
     * called
     */
    private double[] perQuadDistance;

    /**
     * Pointer to next sorted quad in sort iteration methods.<br>
     * After {@link #sortQuads(float, float, float)} is called this will be zero.
     */
    private int sortReadIndex = 0;
    
    /**
     * Cached value of {@link #quadCount()}, set when quads are sorted by distance.
     */
    private int sortMaxIndex = 0;

    public VertexCollector(VertexCollectorList parent) {
        data = new int[0x10000];
        this.parent = parent;
    }
    
    public VertexCollector prepare(RenderState pipeline) {
        this.renderState = pipeline;
        return this;
    }

    public void clear() {
        this.integerSize = 0;
        this.renderState = null;
    }

    public RenderState pipeline() {
        return this.renderState;
    }

    public int byteSize() {
        return this.integerSize * 4;
    }

    public int integerSize() {
        return this.integerSize;
    }

    public int vertexCount() {
        return this.integerSize * 4 / this.renderState.pipeline.piplineVertexFormat().vertexStrideBytes;
    }
    
    public int quadCount() {
        return vertexCount() / 4;
    }

    public int[] rawData() {
        return this.data;
    }

    @Override
    public VertexCollector clone() {
        throw new UnsupportedOperationException();
//        VertexCollector result = new VertexCollector(this.data.length);
//        System.arraycopy(this.data, 0, result.data, 0, this.integerSize);
//        result.integerSize = this.integerSize;
//        result.pipeline = this.pipeline;
//        return result;
    }

    private final void checkForSize(int toBeAdded) {
        if ((integerSize + toBeAdded) > data.length) {
            final int curCap = data.length;
            final int newCap = curCap >= 0x40000 ? curCap + 0x40000 : curCap * 2;
            final int copy[] = new int[newCap];
            System.arraycopy(data, 0, copy, 0, integerSize);
            data = copy;
        }
    }

    public final void add(final int i) {
        data[integerSize++] = i;
    }

    public final void add(final float f) {
        this.add(Float.floatToRawIntBits(f));
    }

    public final void pos(final BlockPos pos, float modelX, float modelY, float modelZ) {
        this.checkForSize(this.renderState.pipeline.piplineVertexFormat().vertexStrideBytes);
        this.add((float)(pos.getX() - parent.renderOriginX + modelX));
        this.add((float)(pos.getY() - parent.renderOriginY + modelY));
        this.add((float)(pos.getZ() - parent.renderOriginZ + modelZ));
    }

    private static class QuadSorter {
        double[] perQuadDistance = new double[512];
        int[] quadSwap = new int[64];

        int data[];
        int quadIntStride;

        @SuppressWarnings("serial")
        private final AbstractIntComparator comparator = new AbstractIntComparator() {
            @Override
            public int compare(int a, int b) {
                return Doubles.compare(perQuadDistance[b], perQuadDistance[a]);
            }
        };

        private final Swapper swapper = new Swapper() {
            @Override
            public void swap(int a, int b) {
                double distSwap = perQuadDistance[a];
                perQuadDistance[a] = perQuadDistance[b];
                perQuadDistance[b] = distSwap;

                System.arraycopy(data, a * quadIntStride, quadSwap, 0, quadIntStride);
                System.arraycopy(data, b * quadIntStride, data, a * quadIntStride, quadIntStride);
                System.arraycopy(quadSwap, 0, data, b * quadIntStride, quadIntStride);
            }
        };

        private void doSort(VertexCollector caller, double x, double y, double z) {
            // works because 4 bytes per int
            data = caller.data;
            quadIntStride = caller.renderState.pipeline.piplineVertexFormat().vertexStrideBytes;
            final int vertexIntStride = quadIntStride / 4;
            final int quadCount = caller.vertexCount() / 4;
            if (perQuadDistance.length < quadCount)
                perQuadDistance = new double[quadCount];
            if (quadSwap.length < quadIntStride)
                quadSwap = new int[quadIntStride];

            for (int j = 0; j < quadCount; ++j) {
                perQuadDistance[j] = caller.getDistanceSq(x, y, z, vertexIntStride, j);
            }

            // sort the indexes by distance - farthest first
            it.unimi.dsi.fastutil.Arrays.quickSort(0, quadCount, comparator, swapper);

            if (caller.perQuadDistance == null || caller.perQuadDistance.length < quadCount)
                caller.perQuadDistance = new double[quadCount];
            System.arraycopy(perQuadDistance, 0, caller.perQuadDistance, 0, quadCount);
        }
    }

    private static final ThreadLocal<QuadSorter> quadSorter = new ThreadLocal<QuadSorter>() {
        @Override
        protected QuadSorter initialValue() {
            return new QuadSorter();
        }
    };

    public void sortQuads(double x, double y, double z) {
        quadSorter.get().doSort(this, x, y, z);
        this.sortReadIndex = 0;
        this.sortMaxIndex = this.quadCount();
    }

    private double getDistanceSq(double x, double y, double z, int integerStride, int vertexIndex) {
        // unpack vertex coordinates
        int i = vertexIndex * integerStride * 4;
        double x0 = Float.intBitsToFloat(this.data[i]);
        double y0 = Float.intBitsToFloat(this.data[i + 1]);
        double z0 = Float.intBitsToFloat(this.data[i + 2]);

        i += integerStride;
        double x1 = Float.intBitsToFloat(this.data[i]);
        double y1 = Float.intBitsToFloat(this.data[i + 1]);
        double z1 = Float.intBitsToFloat(this.data[i + 2]);

        i += integerStride;
        double x2 = Float.intBitsToFloat(this.data[i]);
        double y2 = Float.intBitsToFloat(this.data[i + 1]);
        double z2 = Float.intBitsToFloat(this.data[i + 2]);

        i += integerStride;
        double x3 = Float.intBitsToFloat(this.data[i]);
        double y3 = Float.intBitsToFloat(this.data[i + 1]);
        double z3 = Float.intBitsToFloat(this.data[i + 2]);

        // compute average distance by component
        double dx = (x0 + x1 + x2 + x3) * 0.25 - x;
        double dy = (y0 + y1 + y2 + y3) * 0.25 - y;
        double dz = (z0 + z1 + z2 + z3) * 0.25 - z;

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Index of first quad that will be referenced by {@link #unpackUntilDistance(double)}
     */
    public int sortReadIndex() {
        return this.sortReadIndex;
    }
    
    public boolean hasUnpackedSortedQuads() {
        return this.perQuadDistance != null && this.sortReadIndex < this.sortMaxIndex;
    }

    /**
     * Will return {@link Double#MIN_VALUE} if no unpacked quads remaining.
     */
    public double firstUnpackedDistance() {
        return hasUnpackedSortedQuads() ? this.perQuadDistance[this.sortReadIndex] : Double.MIN_VALUE;
    }

    /**
     * Returns the number of quads that are more or as distant than the distance
     * provided and advances the usage pointer so that
     * {@link #firstUnpackedDistance()} will return the distance to the next quad
     * after that.
     * <p>
     * 
     * (All distances are actually squared distances, to be clear.)
     */
    public int unpackUntilDistance(double minDistanceSquared) {
        if (!hasUnpackedSortedQuads())
            return 0;

        int result = 0;
        final int limit = this.sortMaxIndex;
        while (sortReadIndex < limit && minDistanceSquared <= perQuadDistance[sortReadIndex]) {
            result++;
            sortReadIndex++;
        }
        return result;
    }

    public int[] saveState(int[] priorState) {
        final int outputSize = integerSize + 1;
        int[] result = priorState;
        if (result == null || result.length != outputSize)
            result = new int[outputSize];

        result[0] = renderState.index;
        if (integerSize > 0)
            System.arraycopy(data, 0, result, 1, integerSize);
        return result;
    }

    public VertexCollector loadState(int[] stateData) {
        this.renderState = RenderState.get(stateData[0]);
        final int newSize = stateData.length - 1;
        integerSize = 0;
        if (newSize > 0) {
            checkForSize(newSize);
            integerSize = newSize;
            System.arraycopy(stateData, 1, data, 0, newSize);
        }
        return this;
    }
}
