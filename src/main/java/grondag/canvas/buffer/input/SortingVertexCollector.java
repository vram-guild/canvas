/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.buffer.input;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;

public class SortingVertexCollector extends SimpleVertexCollector {
	private float[] perQuadDistance = new float[512];
	private final int[] swapData;
	private boolean didSwap = false;

	public SortingVertexCollector(RenderState renderState, boolean isTerrain) {
		super(renderState, true, isTerrain);
		swapData = new int[quadStrideInts * 2];
	}

	public boolean sortTerrainQuads(Vec3 sortPos, RegionRenderSector sector) {
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
			perQuadDistance = new float[Mth.smallestEncompassingPowerOfTwo(quadCount)];
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

	@Override
	public boolean sorted() {
		return true;
	}
}
