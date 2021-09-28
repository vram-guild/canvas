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

package grondag.canvas.render.terrain.drawlist;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation.SlabAllocation;

abstract class DrawSpecBuilder {
	private static boolean isShadowMap = false;
	private static final IntArrayList triVertexCount = new IntArrayList();
	private static final IntArrayList baseQuadVertexOffset = new IntArrayList();
	private static int quadCount;

	/** NOT THREAD-SAFE. */
	public static int build(ObjectArrayList<SlabAllocation> inputs, ObjectArrayList<DrawSpec> output, boolean isShadowMap, boolean cullBackFace) {
		assert RenderSystem.isOnRenderThread();

		if (inputs.isEmpty()) {
			return 0;
		}

		DrawSpecBuilder.isShadowMap = isShadowMap;
		quadCount = 0;

		final var slab = inputs.get(0).slab;
		final int limit = inputs.size();
		triVertexCount.clear();
		baseQuadVertexOffset.clear();

		if (cullBackFace) {
			for (int i = 0; i < limit; ++i) {
				assert inputs.get(i).slab == slab;
				acceptAllocBucketed(inputs.get(i));
			}
		} else {
			for (int i = 0; i < limit; ++i) {
				assert inputs.get(i).slab == slab;
				acceptAlloc(inputs.get(i));
			}
		}

		output.add(new DrawSpec(slab, triVertexCount.size(), triVertexCount.elements(), baseQuadVertexOffset.elements()));
		inputs.clear();
		return quadCount;
	}

	private static void acceptAlloc(SlabAllocation alloc) {
		quadCount += alloc.quadVertexCount;

		if (alloc.quadVertexCount <= 65536) {
			triVertexCount.add(alloc.triVertexCount);
			baseQuadVertexOffset.add(alloc.baseQuadVertexIndex);
		} else {
			// split buckets that go beyond what short element indexing can do
			int vertexCountRemaining = alloc.quadVertexCount;
			int firstVertexIndex = alloc.baseQuadVertexIndex;

			while (vertexCountRemaining > 0) {
				final int sliceVertexCount = Math.min(vertexCountRemaining, 65536);
				triVertexCount.add(sliceVertexCount / 4 * 6);
				baseQuadVertexOffset.add(alloc.baseQuadVertexIndex + firstVertexIndex);
				firstVertexIndex += sliceVertexCount;
				vertexCountRemaining -= sliceVertexCount;
			}
		}
	}

	private static void acceptAllocBucketed(SlabAllocation alloc) {
		final var region = alloc.region();
		final int bucketFlags = isShadowMap ? region.shadowVisibleFaceFlags() : region.visibleFaceFlags();
		final var buckets = alloc.region().cullBuckets;

		for (int i = 0; i < 7; ++i) {
			if (((1 << i) & bucketFlags) == 0) {
				continue;
			}

			final var bucket = buckets[i];
			final var bucketVertexCount = bucket.vertexCount();

			if (bucketVertexCount > 0) {
				final var bucketQuadCount = bucketVertexCount >> 2;
				quadCount += bucketQuadCount;

				if (bucketVertexCount <= 65536) {
					triVertexCount.add(bucketQuadCount * 6);
					baseQuadVertexOffset.add(alloc.baseQuadVertexIndex + bucket.firstVertexIndex());
				} else {
					// split buckets that go beyond what short element indexing can do
					int vertexCountRemaining = bucketVertexCount;
					int firstVertexIndex = bucket.firstVertexIndex();

					while (vertexCountRemaining > 0) {
						final int sliceVertexCount = Math.min(vertexCountRemaining, 65536);
						triVertexCount.add(sliceVertexCount / 4 * 6);
						baseQuadVertexOffset.add(alloc.baseQuadVertexIndex + firstVertexIndex);
						firstVertexIndex += sliceVertexCount;
						vertexCountRemaining -= sliceVertexCount;
					}
				}
			}
		}
	}
}
