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

import java.nio.IntBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class BufferDelegate {
	private static final ArrayBlockingQueue<BufferDelegate> POOL = new ArrayBlockingQueue<>(4096);

	public static BufferDelegate claim(AbstractBuffer buffer, int byteOffset, int byteCount) {
		BufferDelegate result = POOL.poll();
		if (result == null) {
			result = new BufferDelegate();
		}
		return result.prepare(buffer, byteOffset, byteCount);
	}

	private int byteCount;
	private int byteOffset;
	private AbstractBuffer buffer;

	private BufferDelegate() {
	}

	private BufferDelegate prepare(AbstractBuffer buffer, int byteOffset, int byteCount) {
		this.buffer = buffer;
		this.byteCount = byteCount;
		this.byteOffset = byteOffset;
		return this;
	}

	public void release() {
		POOL.offer(this);
	}

	/**
	 * How many bytes consumed by this delegate in the buffer.
	 */
	public final int byteCount() {
		return byteCount;
	}

	/**
	 * Start of this delegate's bytes in the buffer.
	 */
	public final int byteOffset() {
		return byteOffset;
	}

	/** chunk will populate this buffer with vertex data. Will be used off thread. */
	public final IntBuffer intBuffer() {
		return buffer.byteBuffer().asIntBuffer();
	}

	public AbstractBuffer buffer() {
		return buffer;
	}
}
