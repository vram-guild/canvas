/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.buffer.encoding;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderState;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryUtil;

import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_QUAD_STRIDE;

import net.minecraft.util.math.MathHelper;

public class VertexCollectorImpl extends AbstractVertexCollector {
	float[] perQuadDistance = new float[512];

	public VertexCollectorImpl prepare(RenderMaterialImpl materialState) {
		clear();
		this.materialState = materialState;
		vertexState(materialState);
		return this;
	}

	public void clear() {
		currentVertexIndex = 0;
		integerSize = 0;
		didPopulateNormal = false;
	}

	public int integerSize() {
		return integerSize;
	}

	public int byteSize() {
		return integerSize * 4;
	}

	public boolean isEmpty() {
		return integerSize == 0;
	}

	public RenderMaterialImpl materialState() {
		return materialState;
	}

	public int vertexCount() {
		return integerSize / CanvasVertexFormats.MATERIAL_VERTEX_STRIDE;
	}

	public int quadCount() {
		return vertexCount() / 4;
	}

	@Override
	public VertexCollectorImpl clone() {
		throw new UnsupportedOperationException();
	}

	public void sortQuads(float x, float y, float z) {
		final int quadCount = vertexCount() / 4;

		if (perQuadDistance.length < quadCount) {
			perQuadDistance = new float[MathHelper.smallestEncompassingPowerOfTwo(quadCount)];
		}

		for (int j = 0; j < quadCount; ++j) {
			perQuadDistance[j] = getDistanceSq(x, y, z, CanvasVertexFormats.MATERIAL_VERTEX_STRIDE, j);
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

	private final int[] swapData = new int[MATERIAL_QUAD_STRIDE * 2];

	private final Swapper swapper = new Swapper() {
		@Override
		public void swap(int a, int b) {
			final float distSwap = perQuadDistance[a];
			perQuadDistance[a] = perQuadDistance[b];
			perQuadDistance[b] = distSwap;

			final int aIndex = a * MATERIAL_QUAD_STRIDE;
			final int bIndex = b * MATERIAL_QUAD_STRIDE;

			System.arraycopy(vertexData, aIndex, swapData, 0, MATERIAL_QUAD_STRIDE);
			System.arraycopy(vertexData, bIndex, swapData, MATERIAL_QUAD_STRIDE, MATERIAL_QUAD_STRIDE);
			System.arraycopy(swapData, 0, vertexData, bIndex, MATERIAL_QUAD_STRIDE);
			System.arraycopy(swapData, MATERIAL_QUAD_STRIDE, vertexData, aIndex, MATERIAL_QUAD_STRIDE);
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

	public VertexCollectorImpl loadState(RenderMaterialImpl state, int[] stateData) {
		if (stateData == null) {
			clear();
			return this;
		}

		materialState = state;
		final int newSize = stateData.length;
		integerSize = 0;

		if (newSize > 0) {
			ensureCapacity(newSize);
			integerSize = newSize;
			System.arraycopy(stateData, 0, vertexData, 0, newSize);
		}

		return this;
	}

	public void toBuffer(IntBuffer intBuffer) {
		intBuffer.put(vertexData, 0, integerSize);
	}

	public void drawAndClear() {
		if (!isEmpty()) {
			drawSingle();
			clear();
		}
	}

	private void sortIfNeeded() {
		if (materialState.sorted) {
			sortQuads(0, 0, 0);
		}
	}

	/** avoid: slow */
	public void drawSingle() {
		sortIfNeeded();

		materialState.renderState.enable();

		final ByteBuffer buffer = TransferBufferAllocator.claim(byteSize());

		final IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.position(0);
		toBuffer(intBuffer);

		CanvasVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL.enableDirect(MemoryUtil.memAddress(buffer));

		GlStateManager.drawArrays(materialState.primitive, 0, vertexCount());

		TransferBufferAllocator.release(buffer);

		RenderState.disable();
	}

	/**
	 * Single-buffer draw, minimizes state changes.
	 * Assumes all collectors are non-empty.
	 */
	public static void drawAndClear(ObjectArrayList<VertexCollectorImpl> drawList) {
		final int limit = drawList.size();

		int bytes = 0;

		for (int i = 0; i < limit; ++i) {
			final VertexCollectorImpl collector = drawList.get(i);
			collector.sortIfNeeded();
			bytes += collector.byteSize();
		}

		// PERF: trial memory mapped here
		final ByteBuffer buffer = TransferBufferAllocator.claim(bytes);
		final IntBuffer intBuffer = buffer.asIntBuffer();
		intBuffer.position(0);

		for (int i = 0; i < limit; ++i) {
			final VertexCollectorImpl collector = drawList.get(i);
			collector.toBuffer(intBuffer);
		}

		CanvasVertexFormats.POSITION_COLOR_TEXTURE_MATERIAL_LIGHT_NORMAL.enableDirect(MemoryUtil.memAddress(buffer));
		int startIndex = 0;

		for (int i = 0; i < limit; ++i) {
			final VertexCollectorImpl collector = drawList.get(i);
			final int vertexCount = collector.vertexCount();
			collector.materialState.renderState.enable();
			GlStateManager.drawArrays(collector.materialState.primitive, startIndex, vertexCount);
			startIndex += vertexCount;
			collector.clear();
		}

		TransferBufferAllocator.release(buffer);
		RenderState.disable();
		drawList.clear();
	}

	@Override
	protected void emitQuad() {
		// WIP: implement condition with indexed draw for terrain
		if (conditionActive) {
			final int newSize = integerSize + CanvasVertexFormats.MATERIAL_QUAD_STRIDE;
			ensureCapacity(newSize + CanvasVertexFormats.MATERIAL_QUAD_STRIDE);
			currentVertexIndex = newSize;
			integerSize = newSize;
		}
	}

	@Override
	public final void add(int[] appendData, int length) {
		final int oldSize = integerSize;
		final int newSize = integerSize + length;
		ensureCapacity(newSize);
		System.arraycopy(appendData, 0, vertexData, oldSize, length);
		integerSize = newSize;
		currentVertexIndex = newSize;
	}

	@Override
	public void add(float... val) {
		final int length = val.length;
		final int oldSize = integerSize;
		final int newSize = integerSize + length;
		final int[] data = vertexData;
		ensureCapacity(newSize);

		for (int i = 0; i < length; ++i) {
			data[i + oldSize] = Float.floatToRawIntBits(val[i]);
		}

		integerSize = newSize;
	}

	public static String debugReport() {
		return String.format("Vertex Collectors - count;%d,   MB allocated:%f", collectorCount.get(), collectorBytes.get() / 1048576f);
	}
}
