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

import grondag.canvas.draw.DrawableDelegate;

public abstract class AbstractBuffer {
	public abstract ByteBuffer byteBuffer();

	public abstract boolean isVbo();

	public BindableBuffer bindable() {
		return DummyBindableBuffer.INSTANCE;
	}

	/**
	 * Uploads or flushes to GPU, depending on the type of buffer. (Or may do nothing.)
	 * Always called from main thread.
	 */
	public void upload() {
	}

	/** called before chunk populates int buffer(). May be called off thread */
	public void lockForWrite() {

	}

	/** called after chunk populates int buffer(). May be called off thread */
	public void unlockForWrite() {
	}

	/**
	 * Signals the buffer is in use. May be called off-thread.
	 *
	 * Called implicitly when bytes are allocated. Store calls explicitly to retain
	 * while this buffer is being filled.
	 */
	public void retain(DrawableDelegate drawable) {
	}

	/**
	 * Signals the buffer will no longer be used. May be called off-thread.
	 */
	public void release(DrawableDelegate drawable) {
	}

	/**
	 * True if buffer has been fully released and recycled.  Disposed buffers cannot be used.
	 */
	public boolean isDisposed() {
		return false;
	}
}
