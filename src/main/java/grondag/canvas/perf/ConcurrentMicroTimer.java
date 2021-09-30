/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.perf;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import grondag.canvas.CanvasMod;

/**
 * For crude but simple microbenchmarks - for small scope, in-game situations
 * where JMH would be more than I want.
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
