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

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.base.AbstractDrawableRegionList;
import grondag.canvas.render.terrain.base.DrawableRegion;
import grondag.canvas.render.terrain.base.DrawableRegionList;
import grondag.canvas.varia.GFX;

public class DrawListRealm extends AbstractDrawableRegionList {
	final ObjectArrayList<DrawListCluster> clusterLists = new ObjectArrayList<>();

	private DrawListRealm(final ObjectArrayList<DrawableRegion> regions, RenderState renderState) {
		super(regions, renderState);

		final Long2ObjectOpenHashMap<DrawListCluster> map = new Long2ObjectOpenHashMap<>();
		final int limit = regions.size();

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			final ClusteredDrawableStorage storage = ((ClusteredDrawableRegion) regions.get(regionIndex)).storage();

			DrawListCluster clusterList = map.get(storage.clusterPos);

			if (clusterList == null) {
				clusterList = new DrawListCluster(storage.getCluster());
				clusterLists.add(clusterList);
			}

			clusterList.add(storage);
		}
	}

	public static DrawableRegionList build(final ObjectArrayList<DrawableRegion> regions, RenderState renderState) {
		return regions.isEmpty() ? DrawableRegionList.EMPTY : new DrawListRealm(regions, renderState);
	}

	@Override
	public void draw() {
		renderState.enable(0, 0, 0, 0, 0);
		final int limit = clusterLists.size();

		for (int i = 0; i < limit; ++i) {
			clusterLists.get(i).draw();
		}

		GFX.bindVertexArray(0);
		RenderState.disable();
	}

	@Override
	protected void closeInner() {
		// NOOP
	}
}
