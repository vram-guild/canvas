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

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.base.AbstractDrawableRegionList;
import grondag.canvas.render.terrain.base.DrawableRegion;
import grondag.canvas.render.terrain.base.DrawableRegionList;
import grondag.canvas.render.terrain.cluster.ClusteredDrawableRegion;
import grondag.canvas.render.terrain.cluster.ClusteredDrawableStorage;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.varia.GFX;

public class RealmDrawList extends AbstractDrawableRegionList {
	final ObjectArrayList<ClusterDrawList> clusterLists = new ObjectArrayList<>();
	final boolean isShadowMap;
	private int quadCount;

	boolean isInvalid = false;

	private RealmDrawList(final ObjectArrayList<DrawableRegion> regions, RenderState renderState, boolean isShadowMap) {
		super(regions, renderState);
		this.isShadowMap = isShadowMap;
		build();
	}

	@Override
	public int quadCount() {
		return quadCount;
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

		quadCount = 0;

		clusterLists.forEach(cl -> {
			cl.build();
			quadCount += cl.quadCount();
		});
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
	public void draw(WorldRenderState worldRenderState) {
		rebuildIfInvalid();
		final var sectorManager = worldRenderState.sectorManager;
		renderState.enable(sectorManager.originBlockX(), 0, sectorManager.originBlockZ());
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
