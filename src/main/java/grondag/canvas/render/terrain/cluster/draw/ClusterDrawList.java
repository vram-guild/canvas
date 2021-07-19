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

package grondag.canvas.render.terrain.cluster.draw;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.render.VertexFormat.DrawMode;

import grondag.canvas.render.terrain.cluster.ClusteredDrawableStorage;
import grondag.canvas.render.terrain.cluster.VertexCluster;
import grondag.canvas.varia.GFX;

public class ClusterDrawList {
	private final ObjectArrayList<ClusteredDrawableStorage> stores = new ObjectArrayList<>();
	private final VertexCluster cluster;

	ClusterDrawList(VertexCluster cluster) {
		this.cluster = cluster;
	}

	public void draw(int elementType, int indexBufferId) {
		final int limit = stores.size();

		cluster.bind();
		GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			ClusteredDrawableStorage store = stores.get(regionIndex);
			GFX.drawElementsBaseVertex(DrawMode.QUADS.mode, store.triVertexCount, elementType, 0L, store.baseVertex());
		}
	}

	public void add(ClusteredDrawableStorage storage) {
		assert storage.getCluster() == cluster;
		stores.add(storage);
	}
}
