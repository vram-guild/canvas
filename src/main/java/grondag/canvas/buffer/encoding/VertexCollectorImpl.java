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

import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.platform.GlStateManager;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderState;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.util.math.MathHelper;

public class VertexCollectorImpl extends AbstractVertexCollector {
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

	public VertexCollectorImpl(RenderContextState contextState) {
		super(contextState);
	}

	public VertexCollectorImpl prepare(RenderMaterialImpl materialState) {
		clear();
		this.materialState = materialState;
		vertexState(materialState);
		return this;
	}

	public void clear() {
		firstVertexIndex = 0;
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

	public void sortQuads(double x, double y, double z) {
		quadSorter.get().doSort(this, x, y, z);
		sortReadIndex = 0;
		sortMaxIndex = quadCount();
	}

	private double getDistanceSq(double x, double y, double z, int integerStride, int vertexIndex) {
		// unpack vertex coordinates
		int i = vertexIndex * integerStride * 4;
		final double x0 = Float.intBitsToFloat(vertexData[i]);
		final double y0 = Float.intBitsToFloat(vertexData[i + 1]);
		final double z0 = Float.intBitsToFloat(vertexData[i + 2]);

		i += integerStride;
		final double x1 = Float.intBitsToFloat(vertexData[i]);
		final double y1 = Float.intBitsToFloat(vertexData[i + 1]);
		final double z1 = Float.intBitsToFloat(vertexData[i + 2]);

		i += integerStride;
		final double x2 = Float.intBitsToFloat(vertexData[i]);
		final double y2 = Float.intBitsToFloat(vertexData[i + 1]);
		final double z2 = Float.intBitsToFloat(vertexData[i + 2]);

		i += integerStride;
		final double x3 = Float.intBitsToFloat(vertexData[i]);
		final double y3 = Float.intBitsToFloat(vertexData[i + 1]);
		final double z3 = Float.intBitsToFloat(vertexData[i + 2]);

		// compute average distance by component
		final double dx = (x0 + x1 + x2 + x3) * 0.25 - x;
		final double dy = (y0 + y1 + y2 + y3) * 0.25 - y;
		final double dz = (z0 + z1 + z2 + z3) * 0.25 - z;

		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Index of first quad that will be referenced by {@link #unpackUntilDistance(double)}
	 */
	public int sortReadIndex() {
		return sortReadIndex;
	}

	public boolean hasUnpackedSortedQuads() {
		return perQuadDistance != null && sortReadIndex < sortMaxIndex;
	}

	/**
	 * Will return {@link Double#MIN_VALUE} if no unpacked quads remaining.
	 */
	public double firstUnpackedDistance() {
		return hasUnpackedSortedQuads() ? perQuadDistance[sortReadIndex] : Double.MIN_VALUE;
	}

	/**
	 * Returns the number of quads that are more or as distant than the distance
	 * provided and advances the usage pointer so that
	 * {@link #firstUnpackedDistance()} will return the distance to the next quad
	 * after that.
	 * <p>
	 * <p>
	 * (All distances are actually squared distances, to be clear.)
	 */
	public int unpackUntilDistance(double minDistanceSquared) {
		if (!hasUnpackedSortedQuads()) {
			return 0;
		}

		int result = 0;
		final int limit = sortMaxIndex;
		while (sortReadIndex < limit && minDistanceSquared <= perQuadDistance[sortReadIndex]) {
			result++;
			sortReadIndex++;
		}
		return result;
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

	private static class QuadSorter {
		double[] perQuadDistance = new double[512];

		private final IntComparator comparator = new IntComparator() {
			@Override
			public int compare(int a, int b) {
				return Doubles.compare(perQuadDistance[b], perQuadDistance[a]);
			}
		};

		int[] quadSwap = new int[128];
		int[] data;
		int quadIntStride;

		private final Swapper swapper = new Swapper() {
			@Override
			public void swap(int a, int b) {
				final double distSwap = perQuadDistance[a];
				perQuadDistance[a] = perQuadDistance[b];
				perQuadDistance[b] = distSwap;

				System.arraycopy(data, a * quadIntStride, quadSwap, 0, quadIntStride);
				//data.copyTo(a * quadIntStride, quadSwap, 0, quadIntStride);
				System.arraycopy(data, b * quadIntStride, data, a * quadIntStride, quadIntStride);
				//data.copyFromDirect(a * quadIntStride, data, b * quadIntStride, quadIntStride);
				System.arraycopy(quadSwap, 0, data, b * quadIntStride, quadIntStride);
				//data.copyFrom(b * quadIntStride, quadSwap, 0, quadIntStride);
			}
		};

		private void doSort(VertexCollectorImpl caller, double x, double y, double z) {
			data = caller.vertexData;

			final int quadCount = caller.vertexCount() / 4;

			if (perQuadDistance.length < quadCount) {
				perQuadDistance = new double[MathHelper.smallestEncompassingPowerOfTwo(quadCount)];
			}

			if (quadSwap.length < quadIntStride) {
				quadSwap = new int[MathHelper.smallestEncompassingPowerOfTwo(quadIntStride)];
			}

			for (int j = 0; j < quadCount; ++j) {
				perQuadDistance[j] = caller.getDistanceSq(x, y, z, CanvasVertexFormats.MATERIAL_VERTEX_STRIDE, j);
			}

			// sort the indexes by distance - farthest first
			it.unimi.dsi.fastutil.Arrays.quickSort(0, quadCount, comparator, swapper);

			if (caller.perQuadDistance == null || caller.perQuadDistance.length < quadCount) {
				caller.perQuadDistance = new double[perQuadDistance.length];
			}

			System.arraycopy(perQuadDistance, 0, caller.perQuadDistance, 0, quadCount);
		}
	}

	private static final ThreadLocal<QuadSorter> quadSorter = new ThreadLocal<QuadSorter>() {
		@Override
		protected QuadSorter initialValue() {
			return new QuadSorter();
		}
	};

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
		// WIP2: implement condition with indexed draw for terrain
		if (conditionActive) {
			final int oldSize = integerSize;
			final int newSize = oldSize + CanvasVertexFormats.MATERIAL_QUAD_STRIDE;
			ensureCapacity(newSize);
			firstVertexIndex = integerSize;
			currentVertexIndex = firstVertexIndex;
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
