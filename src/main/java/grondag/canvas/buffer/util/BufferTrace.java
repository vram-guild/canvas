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

package grondag.canvas.buffer.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class BufferTrace {
	public abstract void onClaim();

	public abstract void onRelease();

	private static final boolean ENABLE = false;

	public static BufferTrace create() {
		return ENABLE ? new Active() : DUMMY;
	}

	private static final BufferTrace DUMMY = new BufferTrace() {
		@Override
		public void onClaim() { }

		@Override
		public void onRelease() { }
	};

	static class Active extends BufferTrace {
		private StackTraceElement[] last;
		private StackTraceElement[] beforeLast;
		private StackTraceElement[] priorBeforeLast;

		private String lastType;
		private String beforeLastType;
		private String priorBeforeLastType;

		private void capture(String type) {
			priorBeforeLastType = beforeLastType;
			beforeLastType = lastType;
			lastType = type;

			priorBeforeLast = beforeLast;
			beforeLast = last;
			last = Thread.currentThread().getStackTrace();
		}

		@Override
		public void onClaim() {
			capture("CLAIM");
		}

		@Override
		public void onRelease() {
			capture("RELEASE");
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			if (priorBeforeLast != null) {
				builder.append(priorBeforeLastType).append("\n===========================================\n")
					.append(Arrays.stream(priorBeforeLast).map(StackTraceElement::toString).collect(Collectors.joining("\n")))
					.append("\n");
			}

			if (beforeLast != null) {
				builder.append(beforeLastType).append("\n===========================================\n")
					.append(Arrays.stream(beforeLast).map(StackTraceElement::toString).collect(Collectors.joining("\n")))
					.append("\n");
			}

			if (last != null) {
				builder.append(lastType).append("\n===========================================\n")
					.append(Arrays.stream(last).map(StackTraceElement::toString).collect(Collectors.joining("\n")))
					.append("\n");
			}

			return builder.toString();
		}
	}
}
