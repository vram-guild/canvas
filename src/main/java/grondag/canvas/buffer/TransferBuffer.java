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

import org.jetbrains.annotations.Nullable;

public interface TransferBuffer {
	boolean isMappedBuffer();

	/**
	 * Binds the buffer for direct rendering. Will remain bound until
	 * bind is called again (call with 0 to unbind) or until {@link #release()} is called.
	 *
	 * <p>Reference must be held until operation is complete!.
	 */
	default void bind(int target) {
		throw new UnsupportedOperationException();
	}

	/** For copying to mapped buffers. ALWAYS returns null. */
	@Nullable
	TransferBuffer releaseToMappedBuffer(ByteBuffer targetBuffer, int targetOffset, int sourceOffset, int byteCount);

	/** For copying to a bound buffer. ALWAYS returns null. */
	@Nullable
	TransferBuffer releaseToSubBuffer(int target, int targetAddress, int byteCount);

	/** For populating new buffers. ALWAYS returns null. */
	@Nullable
	TransferBuffer releaseToBuffer(int target, int usage);

	/** For writing only. */
	IntBuffer asIntBuffer();

	/** MUST be called if one of other release methods isn't. ALWAYS returns null. */
	@Nullable
	TransferBuffer release();
}
