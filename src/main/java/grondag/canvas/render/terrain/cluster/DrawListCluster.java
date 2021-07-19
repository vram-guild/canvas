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

package grondag.canvas.render.terrain.cluster;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.varia.GFX;

public class DrawListCluster {
	private final ObjectArrayList<ClusteredDrawableStorage> stores = new ObjectArrayList<>();
	private final VertexCluster cluster;

	DrawListCluster(VertexCluster cluster) {
		this.cluster = cluster;
	}

	public void draw() {
		final int limit = stores.size();

		Slab lastSlab = null;

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			ClusteredDrawableStorage store = stores.get(regionIndex);
			Slab slab = store.slab();

			if (slab != lastSlab) {
				slab.bind();
				lastSlab = slab;
			}

			// NB offset is baseQuadVertexIndex * 3 because the offset is in bytes
			// six tri vertices per four quad vertices at 2 bytes each gives 6 / 4 * 2 = 3
			GFX.drawElements(GFX.GL_TRIANGLES, store.triVertexCount, GFX.GL_UNSIGNED_SHORT, store.baseQuadVertexIndex() * 3);
		}
	}

	public void add(ClusteredDrawableStorage storage) {
		assert storage.getCluster() == cluster;
		stores.add(storage);
	}
}
