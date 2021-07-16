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

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.util.GlBufferAllocator;
import grondag.canvas.varia.GFX;

abstract class AbstractDrawBuffer {
	protected CanvasVertexFormat format;
	protected int glBufferId = 0;
	protected final int byteCount;
	protected boolean isClosed = false;

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	protected int vaoBufferId = 0;

	protected AbstractDrawBuffer(int bytes) {
		byteCount = bytes;
		glBufferId = GlBufferAllocator.claimBuffer(byteCount);
	}

	public void bind() {
		final CanvasVertexFormat format = this.format;

		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);

			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, glBufferId);
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

	public final void close() {
		if (RenderSystem.isOnRenderThread()) {
			onClose();
		} else {
			RenderSystem.recordRenderCall(this::excuteClose);
		}
	}

	protected final void excuteClose() {
		if (!isClosed) {
			isClosed = true;

			onClose();

			if (vaoBufferId != 0) {
				GFX.deleteVertexArray(vaoBufferId);
				vaoBufferId = 0;
			}

			final int glBufferId = this.glBufferId;

			if (glBufferId != 0) {
				GlBufferAllocator.releaseBuffer(glBufferId, byteCount);
				this.glBufferId = 0;
			}
		}
	}

	protected abstract void onClose();
}
