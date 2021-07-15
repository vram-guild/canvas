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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import grondag.canvas.CanvasMod;

/**
 * Tracks all allocations, ensures deallocation on render reload.
 * Implements configuration of allocation method.
 */
class SimpleTransferBufferAllocator {
	static class AllocationState {
		private final Set<SimpleTransferBuffer> open = Collections.newSetFromMap(new ConcurrentHashMap<SimpleTransferBuffer, Boolean>());

		void add(SimpleTransferBuffer buffer) {
			open.add(buffer);
		}

		void remove(SimpleTransferBuffer buffer) {
			if (!open.remove(buffer)) {
				CanvasMod.LOG.warn("Transfer buffer not found on removal");
			}
		}

		void clear() {
			open.forEach(b -> b.releaseWithoutNotify());
		}
	}

	private static final AtomicReference<AllocationState> STATE = new AtomicReference<>(new AllocationState());

	static TransferBuffer claim(int bytes) {
		SimpleTransferBuffer result = new SimpleTransferBuffer(bytes, STATE.get());
		return result;
	}

	static void forceReload() {
		AllocationState oldState = STATE.getAndSet(new AllocationState());
		oldState.clear();
	}
}
