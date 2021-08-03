/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.buffer.input;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.util.DrawableStream;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.RegionRenderSectorMap.RegionRenderSector;
import grondag.canvas.render.terrain.TerrainFormat;

public class ArrayVertexCollector implements VertexCollector {
	private final int quadStrideInts;
	public final boolean isTerrain;
	private int capacity = 1024;
	private int[] vertexData = new int[capacity];
	private float[] perQuadDistance = new float[512];
	private final int[] swapData;
	private boolean didSwap = false;

	/** also the index of the first vertex when used in VertexConsumer mode. */
	private int integerSize = 0;

	public final RenderState renderState;
	private final VertexBucket.Sorter bucketSorter;

	public ArrayVertexCollector(RenderState renderState, boolean isTerrain) {
		this.renderState = renderState;
		this.isTerrain = isTerrain;
		bucketSorter = isTerrain && !renderState.sorted ? new VertexBucket.Sorter() : null;
		quadStrideInts = isTerrain ? TerrainFormat.TERRAIN_MATERIAL.quadStrideInts : CanvasVertexFormats.STANDARD_MATERIAL_FORMAT.quadStrideInts;
		swapData = new int[quadStrideInts * 2];
		arrayCount.incrementAndGet();
		arryBytes.addAndGet(capacity);
	}

	protected void grow(int newSize) {
		final int oldCapacity = capacity;

		if (newSize > oldCapacity) {
			final int newCapacity = MathHelper.smallestEncompassingPowerOfTwo(newSize);
			final int[] newData = new int[newCapacity];
			System.arraycopy(vertexData, 0, newData, 0, oldCapacity);
			arryBytes.addAndGet(newCapacity - oldCapacity);
			capacity = newCapacity;
			vertexData = newData;
		}
	}

	public int integerSize() {
		return integerSize;
	}

	public int byteSize() {
		return integerSize * 4;
	}

	public int quadCount() {
		return integerSize / quadStrideInts;
	}

	public boolean isEmpty() {
		return integerSize == 0;
	}

	static AtomicInteger arrayCount = new AtomicInteger();
	static AtomicInteger arryBytes = new AtomicInteger();

	public static String debugReport() {
		return String.format("Vertex collectors: %d %4.1fMb", arrayCount.get(), arryBytes.get() / 1048576f);
	}

	@Override
	public int[] data() {
		return vertexData;
	}

	@Override
	public int allocate(int size) {
		final int result = integerSize;
		final int newSize = result + size;
		grow(newSize);
		integerSize = newSize;
		return result;
	}

	@Override
	public int allocate(int size, int bucketIndex) {
		assert isTerrain;

		if (bucketSorter != null) {
			bucketSorter.add(bucketIndex, integerSize);
		}

		return allocate(size);
	}

	public void toBuffer(IntBuffer intBuffer, int startingIndex) {
		intBuffer.put(vertexData, startingIndex, integerSize);
	}

	public void toBuffer(int collectorSourceIndex, TransferBuffer targetBuffer, int bufferTargetIndex) {
		targetBuffer.put(vertexData, collectorSourceIndex, bufferTargetIndex, integerSize);
	}

	public void clear() {
		integerSize = 0;

		if (bucketSorter != null) {
			bucketSorter.clear();
		}
	}

	public VertexBucket[] sortVertexBuckets() {
		return bucketSorter == null ? null : bucketSorter.sort(vertexData, integerSize);
	}

	public boolean sortTerrainQuads(Vec3d sortPos, RegionRenderSector sector) {
		assert isTerrain;

		return sortQuads(
			(float) (sortPos.x - sector.paddedBlockOriginX),
			(float) (sortPos.y - sector.paddedBlockOriginY),
			(float) (sortPos.z - sector.paddedBlockOriginZ)
		);
	}

	private boolean sortQuads(float x, float y, float z) {
		final int quadCount = quadCount();
		final QuadDistanceFunc distanceFunc = isTerrain ? quadDistanceTerrain : quadDistanceStandard;

		if (perQuadDistance.length < quadCount) {
			perQuadDistance = new float[MathHelper.smallestEncompassingPowerOfTwo(quadCount)];
		}

		for (int j = 0; j < quadCount; ++j) {
			perQuadDistance[j] = distanceFunc.compute(x, y, z, j);
		}

		didSwap = false;

		// sort the indexes by distance - farthest first
		// mergesort is important here - quicksort causes problems
		// PERF: consider sorting primitive packed long array with distance in high bits
		// and then use result to reorder the array. Will need to copy vertex data.
		it.unimi.dsi.fastutil.Arrays.mergeSort(0, quadCount, comparator, swapper);

		return didSwap;
	}

	private interface QuadDistanceFunc {
		float compute(float x, float y, float z, int quadIndex);
	}

	private final IntComparator comparator = new IntComparator() {
		@Override
		public int compare(int a, int b) {
			return Float.compare(perQuadDistance[b], perQuadDistance[a]);
		}
	};

	private final Swapper swapper = new Swapper() {
		@Override
		public void swap(int a, int b) {
			didSwap = true;
			final float distSwap = perQuadDistance[a];
			perQuadDistance[a] = perQuadDistance[b];
			perQuadDistance[b] = distSwap;

			final int aIndex = a * quadStrideInts;
			final int bIndex = b * quadStrideInts;

			System.arraycopy(vertexData, aIndex, swapData, 0, quadStrideInts);
			System.arraycopy(vertexData, bIndex, swapData, quadStrideInts, quadStrideInts);
			System.arraycopy(swapData, 0, vertexData, bIndex, quadStrideInts);
			System.arraycopy(swapData, quadStrideInts, vertexData, aIndex, quadStrideInts);
		}
	};

	private final QuadDistanceFunc quadDistanceStandard = this::getDistanceSq;

	private float getDistanceSq(float x, float y, float z, int quadIndex) {
		final int integerStride = quadStrideInts / 4;

		// unpack vertex coordinates
		int i = quadIndex * quadStrideInts;
		final float x0 = Float.intBitsToFloat(vertexData[i]);
		final float y0 = Float.intBitsToFloat(vertexData[i + 1]);
		final float z0 = Float.intBitsToFloat(vertexData[i + 2]);

		i += integerStride;
		final float x1 = Float.intBitsToFloat(vertexData[i]);
		final float y1 = Float.intBitsToFloat(vertexData[i + 1]);
		final float z1 = Float.intBitsToFloat(vertexData[i + 2]);

		i += integerStride;
		final float x2 = Float.intBitsToFloat(vertexData[i]);
		final float y2 = Float.intBitsToFloat(vertexData[i + 1]);
		final float z2 = Float.intBitsToFloat(vertexData[i + 2]);

		i += integerStride;
		final float x3 = Float.intBitsToFloat(vertexData[i]);
		final float y3 = Float.intBitsToFloat(vertexData[i + 1]);
		final float z3 = Float.intBitsToFloat(vertexData[i + 2]);

		// compute average distance by component
		final float dx = (x0 + x1 + x2 + x3) * 0.25f - x;
		final float dy = (y0 + y1 + y2 + y3) * 0.25f - y;
		final float dz = (z0 + z1 + z2 + z3) * 0.25f - z;

		return dx * dx + dy * dy + dz * dz;
	}

	private final QuadDistanceFunc quadDistanceTerrain = this::getDistanceSqTerrain;
	private static final float POS_CONVERSION = 1f / 0xFFFF;

	private float getDistanceSqTerrain(float x, float y, float z, int quadIndex) {
		final int integerStride = quadStrideInts / 4;

		// unpack vertex coordinates
		int i = quadIndex * quadStrideInts;
		final int pos0 = vertexData[i + 2];
		final float x0 = (pos0 & 0xFF) + (vertexData[i] >>> 16) * POS_CONVERSION;
		final float y0 = ((pos0 >> 8) & 0xFF) + (vertexData[i + 1] & 0xFFFF) * POS_CONVERSION;
		final float z0 = ((pos0 >> 16) & 0xFF) + (vertexData[i + 1] >>> 16) * POS_CONVERSION;

		i += integerStride;
		final int pos1 = vertexData[i + 2];
		final float x1 = (pos1 & 0xFF) + (vertexData[i] >>> 16) * POS_CONVERSION;
		final float y1 = ((pos1 >> 8) & 0xFF) + (vertexData[i + 1] & 0xFFFF) * POS_CONVERSION;
		final float z1 = ((pos1 >> 16) & 0xFF) + (vertexData[i + 1] >>> 16) * POS_CONVERSION;

		i += integerStride;
		final int pos2 = vertexData[i + 2];
		final float x2 = (pos2 & 0xFF) + (vertexData[i] >>> 16) * POS_CONVERSION;
		final float y2 = ((pos2 >> 8) & 0xFF) + (vertexData[i + 1] & 0xFFFF) * POS_CONVERSION;
		final float z2 = ((pos2 >> 16) & 0xFF) + (vertexData[i + 1] >>> 16) * POS_CONVERSION;

		i += integerStride;
		final int pos3 = vertexData[i + 2];
		final float x3 = (pos3 & 0xFF) + (vertexData[i] >>> 16) * POS_CONVERSION;
		final float y3 = ((pos3 >> 8) & 0xFF) + (vertexData[i + 1] & 0xFFFF) * POS_CONVERSION;
		final float z3 = ((pos3 >> 16) & 0xFF) + (vertexData[i + 1] >>> 16) * POS_CONVERSION;

		// compute average distance by component
		final float dx = (x0 + x1 + x2 + x3) * 0.25f - x;
		final float dy = (y0 + y1 + y2 + y3) * 0.25f - y;
		final float dz = (z0 + z1 + z2 + z3) * 0.25f - z;

		return dx * dx + dy * dy + dz * dz;
	}

	public int[] saveState(int[] priorState) {
		final int integerSize = this.integerSize;

		if (integerSize == 0) {
			return null;
		}

		int[] result = priorState;

		if (result == null || result.length != integerSize) {
			result = new int[integerSize];
		}

		if (integerSize > 0) {
			System.arraycopy(vertexData, 0, result, 0, integerSize);
		}

		return result;
	}

	public void loadState(int[] stateData) {
		clear();

		if (stateData != null) {
			final int size = stateData.length;
			allocate(size);
			System.arraycopy(stateData, 0, vertexData, 0, size);
		}
	}

	public RenderState renderState() {
		return renderState;
	}

	public void draw(boolean clear) {
		if (!isEmpty()) {
			drawSingle();

			if (clear) {
				clear();
			}
		}
	}

	public void sortIfNeeded() {
		if (renderState.sorted) {
			sortQuads(0, 0, 0);
		}
	}

	/** Avoid: slow. */
	public void drawSingle() {
		// PERF: allocation - or eliminate this
		final ObjectArrayList<ArrayVertexCollector> drawList = new ObjectArrayList<>();
		drawList.add(this);
		draw(drawList);
	}

	/**
	 * Single-buffer draw, minimizes state changes.
	 * Assumes all collectors are non-empty.
	 */
	public static void draw(ObjectArrayList<ArrayVertexCollector> drawList) {
		final DrawableStream buffer = new DrawableStream(drawList);
		buffer.draw(false);
		buffer.close();
	}
}
