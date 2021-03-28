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
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL46C;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.varia.CanvasGlHelper;

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

	public static void unbind() {
		BindStateManager.unbind();
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		final ByteBuffer uploadBuffer = this.uploadBuffer;

		if (uploadBuffer != null) {
			uploadBuffer.rewind();
			BindStateManager.bind(glBufferId());
			GL21.glBufferData(GL21.GL_ARRAY_BUFFER, uploadBuffer, GL21.GL_STATIC_DRAW);
			BindStateManager.unbind();
			TransferBufferAllocator.release(uploadBuffer);
			this.uploadBuffer = null;
		}
	}

	private int glBufferId() {
		int result = glBufferId;

		if (result == -1) {
			assert RenderSystem.isOnGameThread();
			result = GlBufferAllocator.claimBuffer(byteCount);

			assert result > 0;

			glBufferId = result;
		}

		return result;
	}

	public void bind() {
		assert CanvasGlHelper.checkError();

		final CanvasVertexFormat format = this.format;

		if (vaoBufferId == VAO_NONE) {
			// Important this happens BEFORE anything that could affect vertex state
			GL46C.glBindVertexArray(0);
			assert CanvasGlHelper.checkError();

			BindStateManager.bind(glBufferId());

			vaoBufferId = VaoAllocator.claimVertexArray();
			GL46C.glBindVertexArray(vaoBufferId);
			assert CanvasGlHelper.checkError();

			CanvasGlHelper.enableAttributesVao(format.attributeCount());
			format.bindAttributeLocations(0);
		} else {
			GL46C.glBindVertexArray(vaoBufferId);
			assert CanvasGlHelper.checkError();
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

			if (vaoBufferId > 0) {
				VaoAllocator.releaseVertexArray(vaoBufferId);
				vaoBufferId = VAO_NONE;
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
