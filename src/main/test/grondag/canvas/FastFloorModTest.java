package grondag.canvas;

import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;

import grondag.canvas.terrain.util.FastFloorMod;

class FastFloorModTest {
	private static final int MIN = -30000000;
	private static final int MAX = 30000000;

	@Test
	void test() {
		new InnerTest(4).test();
		new InnerTest(5).test();
		new InnerTest(6).test();
		new InnerTest(7).test();
		new InnerTest(8).test();
		new InnerTest(9).test();
		new InnerTest(10).test();
		new InnerTest(11).test();
		new InnerTest(12).test();
		new InnerTest(13).test();
		new InnerTest(14).test();
		new InnerTest(15).test();
		new InnerTest(16).test();
		new InnerTest(17).test();
		new InnerTest(18).test();
		new InnerTest(19).test();
		new InnerTest(20).test();
		new InnerTest(21).test();
		new InnerTest(22).test();
		new InnerTest(23).test();
		new InnerTest(24).test();
		new InnerTest(25).test();
		new InnerTest(26).test();
		new InnerTest(27).test();
		new InnerTest(28).test();
		new InnerTest(29).test();
		new InnerTest(30).test();
		new InnerTest(31).test();
		new InnerTest(32).test();
	}

	static class InnerTest {
		final int size;
		private final IntUnaryOperator mod;

		InnerTest(int radius) {
			size = radius * 2 + 1;
			mod = FastFloorMod.get(radius);
		}

		void test() {
			System.out.println();
			System.out.println("DIAMETER = " + size);

			for (int i = MIN; i <= MAX; ++i) {
				assert Math.floorMod(i, size) == mod.applyAsInt(i);

				//				if (Math.floorMod(i, size) != mod.applyAsInt(i)) {
				//					System.out.println("Should be " + Math.floorMod(i, size));
				//					mod.applyAsInt(i);
				//				}
			}

			System.out.println();
			runA();
			runA();
			runA();
			runA();
			runA();
			runA();

			System.out.println();
			runB();
			runB();
			runB();
			runB();
			runB();
			runB();

			System.out.println();
			runA();
			runA();
			runA();
			runA();
			runA();
			runA();

			System.out.println();
			runB();
			runB();
			runB();
			runB();
			runB();
			runB();

			System.out.println();
			runA();
			runA();
			runA();
			runA();
			runA();
			runA();

			System.out.println();
			runB();
			runB();
			runB();
			runB();
			runB();
			runB();

			System.out.println();
			runA();
			runA();
			runA();
			runA();
			runA();
			runA();

			System.out.println();
			runB();
			runB();
			runB();
			runB();
			runB();
			runB();
		}

		long runA() {
			final long start = System.nanoTime();
			long result = 0;

			for (int n = 0; n < 10; ++n) {
				for (int i = MIN; i <= MAX; ++i) {
					result += Math.floorMod(i, size);
				}
			}

			final long end = System.nanoTime();

			System.out.println("Math: " + (end - start) / 1000.0);

			return result;
		}

		long runB() {
			final long start = System.nanoTime();

			long result = 0;

			for (int n = 0; n < 10; ++n) {
				for (int i = MIN; i <= MAX; ++i) {
					result += mod.applyAsInt(i);
				}
			}

			final long end = System.nanoTime();

			System.out.println("Fast: " + (end - start) / 1000.0);

			return result;
		}
	}
}
