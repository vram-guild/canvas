/*
 * Copyright © Original Authors
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
