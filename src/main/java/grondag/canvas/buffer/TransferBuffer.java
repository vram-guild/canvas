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

import grondag.canvas.buffer.util.BinIndex;

public interface TransferBuffer {
	int sizeBytes();

	void put(int[] source, int sourceStart, int targetStart, int length);

	/** MUST be called if one of other release methods isn't. ALWAYS returns null. */
	@Nullable
	TransferBuffer release();

	@Nullable
	TransferBuffer releaseToBoundBuffer(int target, int targetStartBytes);

	static TransferBuffer claim(int byteSize) {
		if (RenderSystem.isOnRenderThread()) {
			return MappedTransferBuffer.RENDER_THREAD_ALLOCATOR.claim(byteSize);
		} else {
			final MappedTransferBuffer result = MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.claim(byteSize);
			return result == null ? OffHeapTransferBuffer.THREAD_SAFE_ALLOCATOR.claim(byteSize) : result;
		}
	}

	static void forceReload() {
		assert RenderSystem.isOnRenderThread();
		MappedTransferBuffer.RENDER_THREAD_ALLOCATOR.forceReload();
		MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.forceReload();
		OffHeapTransferBuffer.THREAD_SAFE_ALLOCATOR.forceReload();
	}

	static void update() {
		assert RenderSystem.isOnRenderThread();

		MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.forecastUnmetDemand();

		for (int i = 0; i < BinIndex.BIN_COUNT; ++i) {
			final BinIndex bin = BinIndex.fromIndex(i);
			final int demand = MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.unmetDemandForecast(bin);

			if (demand > 0) {
				for (int j = 0; j < demand; ++j) {
					MappedTransferBuffer buff = MappedTransferBuffer.RENDER_THREAD_ALLOCATOR.take(bin);
					buff.prepareForOffThreadUse();
					MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.put(buff);
				}
			}
		}
	}

	static String debugString() {
		return String.format("Peak mapped xfer buffers:%5.1fMb", (double) MappedTransferBuffer.THREAD_SAFE_ALLOCATOR.totalPeakDemandBytes() / 0x100000);
	}
}
