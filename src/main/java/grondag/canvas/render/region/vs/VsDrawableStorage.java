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

import grondag.canvas.buffer.GlBufferAllocator;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.render.region.DrawableStorage;
import grondag.canvas.varia.GFX;

public class VsDrawableStorage implements DrawableStorage {
	private ByteBuffer transferBuffer;
	private static final int VAO_NONE = -1;
	final int byteCount;
	private int baseVertex;

	private int glBufferId = -1;
	private boolean isClosed = false;
	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = VAO_NONE;

	public VsDrawableStorage(ByteBuffer transferBuffer, int byteCount) {
		this.transferBuffer = transferBuffer;
		this.byteCount = byteCount;
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

			if (vaoBufferId > 0) {
				GFX.deleteVertexArray(vaoBufferId);
				vaoBufferId = VAO_NONE;
			}

			final int glBufferId = this.glBufferId;

			if (glBufferId != -1) {
				GlBufferAllocator.releaseBuffer(glBufferId, byteCount);
				this.glBufferId = -1;
			}

			if (transferBuffer != null) {
				TransferBufferAllocator.release(transferBuffer);
				transferBuffer = null;
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

	void setBaseVertex(int baseVertex) {
		this.baseVertex = baseVertex;
	}

	void upload() {
		VsVertexStorage.INSTANCE.allocate(this);
	}
}
