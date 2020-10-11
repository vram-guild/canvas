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

package grondag.canvas.wip.encoding;

import java.nio.IntBuffer;

import com.google.common.primitives.Doubles;
import grondag.canvas.wip.state.WipRenderState;
import grondag.fermion.intstream.IntStreamProvider;
import grondag.fermion.intstream.IntStreamProvider.IntStreamImpl;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;

import net.minecraft.util.math.MathHelper;

public class WipVertexCollectorImpl extends WipAbstractVertexCollector {
	private final IntStreamImpl data = INT_STREAM_PROVIDER.claim();
	private int integerSize = 0;

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
	private int intVertexStride = 1;

	public WipVertexCollectorImpl prepare(WipRenderState materialState) {
		this.materialState = materialState;
		intVertexStride = materialState.vertexStrideInts;
		final WipVertexFormat format = materialState.format;
		colorIndex = format.colorIndex;
		textureIndex = format.textureIndex;
		lightIndex = format.lightIndex;
		normalIndex = format.normalIndex;
		spriteId = -1;
		return this;
	}

	public void clear() {
		integerSize = 0;
		data.reset();
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

	public WipRenderState materialState() {
		return materialState;
	}

	public int vertexCount() {
		return integerSize / intVertexStride;
	}

	public int quadCount() {
		return vertexCount() / 4;
	}

	@Override
	public WipVertexCollectorImpl clone() {
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
		final double x0 = Float.intBitsToFloat(data.get(i));
		final double y0 = Float.intBitsToFloat(data.get(i + 1));
		final double z0 = Float.intBitsToFloat(data.get(i + 2));

		i += integerStride;
		final double x1 = Float.intBitsToFloat(data.get(i));
		final double y1 = Float.intBitsToFloat(data.get(i + 1));
		final double z1 = Float.intBitsToFloat(data.get(i + 2));

		i += integerStride;
		final double x2 = Float.intBitsToFloat(data.get(i));
		final double y2 = Float.intBitsToFloat(data.get(i + 1));
		final double z2 = Float.intBitsToFloat(data.get(i + 2));

		i += integerStride;
		final double x3 = Float.intBitsToFloat(data.get(i));
		final double y3 = Float.intBitsToFloat(data.get(i + 1));
		final double z3 = Float.intBitsToFloat(data.get(i + 2));

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
			data.copyTo(0, result, 0, integerSize);
		}

		return result;
	}

	public WipVertexCollectorImpl loadState(WipRenderState state, int[] stateData) {
		if (stateData == null) {
			clear();
			return this;
		}

		materialState = state;
		final int newSize = stateData.length;
		integerSize = 0;

		if (newSize > 0) {
			integerSize = newSize;
			data.copyFrom(0, stateData, 0, newSize);
		}

		return this;
	}

	public void toBuffer(IntBuffer intBuffer) {
		data.copyTo(0, intBuffer, integerSize);
	}

	@Override
	public void next() {
		data.copyFrom(integerSize, vertexData, 0, intVertexStride);
		integerSize += intVertexStride;
	}

	public void drawAndClear() {
		if (!isEmpty()) {
			materialState.draw(this);
			clear();
		}
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
		IntStreamImpl data;
		int quadIntStride;
		private final Swapper swapper = new Swapper() {
			@Override
			public void swap(int a, int b) {
				final double distSwap = perQuadDistance[a];
				perQuadDistance[a] = perQuadDistance[b];
				perQuadDistance[b] = distSwap;

				data.copyTo(a * quadIntStride, quadSwap, 0, quadIntStride);
				data.copyFromDirect(a * quadIntStride, data, b * quadIntStride, quadIntStride);
				data.copyFrom(b * quadIntStride, quadSwap, 0, quadIntStride);
			}
		};

		private void doSort(WipVertexCollectorImpl caller, double x, double y, double z) {
			data = caller.data;

			final int vertexIntStride = caller.intVertexStride;
			quadIntStride = vertexIntStride * 4;
			final int quadCount = caller.vertexCount() / 4;

			if (perQuadDistance.length < quadCount) {
				perQuadDistance = new double[MathHelper.smallestEncompassingPowerOfTwo(quadCount)];
			}

			if (quadSwap.length < quadIntStride) {
				quadSwap = new int[MathHelper.smallestEncompassingPowerOfTwo(quadIntStride)];
			}

			for (int j = 0; j < quadCount; ++j) {
				perQuadDistance[j] = caller.getDistanceSq(x, y, z, vertexIntStride, j);
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

	// WIP2: confirm parameters after integrating terrain
	public static final IntStreamProvider INT_STREAM_PROVIDER = new IntStreamProvider(4096, 16, 4096);
}
