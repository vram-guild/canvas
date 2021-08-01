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

import java.util.IdentityHashMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.render.terrain.cluster.ClusteredDrawableStorage;
import grondag.canvas.render.terrain.cluster.Slab;
import grondag.canvas.render.terrain.cluster.VertexCluster;
import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation.SlabAllocation;
import grondag.canvas.render.terrain.cluster.VertexClusterRealm;
import grondag.canvas.varia.GFX;

public class ClusterDrawList {
	final ObjectArrayList<ClusteredDrawableStorage> stores = new ObjectArrayList<>();
	final VertexCluster cluster;
	final RealmDrawList owner;
	private final ObjectArrayList<DrawSpec> drawSpecs = new ObjectArrayList<>();

	ClusterDrawList(VertexCluster cluster, RealmDrawList owner) {
		this.cluster = cluster;
		this.owner = owner;
	}

	void build() {
		assert drawSpecs.isEmpty();

		if (cluster.realm == VertexClusterRealm.TRANSLUCENT) {
			buildTranslucent();
		} else {
			buildSolid();
		}
	}

	/** Maintains region sort order at the cost of extra binds/calls if needed. */
	private void buildTranslucent() {
		Slab lastSlab = null;
		final ObjectArrayList<SlabAllocation> specAllocations = new ObjectArrayList<>();
		int specQuadVertexCount = 0;

		for (var region : stores) {
			for (var alloc : region.allocation().allocations()) {
				if (alloc.slab != lastSlab) {
					// NB: addSpec checks for empty region list (will be true for first region)
					// and also clears the list when done.
					addSpec(specAllocations, specQuadVertexCount);
					specQuadVertexCount = 0;
					lastSlab = alloc.slab;
				}

				specAllocations.add(alloc);
				specQuadVertexCount += alloc.quadVertexCount;
			}
		}

		addSpec(specAllocations, specQuadVertexCount);
	}

	private static class SolidSpecList extends ObjectArrayList<SlabAllocation> {
		private int specQuadVertexCount;
	}

	/** Minimizes binds/calls. */
	private void buildSolid() {
		final IdentityHashMap<Slab, SolidSpecList> map = new IdentityHashMap<>();

		// first group regions by slab
		for (var region : stores) {
			for (var alloc : region.allocation().allocations()) {
				var list = map.get(alloc.slab);

				if (list == null) {
					list = new SolidSpecList();
					map.put(alloc.slab, list);
				}

				list.add(alloc);
				list.specQuadVertexCount += alloc.quadVertexCount;
			}
		}

		for (var list: map.values()) {
			assert list.specQuadVertexCount <= SlabIndex.MAX_SLAB_INDEX_QUAD_VERTEX_COUNT;
			addSpec(list, list.specQuadVertexCount);
		}
	}

	/**
	 * Returns the index slab that should be used for next call.
	 * Does nothing if region list is empty and clears region list when done.
	 */
	private void addSpec(ObjectArrayList<SlabAllocation> specAllocations, int specQuadVertexCount) {
		assert specQuadVertexCount >= 0;

		if (specQuadVertexCount == 0) {
			return;
		}

		assert !specAllocations.isEmpty() : "Vertex count is non-zero but region list is empty.";

		final var slab = specAllocations.get(0).slab;
		final int limit = specAllocations.size();
		final int[] triVertexCount = new int[limit];
		final int[] baseQuadVertexOffset = new int[limit];
		int maxTriVertexCount = 0;

		for (int i = 0; i < limit; ++i) {
			final var alloc = specAllocations.get(i);
			assert alloc.slab == slab;
			maxTriVertexCount = Math.max(maxTriVertexCount, alloc.triVertexCount);
			triVertexCount[i] = alloc.triVertexCount;
			baseQuadVertexOffset[i] = alloc.baseQuadVertexIndex;
		}

		drawSpecs.add(new DrawSpec(slab, maxTriVertexCount + 6, triVertexCount, baseQuadVertexOffset));
		specAllocations.clear();
	}

	public void draw() {
		drawNew();
	}

	public void drawNew() {
		final int limit = drawSpecs.size();

		for (int i = 0; i < limit; ++i) {
			final var spec = drawSpecs.get(i);
			spec.bind();
			GFX.glMultiDrawElementsBaseVertex(GFX.GL_TRIANGLES, spec.triVertexCount(), GFX.GL_UNSIGNED_SHORT, spec.triIndexOffset(), spec.baseQuadVertexOffset());
		}
	}

	// WIP: use a version of this for new lists and gradually compact?
	public void drawOld() {
		final int limit = stores.size();

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			ClusteredDrawableStorage store = stores.get(regionIndex);

			for (var alloc : store.allocation().allocations()) {
				alloc.bind();

				// NB offset is baseQuadVertexIndex * 3 because the offset is in bytes
				// six tri vertices per four quad vertices at 2 bytes each gives 6 / 4 * 2 = 3
				GFX.drawElements(GFX.GL_TRIANGLES, alloc.triVertexCount, GFX.GL_UNSIGNED_SHORT, 0L);
			}
		}
	}

	public void add(ClusteredDrawableStorage storage) {
		assert storage.allocation().cluster() == cluster;
		stores.add(storage);
	}

	public void invalidate() {
		owner.invalidate();
	}

	public void release() {
		drawSpecs.forEach(DrawSpec::release);
	}
}
