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
	private final int byteCount;

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

	@Override
	public void close() {
		if (RenderSystem.isOnRenderThread()) {
			onClose();
		} else {
			RenderSystem.recordRenderCall(this::onClose);
		}
	}

	private void onClose() {
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

	public void bind() {
		// WIP - temporary

		if (vaoBufferId == VAO_NONE) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			VsFormat.VS_MATERIAL.enableAttributes();
			VsFormat.VS_MATERIAL.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	public void upload() {
		if (transferBuffer != null) {
			transferBuffer.rewind();
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			GFX.bufferData(GFX.GL_ARRAY_BUFFER, transferBuffer, GFX.GL_STATIC_DRAW);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
			TransferBufferAllocator.release(transferBuffer);
			transferBuffer = null;
		}
	}

	private int glBufferId() {
		int result = glBufferId;

		if (result == -1) {
			result = GlBufferAllocator.claimBuffer(byteCount);

			assert result > 0;

			glBufferId = result;
		}

		return result;
	}

	public int baseVertex() {
		return 0;
	}
}
