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

package grondag.canvas.render.terrain.cluster;

import java.util.ArrayDeque;

import io.vram.frex.api.config.FlawlessFrames;

public class ClusterTaskManager {
	@FunctionalInterface interface ClusterTask {
		/** Task should return false if unable to complete and needs more time next frame. */
		boolean run(long deadlineNanos);
	}

	private static final ArrayDeque<ClusterTask> TASKS = new ArrayDeque<>();

	private ClusterTaskManager() { }

	public static void run(long deadlineNanos) {
		if (FlawlessFrames.isActive()) {
			deadlineNanos = Long.MAX_VALUE;
		}

		do {
			final var task = TASKS.poll();

			if (task == null) {
				break;
			} else {
				if (!task.run(deadlineNanos)) {
					// reschedule for next frame if not complete
					TASKS.offerFirst(task);
				}
			}
		} while (System.nanoTime() < deadlineNanos);
	}

	static void schedule(ClusterTask task) {
		TASKS.offer(task);
	}

	static void clear() {
		TASKS.clear();
	}
}
