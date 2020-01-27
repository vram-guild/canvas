package grondag.canvas.perf;

import java.util.concurrent.atomic.AtomicInteger;

public class ChunkRebuildCounters {
	public final ConcurrentPerformanceCounter counter = new ConcurrentPerformanceCounter();
	public final AtomicInteger blockCounter = new AtomicInteger();
	public final AtomicInteger fluidCounter = new AtomicInteger();

	private static volatile ChunkRebuildCounters counters = new ChunkRebuildCounters();

	public static ChunkRebuildCounters get() {
		return counters;
	}

	public static void reset() {
		counters = new ChunkRebuildCounters();
	}
}