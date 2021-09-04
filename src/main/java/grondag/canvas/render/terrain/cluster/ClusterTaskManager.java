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

package grondag.canvas.render.terrain.cluster;

import java.util.ArrayDeque;

import grondag.frex.api.config.FlawlessFrames;

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
