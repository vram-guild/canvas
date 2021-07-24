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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.UploadableVertexStorage;

public class ClusteredDrawableStorage implements UploadableVertexStorage {
	public final VertexClusterRealm owner;
	public final int byteCount;
	public final int quadVertexCount;
	public final int triVertexCount;
	public final long clusterPos;

	private TransferBuffer transferBuffer;
	private ObjectArrayList<SlabAllocation> allocations = new ObjectArrayList<>();
	private boolean isClosed = false;
	private VertexCluster cluster = null;

	public ClusteredDrawableStorage(VertexClusterRealm owner, TransferBuffer transferBuffer, int byteCount, long packedOriginBlockPos, int quadVertexCount) {
		this.owner = owner;
		this.transferBuffer = transferBuffer;
		this.byteCount = byteCount;
		this.quadVertexCount = quadVertexCount;
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
		close(true);
		return null;
	}

	public void close(boolean notify) {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			if (transferBuffer != null) {
				transferBuffer = transferBuffer.release();
			}

			if (allocations != null) {
				for (var allocation : allocations) {
					// Allocations are nullified after notifying cluster so
					// that cluster can do slab accounting.
					allocation.close();
				}

				if (notify) {
					assert cluster != null;
					cluster.notifyClosed(this);
				}
			}

			cluster = null;
			allocations.clear();
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

	/**
	 * Returns prior allocations and clears current.
	 */
	public ObjectArrayList<SlabAllocation> prepareForReallocation() {
		assert !allocations.isEmpty();
		final var result = allocations;
		allocations = new ObjectArrayList<>();
		return result;
	}

	/**
	 * Controlled by storage so that the vertices can be moved around as
	 * needed to control fragmentation without external entanglements.
	 */
	public ObjectArrayList<SlabAllocation> allocations() {
		assert !allocations.isEmpty() : "Slab allocations queried before allocation";
		return allocations;
	}

	void addAllocation(SlabAllocation allocation) {
		allocations.add(allocation);
		//assert cluster.isPresent(this);
	}

	void setCluster(VertexCluster cluster) {
		assert allocations.isEmpty();
		assert this.cluster == null;
		assert cluster != null;
		this.cluster = cluster;
	}

	public VertexCluster getCluster() {
		//assert clump.isPresent(this);
		return cluster;
	}

	@Override
	public void upload() {
		assert allocations.isEmpty();
		owner.allocate(this);
	}
}
