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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import grondag.canvas.buffer.util.BinIndex;

/**
 * Tracks allocation demand between updates. Meant for an off-thread allocator
 * that must replenish the queue on-thread. Release is not tracked because it
 * cannot normally occur for such an allocator.
 */
public class TrackedBufferAllocator<T extends AllocatableBuffer> extends BufferAllocator<T> {
	private final AtomicInteger[] demandCounters = new AtomicInteger[BinIndex.BIN_COUNT];
	private final int[] unmetDemandForecast = new int[BinIndex.BIN_COUNT];
	private final int[] peakDemandForecast = new int[BinIndex.BIN_COUNT];
	private int totalPeakDemandBytes = 0;

	TrackedBufferAllocator(String traceName, Function<BinIndex, T> allocator, Supplier<Queue<T>> queueFactory) {
		super(traceName, allocator, queueFactory);

		for (int i = 0; i < BinIndex.BIN_COUNT; ++i) {
			demandCounters[i] = new AtomicInteger();
		}
	}

	@Override
	protected void trackClaim(BinIndex bin) {
		demandCounters[bin.binIndex()].incrementAndGet();
	}

	@Override
	public void forceReload() {
		super.forceReload();
		totalPeakDemandBytes = 0;

		for (int i = 0; i < BinIndex.BIN_COUNT; ++i) {
			demandCounters[i].set(0);
			unmetDemandForecast[i] = 0;
			peakDemandForecast[i] = 0;
		}
	}

	public void forecastUnmetDemand() {
		int totalPeak = 0;

		for (int i = 0; i < BinIndex.BIN_COUNT; ++i) {
			// PERF: consider reducing peak demand over time if it isn't used
			final int peak = Math.max(demandCounters[i].getAndSet(0), peakDemandForecast[i]);
			peakDemandForecast[i] = peak;
			totalPeak += peak * BinIndex.fromIndex(i).capacityBytes();
			unmetDemandForecast[i] = Math.max(0, peak - BINS[i].size());
		}

		totalPeakDemandBytes = totalPeak;
	}

	/** Call AFTER {@link #forecastUnmetDemand()}. */
	public int unmetDemandForecast(BinIndex bin) {
		return unmetDemandForecast[bin.binIndex()];
	}

	public int totalPeakDemandBytes() {
		return totalPeakDemandBytes;
	}
}
