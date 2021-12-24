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

package grondag.canvas.buffer.render;

import static grondag.canvas.buffer.util.BinIndex.BIN_COUNT;
import static grondag.canvas.buffer.util.BinIndex.bin;

import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

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

		for (final var list : BINS) {
			for (final var buffer : list) {
				buffer.shutdown();
			}

			list.clear();
		}
	}
}
