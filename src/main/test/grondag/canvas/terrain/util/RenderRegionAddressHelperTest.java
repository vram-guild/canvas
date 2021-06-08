package grondag.canvas.terrain.util;

import java.util.Random;

import org.junit.jupiter.api.Test;

class RenderRegionAddressHelperTest {
	@Test
	void test() {
		doIt();
		doIt();
		doIt();
	}

	private static int TEST_COUNT = 100000000;

	private static void doIt() {
		final Random r = new Random();

		r.setSeed(42);

		final byte[] data = new byte[TEST_COUNT + 3];

		for (int n = 0; n < TEST_COUNT + 3; ++n) {
			data[n] = (byte) (r.nextInt(20) - 2);
		}

		for (int n = 0; n < TEST_COUNT; ++n) {
			final var i = RenderRegionAddressHelper.computeRegionIndex(data[n], data[n + 1], data[n + 2]);
			assert i == RenderRegionAddressHelper.regionIndex(data[n], data[n + 1], data[n + 2]);
		}

		long start = System.nanoTime();

		for (int n = 0; n < TEST_COUNT; ++n) {
			RenderRegionAddressHelper.computeRegionIndex(data[n], data[n + 1], data[n + 2]);
		}

		System.out.println("Compute duration:       " + (System.nanoTime() - start));

		start = System.nanoTime();

		for (int n = 0; n < TEST_COUNT; ++n) {
			RenderRegionAddressHelper.regionIndex(data[n], data[n + 1], data[n + 2]);
		}

		System.out.println("Lookup duration:        " + (System.nanoTime() - start));
	}

	// baseline
	//	Compute duration:       3171940539
	//	Lookup duration:        2465613387
	//	Unsafe Lookup duration: 2404757643

	// short lookup array
	//	Compute duration:       3249437683
	//	Lookup duration:        2490307392
	//	Unsafe Lookup duration: 2486879609
}
