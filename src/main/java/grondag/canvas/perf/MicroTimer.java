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

import grondag.canvas.CanvasMod;

/**
 * For crude but simple microbenchmarks - for small scope, in-game situations
 * where JMH would be more than I want.
 */
public class MicroTimer {
	private final int sampleSize;
	private final String label;
	private int hits;
	private long elapsed;
	private long min;
	private long max;
	private long last;
	private long started;

	public MicroTimer(String label, int sampleSize) {
		this.label = label;
		this.sampleSize = sampleSize;
	}

	public int hits() {
		return hits;
	}

	public long elapsed() {
		return elapsed;
	}

	public void start() {
		started = System.nanoTime();
	}

	public long last() {
		return last;
	}

	/**
	 * Returns true if timer output stats this sample. For use if want to output
	 * supplementary information at same time.
	 */
	public boolean stop() {
		final long t = System.nanoTime() - started;
		elapsed += t;
		last = t;

		if (t < min) {
			min = t;
		}

		if (t > max) {
			max = t;
		}

		final long h = ++hits;

		if (h == sampleSize) {
			reportAndClear();
			return true;
		} else {
			return false;
		}
	}

	public void reportAndClear() {
		if (hits == 0) {
			hits = 1;
		}

		CanvasMod.LOG.info(String.format("Avg %s duration = %,d ns, min = %d, max = %d, total duration = %,d, total runs = %,d", label,
				elapsed / hits, min, max, elapsed / 1000000, hits));

		hits = 0;
		elapsed = 0;
		max = Long.MIN_VALUE;
		min = Long.MAX_VALUE;
	}
}
