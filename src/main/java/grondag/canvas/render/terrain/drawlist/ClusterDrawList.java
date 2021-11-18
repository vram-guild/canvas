/*
 * Copyright © Original Authors
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

import java.util.IdentityHashMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.config.Configurator;
import grondag.canvas.render.terrain.cluster.ClusteredDrawableStorage;
import grondag.canvas.render.terrain.cluster.Slab;
import grondag.canvas.render.terrain.cluster.VertexCluster;
import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation.SlabAllocation;
import grondag.canvas.varia.GFX;

public class ClusterDrawList {
	final ObjectArrayList<ClusteredDrawableStorage> regions = new ObjectArrayList<>();
	final VertexCluster cluster;
	final RealmDrawList owner;
	private final ObjectArrayList<DrawSpec> drawSpecs = new ObjectArrayList<>();
	private int quadCount;

	ClusterDrawList(VertexCluster cluster, RealmDrawList owner) {
		this.cluster = cluster;
		this.owner = owner;
	}

	public int quadCount() {
		return quadCount;
	}

	void build() {
		assert drawSpecs.isEmpty();

		if (cluster.realm.isTranslucent) {
			buildTranslucent();
		} else {
			buildSolid();
		}
	}

	/** Maintains region sort order at the cost of extra binds/calls if needed. */
	private void buildTranslucent() {
		Slab lastSlab = null;
		final ObjectArrayList<SlabAllocation> specAllocations = new ObjectArrayList<>();
		int quadCount = 0;

		for (final var region : regions) {
			final var alloc = region.allocation().getAllocation();

			if (alloc.slab != lastSlab) {
				// NB: builder checks for empty region list (will be true for first region)
				// and also clears the list when done.
				quadCount += DrawSpecBuilder.build(specAllocations, drawSpecs, false, false);
				lastSlab = alloc.slab;
			}

			specAllocations.add(alloc);
		}

		quadCount += DrawSpecBuilder.build(specAllocations, drawSpecs, false, false);

		this.quadCount = quadCount;
	}

	/** Minimizes binds/calls. */
	private void buildSolid() {
		final IdentityHashMap<Slab, ObjectArrayList<SlabAllocation>> map = new IdentityHashMap<>();

		// first group regions by slab
		for (final var region : regions) {
			final var alloc = region.allocation().getAllocation();
			var list = map.get(alloc.slab);

			if (list == null) {
				list = new ObjectArrayList<>();
				map.put(alloc.slab, list);
			}

			list.add(alloc);
		}

		int quadCount = 0;

		for (final var list: map.values()) {
			quadCount += DrawSpecBuilder.build(list, drawSpecs, owner.isShadowMap, Configurator.cullBackfacingTerrain);
		}

		this.quadCount = quadCount;
	}

	public void draw() {
		final int limit = drawSpecs.size();

		for (int i = 0; i < limit; ++i) {
			final var spec = drawSpecs.get(i);
			spec.bind();
			GFX.glMultiDrawElementsBaseVertex(GFX.GL_TRIANGLES, spec.triVertexCount(), GFX.GL_UNSIGNED_SHORT, spec.triIndexOffset(), spec.baseQuadVertexOffset());
		}
	}

	public void add(ClusteredDrawableStorage region) {
		assert region.allocation().cluster() == cluster;
		regions.add(region);
	}

	public void invalidate() {
		owner.invalidate();
	}

	public void release() {
		drawSpecs.forEach(DrawSpec::release);
	}
}
