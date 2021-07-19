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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.render.terrain.cluster.ClusteredDrawableStorage;
import grondag.canvas.render.terrain.cluster.Slab;
import grondag.canvas.render.terrain.cluster.VertexCluster;
import grondag.canvas.render.terrain.cluster.VertexClusterRealm;
import grondag.canvas.varia.GFX;

public class ClusterDrawListState {
	final ObjectArrayList<ClusteredDrawableStorage> stores = new ObjectArrayList<>();
	final VertexCluster cluster;
	private boolean isUnbuilt = true;
	private int drawCount = 0;
	private final ObjectArrayList<Slab> slabs = new ObjectArrayList<>();
	private final ObjectArrayList<SlabIndex> slabIndices = new ObjectArrayList<>();

	ClusterDrawListState(VertexCluster cluster) {
		this.cluster = cluster;
	}

	private void build() {
		assert drawCount == 0;
		assert slabs.isEmpty();
		assert slabIndices.isEmpty();

		if (cluster.owner == VertexClusterRealm.TRANSLUCENT) {
			// need to maintain sort order
			buildTranslucent();
		} else {
			buildSolid();
		}
	}

	private void buildTranslucent() {
		Slab lastSlab = null;

		for (var region : stores) {
			if (region.slab() != lastSlab) {
				slabIndices.add(SlabIndex.claim());
			}
		}
	}

	private void buildSolid() {
		//
	}

	public void draw() {
		if (isUnbuilt) {
			build();
			isUnbuilt = false;
		}

		final int limit = stores.size();

		Slab lastSlab = null;

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			ClusteredDrawableStorage store = stores.get(regionIndex);
			Slab slab = store.slab();

			if (slab != lastSlab) {
				slab.bind();
				SlabIndex.fullSlabIndex().bind();
				lastSlab = slab;
			}

			// NB offset is baseQuadVertexIndex * 3 because the offset is in bytes
			// six tri vertices per four quad vertices at 2 bytes each gives 6 / 4 * 2 = 3
			GFX.drawElements(GFX.GL_TRIANGLES, store.triVertexCount, GFX.GL_UNSIGNED_SHORT, store.baseQuadVertexIndex() * 3);
		}

		SlabIndex.fullSlabIndex().unbind();
	}

	// WIP: use a version of this for new lists and gradually compact
	public void drawSlow() {
		final int limit = stores.size();

		Slab lastSlab = null;

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			ClusteredDrawableStorage store = stores.get(regionIndex);
			Slab slab = store.slab();

			if (slab != lastSlab) {
				slab.bind();
				SlabIndex.fullSlabIndex().bind();
				lastSlab = slab;
			}

			// NB offset is baseQuadVertexIndex * 3 because the offset is in bytes
			// six tri vertices per four quad vertices at 2 bytes each gives 6 / 4 * 2 = 3
			GFX.drawElements(GFX.GL_TRIANGLES, store.triVertexCount, GFX.GL_UNSIGNED_SHORT, store.baseQuadVertexIndex() * 3);
		}

		SlabIndex.fullSlabIndex().unbind();
	}

	public void add(ClusteredDrawableStorage storage) {
		assert storage.getCluster() == cluster;
		stores.add(storage);
	}
}
