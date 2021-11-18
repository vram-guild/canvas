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

package grondag.canvas.terrain.util;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.CanvasTerrainRenderContext;

/**
 * Simple executor service with ability to submit privileged tasks
 * that run before non-privileged tasks that have not yet started, plus
 * distance-sorted execution.  Privilege is indicated by distance == -1
 * and privileged tasks run in order of submission.
 */
public class DedicatedTerrainExecutor implements TerrainExecutor {
	private final PriorityBlockingQueue<TerrainExecutorTask> queue = new PriorityBlockingQueue<>(1024, new Comparator<TerrainExecutorTask>() {
		@Override
		public int compare(TerrainExecutorTask o1, TerrainExecutorTask o2) {
			return Integer.compare(o1.priority(), o2.priority());
		}
	});

	private final int poolSize = threadCount();

	private final ImmutableList<Worker> workers;

	private final AtomicInteger renderTaskCount = new AtomicInteger();
	private int lastRenderTaskCount;
	private long nextTime;
	private String report = "";

	DedicatedTerrainExecutor() {
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
		return threadCount > 1 ? threadCount : 1;
	}

	@Override
	public void execute(TerrainExecutorTask task) {
		queue.add(task);
	}

	@Override
	public void clear() {
		queue.clear();

		for (final Worker w : workers) {
			w.context = new CanvasTerrainRenderContext();
		}
	}

	@Override
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	private class Worker implements Runnable {
		private CanvasTerrainRenderContext context = new CanvasTerrainRenderContext();

		@Override
		public void run() {
			while (true) {
				try {
					final TerrainExecutorTask t = queue.take();

					if (t != null) {
						t.run(context);
					}
				} catch (final InterruptedException e) {
					// NOOP
				} catch (final Exception e) {
					CanvasMod.LOG.error("Unhandled error during rendering. Impact unknown.", e);
				}
			}
		}
	}

	@Override
	public void execute(Runnable command) {
		throw new UnsupportedOperationException("Dedicated terrain executor recevied shared-mode task");
	}

	@Override
	public void debugReport(List<String> target) {
		final long newTime = System.currentTimeMillis();

		if (newTime > nextTime) {
			nextTime = newTime + 1000;
			final int newRenderCount = renderTaskCount.get();
			report = String.format("Render tasks: %d rate: %d", queue.size(), newRenderCount - lastRenderTaskCount);
			lastRenderTaskCount = newRenderCount;
		}

		target.add(report);
	}
}
