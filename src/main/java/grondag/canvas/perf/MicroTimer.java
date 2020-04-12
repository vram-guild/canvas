/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
 ******************************************************************************/
package grondag.canvas.perf;

import grondag.canvas.CanvasMod;

/**
 * For crude but simple microbenchmarks - for small scope, in-game situations
 * where JMH would be more than I want
 */
public class MicroTimer {
	private int hits;
	private long elapsed;
	private long min;
	private long max;
	private long last;
	private final int sampleSize;
	private final String label;
	private long started ;

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

	public long last()  {
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

	public void reportAndClear(){
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
