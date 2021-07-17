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

import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.buffer.util.BinIndex;

/**
 * Tracks all allocations, ensures deallocation on render reload.
 * Implements configuration of allocation method.
 */
public class BufferAllocator<T extends AllocatableBuffer> {
	private final Function<BinIndex, T> allocator;
	private final String traceName;

	@SuppressWarnings("unchecked")
	protected final Queue<T>[] BINS = new Queue[BIN_COUNT];

	BufferAllocator(String traceName, Function<BinIndex, T> allocator, Supplier<Queue<T>> queueFactory) {
		this.allocator = allocator;
		this.traceName = traceName;

		for (int i = 0; i < BIN_COUNT; ++i) {
			BINS[i] = queueFactory.get();
		}
	}

	/**
	 * Allocator may return null to allow for external fall-back allocation.
	 */
	public final @Nullable T claim(int claimedBytes) {
		final BinIndex binIndex = bin(claimedBytes);
		final var bin = BINS[binIndex.binIndex()];
		trackClaim(binIndex);
		T result = bin.poll();

		if (result == null) {
			result = allocator.apply(binIndex);

			if (result == null) {
				return null;
			}
		}

		result.prepare(claimedBytes);
		result.trace().trace(traceName + " CLAIM");
		return result;
	}

	/**
	 * Removes or creates an instance without claiming it.
	 * For transferring on-thread releases back to an off-thread allocator.
	 */
	public final @Nullable T take(BinIndex binIndex) {
		assert RenderSystem.isOnRenderThread();
		T result = BINS[binIndex.binIndex()].poll();

		if (result == null) {
			result = allocator.apply(binIndex);

			if (result == null) {
				return null;
			}
		}

		result.trace().trace(traceName + " TAKE");
		return result;
	}

	/**
	 * Adds an instance directly without any extra steps.
	 * For transferring on-thread releases back to an off-thread allocator.
	 */
	public final void put(T buffer) {
		assert RenderSystem.isOnRenderThread();
		buffer.trace().trace(traceName + " PUT");
		BINS[buffer.binIndex().binIndex()].offer(buffer);
	}

	/** For the tracking sub-type. */
	protected void trackClaim(BinIndex binIndex) {
		// NOOP;
	}

	public void release (T buffer) {
		assert RenderSystem.isOnRenderThread();
		buffer.trace().trace(traceName + " RELEASE");
		BINS[buffer.binIndex().binIndex()].offer(buffer);
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
