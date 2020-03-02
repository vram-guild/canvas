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

public class BufferDelegate {
	private final int byteCount;
	private final int byteOffset;
	private final VboBuffer buffer;

	public BufferDelegate (VboBuffer buffer, int byteOffset, int byteCount) {
		this.buffer = buffer;
		this.byteCount = byteCount;
		this.byteOffset = byteOffset;
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

	public VboBuffer buffer() {
		return buffer;
	}
}
