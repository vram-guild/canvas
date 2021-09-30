/*
 * Copyright © Contributing Authors
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

package grondag.canvas.perf;

import io.vram.sc.concurrency.ConcurrentPerformanceCounter;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public abstract class ChunkRebuildCounters {
	public static final boolean ENABLED = Configurator.enablePerformanceTrace;
	private static final ConcurrentPerformanceCounter buildCounter = new ConcurrentPerformanceCounter();
	private static final ConcurrentPerformanceCounter copyCounter = new ConcurrentPerformanceCounter();
	private static final ConcurrentPerformanceCounter uploadCounter = new ConcurrentPerformanceCounter();
	private static final ThreadLocal<Long> chunkStart = ThreadLocal.withInitial(() -> 0L);
	private static final ThreadLocal<Long> copyStart = ThreadLocal.withInitial(() -> 0L);
	private static final ThreadLocal<Long> uploadStart = ThreadLocal.withInitial(() -> 0L);

	private ChunkRebuildCounters() {
	}

	public static void reset() {
		buildCounter.clearStats();
		copyCounter.clearStats();
		uploadCounter.clearStats();
	}

	public static void startChunk() {
		chunkStart.set(System.nanoTime());
	}

	public static void completeChunk() {
		buildCounter.endRun(chunkStart.get());
		final int chunkCount = buildCounter.addCount(1);

		if (chunkCount == 2000) {
			CanvasMod.LOG.info(String.format("Rebuild elapsed time per region for last 2000 chunks = %,dns  total time: %fs", buildCounter.runTime() / 2000, buildCounter.runTime() / 1000000000d));

			final int copyCount = copyCounter.runCount();
			CanvasMod.LOG.info(String.format("World copy time per chunk for last %d regions = %,dns  total time: %fs", copyCount, copyCount == 0 ? 0 : copyCounter.runTime() / copyCount, copyCounter.runTime() / 1000000000d));

			final int uploadCount = uploadCounter.runCount();
			CanvasMod.LOG.info(String.format("Upload time per region for last %d regions = %,dns  total time: %fs", uploadCount, uploadCount == 0 ? 0 : uploadCounter.runTime() / uploadCount, uploadCounter.runTime() / 1000000000d));
			reset();

			CanvasMod.LOG.info("");
		}
	}

	public static void startCopy() {
		copyStart.set(System.nanoTime());
	}

	public static void completeCopy() {
		copyCounter.endRun(copyStart.get());
		copyCounter.addCount(1);
	}

	public static void startUpload() {
		uploadStart.set(System.nanoTime());
	}

	public static void completeUpload() {
		uploadCounter.endRun(uploadStart.get());
		uploadCounter.addCount(1);
	}
}
