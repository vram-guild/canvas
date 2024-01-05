/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.buffer.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.varia.GFX;

public class BufferSynchronizer {
	private static final ObjectArrayFIFOQueue<SyncBufferList> queue = new ObjectArrayFIFOQueue<>(4);
	private static SyncBufferList currentFrameAccumulator = new SyncBufferList();

	public static void accept(SynchronizedBuffer buffer) {
		currentFrameAccumulator.add(buffer);
	}

	public static void checkPoint() {
		releaseBuffers();

		if (!currentFrameAccumulator.isEmpty()) {
			currentFrameAccumulator.claimFence();
			queue.enqueue(currentFrameAccumulator);
			currentFrameAccumulator = new SyncBufferList();
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

	@SuppressWarnings("serial")
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
			fence = GFX.fenceSync();
		}

		private void release() {
			assert fence != 0;

			for (final var buffer : this) {
				buffer.onBufferSync();
			}

			clear();
			GFX.deleteSync(fence);
			fence = 0;
		}
	}

	public interface SynchronizedBuffer {
		void onBufferSync();

		void shutdown();
	}
}
