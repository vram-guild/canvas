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

package grondag.canvas.perf;

import it.unimi.dsi.fastutil.longs.LongArrays;

public class RunTimer {
	public static final ThreadSafeRunTimer THREADED_5000 = new ThreadSafeRunTimer("THREADED 5000", 5000);
	public static final ThreadSafeRunTimer THREADED_50K = new ThreadSafeRunTimer("THREADED 50K", 50000);
	public static RunTimer TIMER_200 = new RunTimer("GENERIC 200", 200);
	public static RunTimer TIMER_2400 = new RunTimer("GENERIC 2400", 2400);
	public static RunTimer TIMER_100000 = new RunTimer("GENERIC 100000", 100000);
	final String label;
	final long[] data;
	final int size;
	int counter;
	long start;

	public RunTimer(String label, int sampleCount) {
		this.label = label;
		size = sampleCount;
		data = new long[sampleCount];
	}

	public static void stats(long[] data) {
		final int count = data.length;
		final int bucketSize = count / 5;
		final int rem = count - bucketSize * 5;
		final int[] sizes = new int[5];
		sizes[0] = bucketSize + (rem > 0 ? 1 : 0);
		sizes[1] = bucketSize + (rem > 1 ? 1 : 0);
		sizes[2] = bucketSize + (rem > 2 ? 1 : 0);
		sizes[3] = bucketSize + (rem > 3 ? 1 : 0);
		sizes[4] = bucketSize;

		int bucketIndex = 0;
		int nextBucket = sizes[0];
		final long[] buckets = new long[5];

		long b = 0;

		LongArrays.quickSort(data);

		for (int i = 0; i < count; i++) {
			if (i == nextBucket) {
				buckets[bucketIndex++] = b;
				b = 0;
				nextBucket += sizes[bucketIndex];
			}

			b += data[i];
		}

		buckets[4] = b;

		final long total = buckets[0] + buckets[1] + buckets[2] + buckets[3] + buckets[4];

		System.out.println(String.format("Total: %,d  Min: %,d  Max: %,d  Mean: %,d", total, data[0], data[count - 1],
				total / count));
		System.out.println(String.format("Bucket Percent: %,d   %,d   %,d   %,d   %,d", buckets[0] * 100 / total,
				buckets[1] * 100 / total, buckets[2] * 100 / total, buckets[3] * 100 / total,
				buckets[4] * 100 / total));
		System.out.println(String.format("Bucket Averages: %,d   %,d   %,d   %,d   %,d", buckets[0] / sizes[0],
				buckets[1] / sizes[1], buckets[2] / sizes[2], buckets[3] / sizes[3], buckets[4] / sizes[4]));
		System.out.println();
	}

	public void start() {
		start = System.nanoTime();
	}

	public void finish() {
		data[counter++] = (System.nanoTime() - start);

		if (counter == size) {
			System.out.println("Run Timer Result for " + label);
			stats(data);
			counter = 0;
		}
	}

	public static class ThreadSafeRunTimer {
		final String label;
		final int size;
		private final ThreadLocal<RunTimer> timers = new ThreadLocal<RunTimer>() {
			@Override
			protected RunTimer initialValue() {
				return new RunTimer(label, size);
			}
		};

		public ThreadSafeRunTimer(String label, int sampleCount) {
			this.label = label;
			size = sampleCount;
		}

		public final void start() {
			timers.get().start();
		}

		public final void finish() {
			timers.get().finish();
		}
	}
}
