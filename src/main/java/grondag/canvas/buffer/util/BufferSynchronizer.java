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

package grondag.canvas.buffer.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.varia.GFX;

public class BufferSynchronizer {
	private static final ObjectArrayFIFOQueue<SyncBufferList> queue = new ObjectArrayFIFOQueue<>(4);
	private static SyncBufferList currentFrameAccumultator = new SyncBufferList();

	public static void accept(SynchronizedBuffer buffer) {
		currentFrameAccumultator.add(buffer);
	}

	public static void checkPoint() {
		releaseBuffers();

		if (!currentFrameAccumultator.isEmpty()) {
			currentFrameAccumultator.claimFence();
			queue.enqueue(currentFrameAccumultator);
			currentFrameAccumultator = new SyncBufferList();
		}
	}

	private static void releaseBuffers() {
		while (!queue.isEmpty()) {
			final var list = queue.dequeue();

			if (!list.complete(0)) {
				queue.enqueueFirst(list);
				break;
			}
		}
	}

	private static class SyncBufferList extends ObjectArrayList<SynchronizedBuffer> {
		private long fence = 0;

		private boolean complete(long waitNanos) {
			assert fence != 0;
			final long nanos = System.nanoTime();

			final int status = GFX.clientWaitSync(fence, 0, waitNanos);

			if (status == GFX.GL_ALREADY_SIGNALED || status == GFX.GL_CONDITION_SATISFIED) {
				if (status == GFX.GL_CONDITION_SATISFIED) {
					System.out.println("Fence wait time (ms): " + (System.nanoTime() - nanos) / 1000000.0);
				}

				release();
				return true;
			} else {
				assert status == GFX.GL_TIMEOUT_EXPIRED;
				return false;
			}
		}

		private void claimFence() {
			assert fence == 0;
			fence = GFX.fenceSynch();
		}

		private void release() {
			assert fence != 0;

			for (var buffer : this) {
				buffer.onBufferSync();
			}

			clear();
			fence = 0;
		}
	}

	public interface SynchronizedBuffer {
		void onBufferSync();

		void shutdown();
	}
}
