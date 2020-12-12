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

package grondag.canvas.perf;

import java.util.function.LongSupplier;

import net.minecraft.util.Util;

import grondag.canvas.CanvasMod;

public abstract class LagFinder {
	public abstract void start(String label);

	public abstract void swap(String label);

	public abstract void complete();

	private static class Dummy extends LagFinder {
		@Override
		public void start(String label) { }

		@Override
		public void swap(String label) { }

		@Override
		public void complete() { }
	}

	private static class Simple extends LagFinder {
		private String label;
		private long started;
		private long threshold;
		private final LongSupplier thresholdFunction;

		Simple(LongSupplier thresholdFunction) {
			this.thresholdFunction = thresholdFunction;
		}

		@Override
		public void start(String label) {
			threshold = thresholdFunction.getAsLong();
			this.label = label;
			started = Util.getMeasuringTimeNano();
		}

		@Override
		public void swap(String label) {
			final long now = Util.getMeasuringTimeNano();
			final long elapsed = now - started;

			if (elapsed > threshold) {
				CanvasMod.LOG.info(String.format("Lag spike at %s - %,dns, threshold is %,dns", this.label, elapsed, threshold));
			}

			this.label = label;
			started = now;
		}

		@Override
		public void complete() {
			final long elapsed = Util.getMeasuringTimeNano() - started;

			if (elapsed > threshold) {
				CanvasMod.LOG.info(String.format("Lag spike at %s - %,dns, threshold is %,dns", label, elapsed, threshold));
			}
		}
	}

	public static final LagFinder DUMMMY = new Dummy();

	public static LagFinder create(LongSupplier thresholdFunction) {
		return new Simple(thresholdFunction);
	}
}
