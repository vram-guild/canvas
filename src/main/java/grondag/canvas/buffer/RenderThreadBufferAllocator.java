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

import java.util.function.Function;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.util.BinIndex;

/**
 * Tracks all allocations, ensures deallocation on render reload.
 * Implements configuration of allocation method.
 */
public class RenderThreadBufferAllocator<T extends AllocatableBuffer> {
	private final Function<BinIndex, T> allocator;

	@SuppressWarnings("unchecked")
	private final ObjectArrayList<T>[] BINS = new ObjectArrayList[BIN_COUNT];

	RenderThreadBufferAllocator(Function<BinIndex, T> allocator) {
		this.allocator = allocator;

		for (int i = 0; i < BIN_COUNT; ++i) {
			BINS[i] = new ObjectArrayList<>();
		}
	}

	public T claim(int claimedBytes) {
		assert RenderSystem.isOnRenderThread();
		final BinIndex binIndex = bin(claimedBytes);
		final var list = BINS[binIndex.binIndex()];
		final T result = list.isEmpty() ? allocator.apply(binIndex) : list.pop();
		result.prepare(claimedBytes);
		return result;
	}

	public void release (T buffer) {
		assert RenderSystem.isOnRenderThread();
		BINS[buffer.binIndex().binIndex()].add(buffer);
	}

	public void forceReload() {
		assert RenderSystem.isOnRenderThread();

		for (var list : BINS) {
			for (var buffer : list) {
				buffer.shutdown();
			}

			list.clear();
		}
	}
}
