package grondag.canvas.perf;

import java.util.concurrent.atomic.AtomicInteger;

import grondag.canvas.Configurator;
import grondag.fermion.sc.concurrency.ConcurrentPerformanceCounter;

public class ChunkRebuildCounters {
	public static final boolean ENABLED = Configurator.enablePerformanceTrace;

	public final ConcurrentPerformanceCounter buildCounter = new ConcurrentPerformanceCounter();
	public final ConcurrentPerformanceCounter copyCounter = new ConcurrentPerformanceCounter();

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