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

package grondag.canvas.buffer.encoding;

import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_INT_QUAD_STRIDE;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_INT_VERTEX_STRIDE;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.math.MathHelper;

import grondag.canvas.material.state.RenderState;

public class ArrayVertexCollector implements VertexCollector {
	private int capacity = 1024;
	private int[] vertexData = new int[capacity];
	private float[] perQuadDistance = new float[512];
	/** also the index of the first vertex when used in VertexConsumer mode. */
	private int integerSize = 0;

	public final RenderState renderState;

	public ArrayVertexCollector(RenderState renderState) {
		this.renderState = renderState;
		arrayCount.incrementAndGet();
		arryBytes.addAndGet(capacity);
	}

	protected void grow(int newSize) {
		if (newSize > capacity) {
			final int newCapacity = MathHelper.smallestEncompassingPowerOfTwo(newSize);
			final int[] newData = new int[newCapacity];
			System.arraycopy(vertexData, 0, newData, 0, capacity);
			arryBytes.addAndGet(newCapacity - capacity);
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

	public int vertexCount() {
		return integerSize / MATERIAL_INT_VERTEX_STRIDE;
	}

	public int quadCount() {
		return vertexCount() / 4;
	}

	public boolean isEmpty() {
		return integerSize == 0;
	}

	static AtomicInteger arrayCount = new AtomicInteger();
	static AtomicInteger arryBytes = new AtomicInteger();

	public static String debugReport() {
		return String.format("CPU Vertex Arrays - count;%d,   MB allocated:%f", arrayCount.get(), arryBytes.get() / 1048576f);
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

	public void toBuffer(IntBuffer intBuffer) {
		intBuffer.put(vertexData, 0, integerSize);
	}

	public void clear() {
		integerSize = 0;
	}

	public void sortQuads(float x, float y, float z) {
		final int quadCount = vertexCount() / 4;

		if (perQuadDistance.length < quadCount) {
			perQuadDistance = new float[MathHelper.smallestEncompassingPowerOfTwo(quadCount)];
		}

		for (int j = 0; j < quadCount; ++j) {
			perQuadDistance[j] = getDistanceSq(x, y, z, MATERIAL_INT_VERTEX_STRIDE, j);
		}

		// sort the indexes by distance - farthest first
		// mergesort is important here - quicksort causes problems
		// PERF: consider sorting primitive packed long array with distance in high bits
		// and then use result to reorder the array. Will need to copy vertex data.
		it.unimi.dsi.fastutil.Arrays.mergeSort(0, quadCount, comparator, swapper);
	}

	private final IntComparator comparator = new IntComparator() {
		@Override
		public int compare(int a, int b) {
			return Float.compare(perQuadDistance[b], perQuadDistance[a]);
		}
	};

	private final int[] swapData = new int[MATERIAL_INT_QUAD_STRIDE * 2];

	private final Swapper swapper = new Swapper() {
		@Override
		public void swap(int a, int b) {
			final float distSwap = perQuadDistance[a];
			perQuadDistance[a] = perQuadDistance[b];
			perQuadDistance[b] = distSwap;

			final int aIndex = a * MATERIAL_INT_QUAD_STRIDE;
			final int bIndex = b * MATERIAL_INT_QUAD_STRIDE;

			System.arraycopy(vertexData, aIndex, swapData, 0, MATERIAL_INT_QUAD_STRIDE);
			System.arraycopy(vertexData, bIndex, swapData, MATERIAL_INT_QUAD_STRIDE, MATERIAL_INT_QUAD_STRIDE);
			System.arraycopy(swapData, 0, vertexData, bIndex, MATERIAL_INT_QUAD_STRIDE);
			System.arraycopy(swapData, MATERIAL_INT_QUAD_STRIDE, vertexData, aIndex, MATERIAL_INT_QUAD_STRIDE);
		}
	};

	private float getDistanceSq(float x, float y, float z, int integerStride, int vertexIndex) {
		// unpack vertex coordinates
		int i = vertexIndex * integerStride * 4;
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

	void sortIfNeeded() {
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
		final DrawableBuffer buffer = new DrawableBuffer(drawList);
		buffer.draw(false);
		buffer.close();
	}
}
