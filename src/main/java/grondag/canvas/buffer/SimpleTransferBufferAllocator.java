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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import grondag.canvas.config.Configurator;

/**
 * Tracks all allocations, ensures deallocation on render reload.
 * Implements configuration of allocation method.
 */
class SimpleTransferBufferAllocator {
	private static final Set<SimpleTransferBuffer> OPEN = Collections.newSetFromMap(new IdentityHashMap<SimpleTransferBuffer, Boolean>());
	private static int allocatedBytes = 0;
	private static int peakBytes = 0;
	private static int peakSize = 0;
	private static int zeroCount = 0;

	static synchronized TransferBuffer claim(int bytes) {
		SimpleTransferBuffer result = new SimpleTransferBuffer(bytes);
		allocatedBytes += result.capacityBytes;
		OPEN.add(result);
		return result;
	}

	static synchronized void recordRelease(SimpleTransferBuffer buffer) {
		if (OPEN.remove(buffer)) {
			allocatedBytes -= buffer.capacityBytes;
		}
	}

	static synchronized void forceReload() {
		OPEN.forEach(b -> b.releaseWithoutNotify());
		OPEN.clear();
		allocatedBytes = 0;
	}

	static String debugString() {
		final int size = OPEN.size();

		if (size == 0 && ++zeroCount >= 10) {
			peakBytes = 0;
			peakSize = 0;
			zeroCount = 0;
		} else {
			if (allocatedBytes > peakBytes) {
				peakBytes = allocatedBytes;
			}

			if (size > peakSize) {
				peakSize = size;
			}
		}

		return String.format("Peak transfer buffers: %03d @ %03dMB - %s mode", peakSize, peakBytes / 0x100000,
			Configurator.safeNativeMemoryAllocation ? "safe" : "fast");
	}
}
