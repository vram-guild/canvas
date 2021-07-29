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

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import org.apache.commons.lang3.tuple.Pair;

public abstract class BufferTrace {
	public abstract void trace(String label);

	private static final boolean ENABLE = false;
	private static final int MAX_SIZE = 20;

	public static BufferTrace create() {
		return ENABLE ? new Active() : DUMMY;
	}

	private static final BufferTrace DUMMY = new BufferTrace() {
		@Override
		public void trace(String label) { }
	};

	static class Active extends BufferTrace {
		private final ObjectArrayFIFOQueue<Pair<String, StackTraceElement[]>> queue = new ObjectArrayFIFOQueue<>();

		@Override
		public void trace(String type) {
			synchronized (queue) {
				queue.enqueue(Pair.of(
						type + " " + Thread.currentThread().getName(),
						Thread.currentThread().getStackTrace()));

				if (queue.size() > MAX_SIZE) {
					queue.dequeue();
				}
			}
		}

		@Override
		public String toString() {
			synchronized (queue) {
				final int limit = queue.size();
				StringBuilder builder = new StringBuilder();

				for (int i = 0; i < limit; ++i) {
					final var entry = queue.dequeue();

					builder.append(entry.getLeft()).append("\n===========================================\n")
					.append(Arrays.stream(entry.getRight()).map(StackTraceElement::toString).collect(Collectors.joining("\n")))
					.append("\n");

					queue.enqueue(entry);
				}

				return builder.toString();
			}
		}
	}
}
