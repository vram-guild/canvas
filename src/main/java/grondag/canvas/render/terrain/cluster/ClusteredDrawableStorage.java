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

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.input.VertexBucket;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.UploadableVertexStorage;
import grondag.canvas.render.terrain.cluster.VertexCluster.RegionAllocation;

public class ClusteredDrawableStorage implements UploadableVertexStorage {
	public final VertexClusterRealm realm;
	public final int byteCount;
	public final int quadVertexCount;
	public final int triVertexCount;
	public final long clusterPos;
	public final long packedOriginBlockPos;
	public final VertexBucket[] buckets;

	private TransferBuffer transferBuffer;
	private boolean isClosed = false;
	private RegionAllocation allocation = null;

	public ClusteredDrawableStorage(VertexClusterRealm owner, TransferBuffer transferBuffer, int byteCount, long packedOriginBlockPos, int quadVertexCount, VertexBucket[] buckets) {
		realm = owner;
		this.transferBuffer = transferBuffer;
		this.byteCount = byteCount;
		this.quadVertexCount = quadVertexCount;
		this.buckets = buckets;
		this.packedOriginBlockPos = packedOriginBlockPos;
		triVertexCount = quadVertexCount / 4 * 6;
		clusterPos = VertexClusterRealm.clusterPos(packedOriginBlockPos);
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
	public int bucketFlags() {
		assert buckets != null : "bucket flags requested when buckets not present";
		return realm.drawListCullingHelper.computeFlags(packedOriginBlockPos);
	}
}
