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
import org.jetbrains.annotations.Nullable;

public interface TransferBuffer {
	int sizeBytes();

	void put(int[] source, int sourceStart, int targetStart, int length);

	/** MUST be called if one of other release methods isn't. ALWAYS returns null. */
	@Nullable
	TransferBuffer release();

	@Nullable
	TransferBuffer releaseToBoundBuffer(int target, int targetStartBytes);

	static TransferBuffer claim(int byteCount) {
		if (RenderSystem.isOnRenderThread()) {
			return MappedTransferBuffer.claim(byteCount);
		} else {
			return ArrayTransferBuffer.claim(byteCount);
		}
	}
}
