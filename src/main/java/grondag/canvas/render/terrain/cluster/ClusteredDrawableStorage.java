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

package grondag.canvas.render.terrain.cluster;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.input.FaceBucket;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.UploadableVertexStorage;
import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation;
import grondag.canvas.terrain.region.RegionPosition;

public class ClusteredDrawableStorage implements UploadableVertexStorage {
	public final VertexClusterRealm realm;
	public final int byteCount;
	public final int quadVertexCount;
	public final int triVertexCount;
	public final long clusterPos;
	public final RegionPosition regionOrigin;
	public final FaceBucket[] faceBuckets;

	private TransferBuffer transferBuffer;
	private boolean isClosed = false;
	private RegionAllocation allocation = null;

	public ClusteredDrawableStorage(VertexClusterRealm owner, TransferBuffer transferBuffer, int byteCount, RegionPosition regionOrigin, int quadVertexCount, FaceBucket[] buckets) {
		realm = owner;
		this.transferBuffer = transferBuffer;
		this.byteCount = byteCount;
		this.quadVertexCount = quadVertexCount;
		faceBuckets = buckets;
		this.regionOrigin = regionOrigin;
		triVertexCount = quadVertexCount / 4 * 6;
		clusterPos = VertexClusterRealm.clusterPos(regionOrigin.asLong());
	}

	TransferBuffer getAndClearTransferBuffer() {
		TransferBuffer result = transferBuffer;
		transferBuffer = null;
		return result;
	}

	@Override
	public ClusteredDrawableStorage release() {
		close();
		return null;
	}

	public void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			if (transferBuffer != null) {
				transferBuffer = transferBuffer.release();
			}

			if (allocation != null) {
				allocation.onRegionClosed();
				allocation = null;
			}
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

	public RegionAllocation allocation() {
		return allocation;
	}

	@Override
	public void upload() {
		assert allocation == null;
		allocation = realm.allocate(this);
	}

	/** Flag 6 (unassigned) will always be set. */
	public int visibleFaceFlags() {
		assert faceBuckets != null : "bucket flags requested when buckets not present";
		return regionOrigin.visibleFaceFlags();
	}

	/** Flag 6 (unassigned) will always be set. */
	public int shadowVisibleFaceFlags() {
		assert faceBuckets != null : "bucket flags requested when buckets not present";
		return regionOrigin.shadowVisibleFaceFlags();
	}
}
