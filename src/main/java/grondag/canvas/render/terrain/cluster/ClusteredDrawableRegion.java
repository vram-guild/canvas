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

package grondag.canvas.render.terrain.cluster;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.VertexBucket;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.render.terrain.base.AbstractDrawableRegion;
import grondag.canvas.render.terrain.base.DrawableRegion;
import grondag.canvas.render.terrain.base.UploadableRegion;
import grondag.canvas.terrain.region.RegionPosition;

public class ClusteredDrawableRegion extends AbstractDrawableRegion<ClusteredDrawableStorage> {
	private ClusteredDrawableRegion(int quadVertexCount, ClusteredDrawableStorage storage) {
		super(quadVertexCount, storage);
	}

	public static UploadableRegion uploadable(VertexCollectorList collectorList, VertexClusterRealm realm, int byteCount, RegionPosition origin) {
		final boolean translucent = realm.isTranslucent;
		final ObjectArrayList<ArrayVertexCollector> drawList = collectorList.sortedDrawList(translucent ? TerrainRenderStates.TRANSLUCENT_PREDICATE : TerrainRenderStates.SOLID_PREDICATE);

		if (drawList.isEmpty()) {
			return UploadableRegion.EMPTY_UPLOADABLE;
		}

		final ArrayVertexCollector collector = drawList.get(0);

		// WIP: restore ability to have more than one pass in non-translucent terrain, for decals, etc.
		// Note that every render state/pass will have a separate storage and storage will control
		// the vertex offset for each.  The calls won't be batched by region so there's no advantage to
		// making them adjacent in storage and smaller allocations may be easier to manage for storage.
		assert drawList.size() == 1;
		assert collector.sorted == translucent;

		final TransferBuffer transferBuffer = TransferBuffers.claim(byteCount);
		final VertexBucket[] buckets = translucent ? null : collector.sortVertexBuckets();
		collector.toBuffer(0, transferBuffer, 0);
		final ClusteredDrawableStorage storage = new ClusteredDrawableStorage(
				realm,
				transferBuffer, byteCount, origin, collector.quadCount() * 4,
				buckets);

		return new ClusteredDrawableRegion(collector.quadCount() * 4, storage);
	}

	@Override
	public DrawableRegion produceDrawable() {
		storage().upload();
		return this;
	}

	@Override
	protected void closeInner() {
		// NOOP
	}
}
