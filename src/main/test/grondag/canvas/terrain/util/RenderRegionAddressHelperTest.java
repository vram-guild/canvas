package grondag.canvas.terrain.util;

import java.util.Random;

import org.junit.jupiter.api.Test;

class RenderRegionAddressHelperTest {
	@Test
	void test() {
		runTests();
		runTests();
		runTests();
	}

	private static int TEST_COUNT = 100000000;

	private static void runTests() {
		final Random r = new Random();

		r.setSeed(42);

		final byte[] data = new byte[TEST_COUNT + 3];

		for (int n = 0; n < TEST_COUNT + 3; ++n) {
			data[n] = (byte) (r.nextInt(20) - 2);
		}

		for (int n = 0; n < TEST_COUNT; ++n) {
			final var i = RenderRegionStateIndexer.computeRegionIndex(data[n], data[n + 1], data[n + 2]);
			assert i == RenderRegionStateIndexer.regionIndex(data[n], data[n + 1], data[n + 2]);
		}

		long start = System.nanoTime();

		for (int n = 0; n < TEST_COUNT; ++n) {
			RenderRegionStateIndexer.computeRegionIndex(data[n], data[n + 1], data[n + 2]);
		}

		System.out.println("Compute duration:       " + (System.nanoTime() - start));

		start = System.nanoTime();

		for (int n = 0; n < TEST_COUNT; ++n) {
			RenderRegionStateIndexer.regionIndex(data[n], data[n + 1], data[n + 2]);
		}

		System.out.println("Lookup duration:        " + (System.nanoTime() - start));
	}
}
