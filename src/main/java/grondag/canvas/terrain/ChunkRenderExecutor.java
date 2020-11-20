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

package grondag.canvas.terrain;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.fermion.sc.Sc;

/**
 * Simple executor service with ability to submit privileged tasks
 * that run before non-privileged tasks that have not yet started, plus
 * distance-sorted execution.  Privilege is indicated by distance == -1
 * and privileged tasks run in order of submission.
 */
public class ChunkRenderExecutor {
	private final PriorityBlockingQueue<ChunkBuildTask> queue = new PriorityBlockingQueue<>(1024, new Comparator<ChunkBuildTask>() {
		@Override
		public int compare(ChunkBuildTask o1, ChunkBuildTask o2) {
			return Integer.compare(o1.priority, o2.priority);
		}
	});

	private final int poolSize = threadCount();

	@SuppressWarnings("unused")
	private final ImmutableList<Worker> workers;

	public ChunkRenderExecutor() {
		final ImmutableList.Builder<Worker> builder = ImmutableList.builder();

		for (int i = 0; i < poolSize; i++) {
			final Worker w = new Worker();
			builder.add(w);

			final Thread thread = new Thread(
					new Worker(),
					"Canvas Render Thread - " + i);
			thread.setDaemon(true);
			thread.start();
		}

		workers = builder.build();
	}

	private static int threadCount() {
		final int threadCount = Runtime.getRuntime().availableProcessors() - 1;

		if (threadCount > 4 && !MinecraftClient.getInstance().is64Bit()) {
			return 4;
		}

		return threadCount > 1 ? threadCount : 1;
	}

	public void execute(Consumer<TerrainRenderContext> task, int squaredDistance) {
		queue.add(new ChunkBuildTask(task, squaredDistance));
	}

	public void clear() {
		queue.clear();

		for (final Worker w : workers) {
			w.context.close();
			w.context = new TerrainRenderContext();
		}
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	private class ChunkBuildTask {
		final Consumer<TerrainRenderContext> task;

		/**
		 * Normally squared distance. Use -1 for privileged execution
		 */
		final int priority;

		ChunkBuildTask(Consumer<TerrainRenderContext> task, int priority) {
			this.task = task;
			this.priority = priority;
		}
	}

	private class Worker implements Runnable {
		private TerrainRenderContext context = new TerrainRenderContext();

		@Override
		public void run() {
			while (true) {
				try {
					final ChunkBuildTask t = queue.take();

					if (t != null) {
						t.task.accept(context);
					}
				} catch (final InterruptedException e) {
					// NOOP
				} catch (final Exception e) {
					Sc.LOG.error("Unhandled error during rendering. Impact unknown.", e);
				}
			}
		}
	}
}
