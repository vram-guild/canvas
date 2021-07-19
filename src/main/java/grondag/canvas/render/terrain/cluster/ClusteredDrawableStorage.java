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

import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.UploadableVertexStorage;

public class ClusteredDrawableStorage implements UploadableVertexStorage {
	private static final int NOT_ALLOCATED = -1;

	public final VertexClusterRealm owner;
	public final int byteCount;
	public final int quadVertexCount;
	public final int triVertexCount;
	public final long clusterPos;

	private TransferBuffer transferBuffer;
	private Slab slab;
	private int baseQuadVertexIndex = NOT_ALLOCATED;
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

			if (slab != null) {
				slab.removeRegion(this);
				slab = null;
			}

			if (cluster != null) {
				if (notify) {
					cluster.notifyClosed(this);
				}

				cluster = null;
			}

			baseQuadVertexIndex = NOT_ALLOCATED;
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

	/**
	 * Controlled by storage so that the vertices can be moved around as
	 * needed to control fragmentation without external entanglements.
	 */
	public int baseQuadVertexIndex() {
		assert baseQuadVertexIndex != NOT_ALLOCATED;

		return baseQuadVertexIndex;
	}

	public Slab slab() {
		return slab;
	}

	void setLocation(Slab slab, int baseQuadVertexIndex) {
		this.slab = slab;
		this.baseQuadVertexIndex = baseQuadVertexIndex;
		//assert cluster.isPresent(this);
	}

	void setCluster(VertexCluster cluster) {
		assert baseQuadVertexIndex == NOT_ALLOCATED;
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
		assert baseQuadVertexIndex == NOT_ALLOCATED;
		owner.allocate(this);
	}
}
