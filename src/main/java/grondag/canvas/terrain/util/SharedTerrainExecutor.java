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

package grondag.canvas.terrain.util;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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
public class SharedTerrainExecutor implements TerrainExecutor {
	private final PriorityBlockingQueue<TerrainExecutorTask> renderQueue = new PriorityBlockingQueue<>(4096, new Comparator<TerrainExecutorTask>() {
		@Override
		public int compare(TerrainExecutorTask o1, TerrainExecutorTask o2) {
			return Integer.compare(o1.priority(), o2.priority());
		}
	});

	private final ArrayBlockingQueue<Runnable> serverQueue = new ArrayBlockingQueue<>(4096);

	private final int poolSize = threadCount();
	private final Semaphore mixedSignal = new Semaphore(poolSize - 2);

	private final ImmutableList<Worker> workers;

	private final AtomicInteger renderTaskCount = new AtomicInteger();
	private final AtomicInteger serverTaskCount = new AtomicInteger();

	private int lastRenderTaskCount;
	private int lastServerTaskCount;
	private long nextTime;
	private String report0 = "", report1 = "";

	SharedTerrainExecutor() {
		final ImmutableList.Builder<Worker> builder = ImmutableList.builder();

		final RenderWorker renderWorker = new RenderWorker();
		builder.add(renderWorker);
		final Thread rederThread = new Thread(renderWorker, "Canvas Render Thread");
		rederThread.setDaemon(true);
		rederThread.start();

		final ServerWorker serverWorker = new ServerWorker();
		builder.add(serverWorker);
		final Thread serverThread = new Thread(serverWorker, "Canvas Server Thread");
		serverThread.setDaemon(true);
		serverThread.start();

		final int limit = poolSize - 2;

		for (int i = 0; i < limit; i++) {
			final RenderWorker w = ((i & 1) == 0) ? new RenderFirstWorker() : new ServerFirstWorker();
			builder.add(w);

			final Thread thread = new Thread(w, "Canvas Mixed Thread - " + i);
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

		return threadCount > 1 ? threadCount - 1 : 1;
	}

	@Override
	public void execute(TerrainExecutorTask task) {
		renderQueue.add(task);
		mixedSignal.release();
		renderTaskCount.incrementAndGet();
	}

	@Override
	public void execute(Runnable command) {
		serverQueue.add(command);
		mixedSignal.release();
		serverTaskCount.incrementAndGet();
	}

	@Override
	public void clear() {
		renderQueue.clear();

		for (final Worker w : workers) {
			w.close();
		}
	}

	@Override
	public boolean isEmpty() {
		return renderQueue.isEmpty();
	}

	private interface Worker extends Runnable {
		void close();
	}

	private class RenderWorker implements Worker {
		protected TerrainRenderContext context = new TerrainRenderContext();

		@Override
		public void run() {
			while (true) {
				try {
					final TerrainExecutorTask t = renderQueue.take();

					if (t != null) {
						t.run(context);
					}
				} catch (final InterruptedException e) {
					// NOOP
				} catch (final Exception e) {
					Sc.LOG.error("Unhandled error during rendering. Impact unknown.", e);
				}
			}
		}

		@Override
		public void close() {
			context.close();
		}
	}

	private class ServerWorker implements Worker {
		@Override
		public void run() {
			while (true) {
				try {
					final Runnable task = serverQueue.take();

					if (task != null) {
						task.run();
					}
				} catch (final InterruptedException e) {
					// NOOP
				} catch (final Exception e) {
					Sc.LOG.error("Unhandled error during rendering. Impact unknown.", e);
				}
			}
		}

		@Override
		public void close() {
			// NOOP
		}
	}

	private class RenderFirstWorker extends RenderWorker {
		@Override
		public void run() {
			while (true) {
				try {
					final TerrainExecutorTask t = renderQueue.poll();

					if (t == null) {
						Runnable runnable = serverQueue.poll();

						if (runnable != null) {
							runnable.run();
						}
					} else {
						t.run(context);
					}

					mixedSignal.acquire();
				} catch (final InterruptedException e) {
					// NOOP
				} catch (final Exception e) {
					Sc.LOG.error("Unhandled error during rendering. Impact unknown.", e);
				}
			}
		}

		@Override
		public void close() {
			context.close();
		}
	}

	private class ServerFirstWorker extends RenderWorker {
		@Override
		public void run() {
			while (true) {
				try {
					Runnable runnable = serverQueue.poll();

					if (runnable == null) {
						final TerrainExecutorTask t = renderQueue.poll();

						if (t != null) {
							t.run(context);
						}
					} else {
						runnable.run();
					}

					mixedSignal.acquire();
				} catch (final InterruptedException e) {
					// NOOP
				} catch (final Exception e) {
					Sc.LOG.error("Unhandled error during rendering. Impact unknown.", e);
				}
			}
		}

		@Override
		public void close() {
			context.close();
		}
	}

	@Override
	public void debugReport(List<String> target) {
		long newTime = System.currentTimeMillis();

		if (newTime > nextTime) {
			nextTime = newTime + 1000;

			final int newRenderCount = renderTaskCount.get();
			final int newServerCount = serverTaskCount.get();

			report0 = String.format("Render tasks: %d rate: %d",
					renderQueue.size(), newRenderCount - lastRenderTaskCount);

			report1 = String.format("Server tasks: %d rate: %d",
					serverQueue.size(), newServerCount - lastServerTaskCount);

			lastRenderTaskCount = newRenderCount;
			lastServerTaskCount = newServerCount;
		}

		target.add(report0);
		target.add(report1);
	}
}
