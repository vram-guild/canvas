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
		final var buckets = alloc.region().faceBuckets;

		for (int i = 0; i < 7; ++i) {
			if (((1 << i) & bucketFlags) == 0) {
				continue;
			}

			final var bucket = buckets[i];
			final var bucketVertexCount = isShadowMap ? bucket.shadowVertexCount() : bucket.colorVertexCount();
			final var bucketVertexIndex = isShadowMap ? bucket.shadowVertexIndex() : bucket.colorVertexIndex();

			if (bucketVertexCount > 0) {
				final var bucketQuadCount = bucketVertexCount >> 2;
				quadCount += bucketQuadCount;

				if (bucketVertexCount <= 65536) {
					triVertexCount.add(bucketQuadCount * 6);
					baseQuadVertexOffset.add(alloc.baseQuadVertexIndex + bucketVertexIndex);
				} else {
					// split buckets that go beyond what short element indexing can do
					int vertexCountRemaining = bucketVertexCount;
					int firstVertexIndex = bucketVertexIndex;

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
