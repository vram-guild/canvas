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

package grondag.canvas.render.terrain.cluster.drawlist;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation.SlabAllocation;

abstract class DrawSpecBuilder {
	protected abstract void acceptAlloc(SlabAllocation alloc);

	final void build(ObjectArrayList<SlabAllocation> inputs, ObjectArrayList<DrawSpec> output) {
		assert RenderSystem.isOnRenderThread();

		if (inputs.isEmpty()) {
			return;
		}

		final var slab = inputs.get(0).slab;
		final int limit = inputs.size();
		triVertexCount.clear();
		baseQuadVertexOffset.clear();

		for (int i = 0; i < limit; ++i) {
			final var alloc = inputs.get(i);
			assert alloc.slab == slab;
			acceptAlloc(alloc);
		}

		output.add(new DrawSpec(slab, triVertexCount.size(), triVertexCount.elements(), baseQuadVertexOffset.elements()));
		inputs.clear();
	}

	static final IntArrayList triVertexCount = new IntArrayList();
	static final IntArrayList baseQuadVertexOffset = new IntArrayList();

	static final DrawSpecBuilder SOLID = new DrawSpecBuilder() {
		@Override
		protected void acceptAlloc(SlabAllocation alloc) {
			final int bucketFlags = alloc.region().bucketFlags();
			final var buckets = alloc.region().buckets;

			for (int i = 0; i < 7; ++i) {
				if (((1 << i) & bucketFlags) == 0) {
					continue;
				}

				var bucket = buckets[i];

				if (bucket.vertexCount() > 0) {
					triVertexCount.add(bucket.vertexCount() / 4 * 6);
					baseQuadVertexOffset.add(alloc.baseQuadVertexIndex + bucket.firstVertexIndex());
				}
			}
		}
	};

	static final DrawSpecBuilder TRANSLUCENT = new DrawSpecBuilder() {
		@Override
		protected void acceptAlloc(SlabAllocation alloc) {
			triVertexCount.add(alloc.triVertexCount);
			baseQuadVertexOffset.add(alloc.baseQuadVertexIndex);
		}
	};
}
