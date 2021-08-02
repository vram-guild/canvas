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

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.base.AbstractDrawableRegionList;
import grondag.canvas.render.terrain.base.DrawableRegion;
import grondag.canvas.render.terrain.base.DrawableRegionList;
import grondag.canvas.render.terrain.cluster.ClusteredDrawableRegion;
import grondag.canvas.render.terrain.cluster.ClusteredDrawableStorage;
import grondag.canvas.varia.GFX;

public class RealmDrawList extends AbstractDrawableRegionList {
	final ObjectArrayList<ClusterDrawList> clusterLists = new ObjectArrayList<>();
	final boolean isShadowMap;

	boolean isInvalid = false;

	private RealmDrawList(final ObjectArrayList<DrawableRegion> regions, RenderState renderState, boolean isShadowMap) {
		super(regions, renderState);
		this.isShadowMap = isShadowMap;
		build();
	}

	private void build() {
		final Long2ObjectOpenHashMap<ClusterDrawList> map = new Long2ObjectOpenHashMap<>();
		final int limit = regions.size();

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			final ClusteredDrawableStorage storage = ((ClusteredDrawableRegion) regions.get(regionIndex)).storage();

			ClusterDrawList clusterList = map.get(storage.clusterPos);

			if (clusterList == null) {
				clusterList = new ClusterDrawList(storage.allocation().cluster(), this);
				clusterLists.add(clusterList);
				map.put(storage.clusterPos, clusterList);
			}

			clusterList.add(storage);
		}

		clusterLists.forEach(cl -> cl.build());
	}

	private void rebuildIfInvalid() {
		if (isInvalid) {
			// Rarely happens because slab reallocation typically happen
			// in response to player movement, which will naturally force
			// a new draw list to be created.
			isInvalid = false;
			closeInner();
			build();
		}
	}

	public static DrawableRegionList build(final ObjectArrayList<DrawableRegion> regions, RenderState renderState, boolean isShadowMap) {
		return regions.isEmpty() ? DrawableRegionList.EMPTY : new RealmDrawList(regions, renderState, isShadowMap);
	}

	@Override
	public void draw() {
		rebuildIfInvalid();
		renderState.enable(0, 0, 0, 0, 0);
		final int limit = clusterLists.size();
		GFX.bindVertexArray(0);

		for (int i = 0; i < limit; ++i) {
			clusterLists.get(i).draw();
		}

		GFX.bindVertexArray(0);
		GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, 0);
		RenderState.disable();
	}

	@Override
	protected void closeInner() {
		clusterLists.forEach(ClusterDrawList::release);
		clusterLists.clear();
	}

	void invalidate() {
		isInvalid = true;
	}
}
