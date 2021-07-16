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

import static grondag.canvas.buffer.util.BinIndex.BIN_COUNT;
import static grondag.canvas.buffer.util.BinIndex.bin;

import java.util.IdentityHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.util.BinIndex;

/**
 * Tracks all allocations, ensures deallocation on render reload.
 * Implements configuration of allocation method.
 */
public class StreamBufferAllocator {
	private static class AllocationState {
		private final CanvasVertexFormat format;

		@SuppressWarnings("unchecked")
		private static final ObjectArrayList<StreamBuffer>[] BINS = new ObjectArrayList[BIN_COUNT];

		AllocationState(CanvasVertexFormat format) {
			this.format = format;

			for (int i = 0; i < BIN_COUNT; ++i) {
				BINS[i] = new ObjectArrayList<>();
			}
		}

		private StreamBuffer claim(int claimedBytes) {
			final BinIndex binIndex = bin(claimedBytes);
			final var list = BINS[binIndex.binIndex()];
			final StreamBuffer result = list.isEmpty() ? new StreamBuffer(binIndex, format) : list.pop();
			result.prepare(claimedBytes);
			return result;
		}

		private void release (StreamBuffer buffer) {
			BINS[buffer.binIndex.binIndex()].add(buffer);
		}

		private void forceReload() {
			for (var list : BINS) {
				for (var stream : list) {
					stream.shutdown();
				}

				list.clear();
			}
		}
	}

	private static final IdentityHashMap<CanvasVertexFormat, AllocationState> ALLOCATORS = new IdentityHashMap<>();

	static StreamBuffer claim(CanvasVertexFormat format, int bytes) {
		assert RenderSystem.isOnRenderThread();
		return ALLOCATORS.computeIfAbsent(format, AllocationState::new).claim(bytes);
	}

	public static void forceReload() {
		assert RenderSystem.isOnRenderThread();
		ALLOCATORS.values().forEach(AllocationState::forceReload);
	}

	static void release(StreamBuffer streamBuffer) {
		assert RenderSystem.isOnRenderThread();
		ALLOCATORS.get(streamBuffer.format).release(streamBuffer);
	}
}
