/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.perf;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import grondag.canvas.CanvasMod;

/**
 * For crude but simple microbenchmarks - for small scope, in-game situations
 * where JMH would be more than I want
 */
public class ConcurrentMicroTimer {
	private final AtomicInteger hits = new AtomicInteger();
	private final AtomicLong elapsed = new AtomicLong();
	private final int sampleSize;
	private final String label;
	private final ThreadLocal<Long> started = ThreadLocal.withInitial(() -> 0L);

	public ConcurrentMicroTimer(String label, int sampleSize) {
		this.label = label;
		this.sampleSize = sampleSize;
	}

	public int hits() {
		return hits.get();
	}

	public long elapsed() {
		return elapsed.get();
	}

	public void start() {
		started.set(System.nanoTime());
	}

	/**
	 * Returns true if timer output stats this sample. For use if want to output
	 * supplementary information at same time.
	 */
	public boolean stop() {
		final long end = System.nanoTime();
		final long e = elapsed.addAndGet(end - started.get());
		final long h = hits.incrementAndGet();
		if (h == sampleSize) {
			doReportAndClear(e, h);
			return true;
		} else {
			return false;
		}
	}

	private void doReportAndClear(long e, long h) {
		hits.set(0);
		elapsed.set(0);
		if (h == 0) h = 1;
		CanvasMod.LOG.info(String.format("Avg %s duration = %,d ns, total duration = %,d, total runs = %,d", label, e / h,
				e / 1000000, h));
	}

	public void reportAndClear() {
		doReportAndClear(elapsed.get(), hits.get());
	}
}
