package grondag.canvas.perf;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentPerformanceCounter {
	private final AtomicLong runTime = new AtomicLong(0);
	private final AtomicInteger runCount = new AtomicInteger(0);

	public void clearStats() {
		runCount.set(0);
		runTime.set(0);
	}

	public long startRun() {
		return System.nanoTime();
	}

	public long endRun(long startTime) {
		return runTime.addAndGet(System.nanoTime() - startTime);
	}

	public int addCount(int howMuch) {
		return runCount.addAndGet(howMuch);
	}

	public int runCount() { return runCount.get(); }
	public long runTime() { return runTime.get(); }
	public long timePerRun() { return runCount.get() == 0 ? 0 : runTime.get() / runCount.get(); }
	public String stats() { return String.format("time this sample = %1$.3fs for %2$,d items @ %3$,dns each."
			, ((double)runTime() / 1000000000), runCount(),  timePerRun()); }
}
