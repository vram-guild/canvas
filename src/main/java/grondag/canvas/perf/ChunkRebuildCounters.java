package grondag.canvas.perf;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.fermion.sc.concurrency.ConcurrentPerformanceCounter;

public abstract class ChunkRebuildCounters {
	private ChunkRebuildCounters() {}

	public static final boolean ENABLED = Configurator.enablePerformanceTrace;

	private static final ConcurrentPerformanceCounter buildCounter = new ConcurrentPerformanceCounter();
	private static final ConcurrentPerformanceCounter copyCounter = new ConcurrentPerformanceCounter();
	private static final ConcurrentPerformanceCounter uploadCounter = new ConcurrentPerformanceCounter();

	public static void reset() {
		buildCounter.clearStats();
		copyCounter.clearStats();
		uploadCounter.clearStats();
	}

	private static final ThreadLocal<Long> chunkStart = ThreadLocal.withInitial(() -> 0L);

	public static void startChunk() {
		chunkStart.set(System.nanoTime());
	}

	public static void completeChunk() {
		buildCounter.endRun(chunkStart.get());
		final int chunkCount = buildCounter.addCount(1);

		if(chunkCount == 2000) {
			CanvasMod.LOG.info(String.format("Rebuild elapsed time per region for last 2000 chunks = %,dns  total time: %fs", buildCounter.runTime() / 2000, buildCounter.runTime() / 1000000000d));

			final int copyCount = copyCounter.runCount();
			CanvasMod.LOG.info(String.format("World copy time per chunk for last %d regions = %,dns  total time: %fs", copyCount, copyCount == 0 ? 0 : copyCounter.runTime() / copyCount, copyCounter.runTime() / 1000000000d));

			final int uploadCount = uploadCounter.runCount();
			CanvasMod.LOG.info(String.format("Upload time per region for last %d regions = %,dns  total time: %fs", uploadCount, uploadCount == 0 ? 0 : uploadCounter.runTime() / uploadCount, uploadCounter.runTime() / 1000000000d));
			reset();

			CanvasMod.LOG.info("");
		}
	}

	private static final ThreadLocal<Long> copyStart = ThreadLocal.withInitial(() -> 0L);

	public static void startCopy() {
		copyStart.set(System.nanoTime());
	}

	public static void completeCopy() {
		copyCounter.endRun(copyStart.get());
		copyCounter.addCount(1);
	}

	private static final ThreadLocal<Long> uploadStart = ThreadLocal.withInitial(() -> 0L);

	public static void startUpload() {
		uploadStart.set(System.nanoTime());
	}

	public static void completeUpload() {
		uploadCounter.endRun(uploadStart.get());
		uploadCounter.addCount(1);
	}
}
