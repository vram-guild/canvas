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
