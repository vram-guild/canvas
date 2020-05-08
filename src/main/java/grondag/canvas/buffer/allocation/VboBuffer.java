/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.buffer.allocation;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import grondag.canvas.varia.GLBufferStore;

public class VboBuffer implements AllocationProvider, AutoCloseable {
	ByteBuffer uploadBuffer;

	int byteOffset = 0;

	private int glBufferId = -1;

	private boolean isClosed = false;

	public VboBuffer(int bytes) {
		// TODO: get rid of BufferAllocator if it won't be faster
		uploadBuffer = MemoryUtil.memAlloc(bytes); //BufferUtils.createByteBuffer(bytes); //BufferAllocator.claim(bytes);
	}

	public void upload() {
		assert RenderSystem.isOnRenderThread();

		final ByteBuffer uploadBuffer = this.uploadBuffer;

		if(uploadBuffer != null) {
			bind();
			uploadBuffer.rewind();
			GL21.glBufferData(GL21.GL_ARRAY_BUFFER, uploadBuffer, GL21.GL_STATIC_DRAW);
			unbind();
			MemoryUtil.memFree(uploadBuffer);
			//BufferAllocator.release(uploadBuffer);
			this.uploadBuffer = null;
		}
	}

	public ByteBuffer byteBuffer() {
		return uploadBuffer;
	}

	@Override
	public void claimAllocation(int byteCount, Consumer<BufferDelegate> consumer) {
		consumer.accept(new BufferDelegate(this, byteOffset, byteCount));
		byteOffset += byteCount;
	}

	public int glBufferId() {
		int result = glBufferId;

		if(result == -1) {
			assert RenderSystem.isOnGameThread();
			result = GLBufferStore.claimBuffer();

			assert result > 0;

			glBufferId = result;
		}

		return result;
	}

	/**
	 * @return true if bound buffer changed
	 */
	public boolean bind() {
		assert RenderSystem.isOnRenderThread();

		final int glBufferId = glBufferId();

		return BindStateManager.bind(glBufferId);
	}

	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public void close() throws Exception {
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

			if(glBufferId != -1) {
				GLBufferStore.releaseBuffer(glBufferId);
				this.glBufferId = -1;
			}

			final ByteBuffer uploadBuffer = this.uploadBuffer;

			if(uploadBuffer != null) {
				MemoryUtil.memFree(uploadBuffer);
				//BufferUtils.BufferAllocator.release(uploadBuffer);
				this.uploadBuffer = null;
			}
		}
	}

	public static void unbind() {
		BindStateManager.unbind();
	}
}
