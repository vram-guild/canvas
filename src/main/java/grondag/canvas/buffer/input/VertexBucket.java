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

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.util.Mth;

import grondag.canvas.render.terrain.TerrainFormat;

public record VertexBucket(int firstVertexIndex, int vertexCount) {
	public static class Sorter {
		private final IntArrayList[] buckets = new IntArrayList[7];
		private int[] swapData;

		Sorter() {
			for (int i = 0; i < 7; ++i) {
				buckets[i] = new IntArrayList();
			}
		}

		void clear() {
			for (final var b : buckets) {
				b.clear();
			}
		}

		VertexBucket[] sort(int[] vertexData, int integerSize) {
			int[] swapData = this.swapData;

			if (swapData == null || swapData.length < integerSize) {
				swapData = new int[Mth.smallestEncompassingPowerOfTwo(integerSize)];
				this.swapData = swapData;
			}

			System.arraycopy(vertexData, 0, swapData, 0, integerSize);
			final VertexBucket[] result = new VertexBucket[7];
			int baseVertexIndex = 0;
			int targetIndex = 0;

			for (int i = 0; i < 7; ++i) {
				final var bucket = buckets[i];

				for (final int sourceIndex : bucket) {
					System.arraycopy(swapData, sourceIndex, vertexData, targetIndex, TerrainFormat.TERRAIN_MATERIAL.quadStrideInts);
					targetIndex += TerrainFormat.TERRAIN_MATERIAL.quadStrideInts;
				}

				final int vertexCount = bucket.size() * 4;
				result[i] = new VertexBucket(baseVertexIndex, vertexCount);
				baseVertexIndex += vertexCount;

				assert baseVertexIndex * TerrainFormat.TERRAIN_MATERIAL.vertexStrideInts == targetIndex;
			}

			assert targetIndex == integerSize;

			return result;
		}

		void add(int bucketIndex, int integerIndex) {
			buckets[bucketIndex].add(integerIndex);
		}
	}
}
