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

package grondag.canvas.render.region.vs;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.render.region.DrawableStorage;

public class ClumpedDrawableStorage implements DrawableStorage {
	private ByteBuffer transferBuffer;
	final int byteCount;
	final int triVertexCount;
	private int baseVertex;
	@SuppressWarnings("unused")
	private long packedOriginBlockPos; // WIP: remove this later, only useful for debug
	private boolean isClosed = false;
	final long clumpPos;
	private ClumpedVertexStorageClump clump = null;

	public ClumpedDrawableStorage(ByteBuffer transferBuffer, int byteCount, long packedOriginBlockPos, int triVertexCount) {
		this.transferBuffer = transferBuffer;
		this.byteCount = byteCount;
		this.packedOriginBlockPos = packedOriginBlockPos;
		this.triVertexCount = triVertexCount;
		clumpPos = ClumpedVertexStorage.clumpPos(packedOriginBlockPos);
	}

	ByteBuffer getAndClearTransferBuffer() {
		ByteBuffer result = transferBuffer;
		transferBuffer = null;
		return result;
	}

	@Override
	public void close() {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			if (transferBuffer != null) {
				TransferBufferAllocator.release(transferBuffer);
				transferBuffer = null;
			}

			if (clump != null) {
				clump.notifyClosed(this);
				clump = null;
			}
		}
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	/**
	 * Controlled by storage so that the vertices can be moved around as
	 * needed to control fragmentation without external entanglements.
	 */
	public int baseVertex() {
		return baseVertex;
	}

	public int baseByteAddress() {
		return baseVertex * VsFormat.VS_MATERIAL.vertexStrideBytes;
	}

	void setBaseVertex(int baseVertex) {
		this.baseVertex = baseVertex;
	}

	void setClump(ClumpedVertexStorageClump clump) {
		assert this.clump == null;
		assert clump != null;
		this.clump = clump;
	}

	ClumpedVertexStorageClump getClump() {
		return clump;
	}

	@Override
	public void upload() {
		ClumpedVertexStorage.INSTANCE.allocate(this);
	}
}
