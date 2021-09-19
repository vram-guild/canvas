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
