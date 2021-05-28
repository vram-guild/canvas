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

package grondag.canvas.buffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.varia.GFX;

public class VboBuffer {
	private static final int VAO_NONE = -1;
	public final CanvasVertexFormat format;
	private final int byteCount;
	ByteBuffer uploadBuffer;
	private int glBufferId = -1;
	private boolean isClosed = false;
	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = VAO_NONE;

	public VboBuffer(int bytes, CanvasVertexFormat format) {
		uploadBuffer = TransferBufferAllocator.claim(bytes);
		this.format = format;
		byteCount = bytes;
	}

	public void upload() {
		final ByteBuffer uploadBuffer = this.uploadBuffer;

		if (uploadBuffer != null) {
			uploadBuffer.rewind();
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			GFX.bufferData(GFX.GL_ARRAY_BUFFER, uploadBuffer, GFX.GL_STATIC_DRAW);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
			TransferBufferAllocator.release(uploadBuffer);
			this.uploadBuffer = null;
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

	public void bind() {
		final CanvasVertexFormat format = this.format;

		if (vaoBufferId == VAO_NONE) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId());
			format.enableAttributes();
			format.bindAttributeLocations(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

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

			final ByteBuffer uploadBuffer = this.uploadBuffer;

			if (uploadBuffer != null) {
				TransferBufferAllocator.release(uploadBuffer);
				this.uploadBuffer = null;
			}
		}
	}

	public IntBuffer intBuffer() {
		return uploadBuffer.asIntBuffer();
	}

	@FunctionalInterface
	private interface VertexBinder {
		void bind();
	}
}
