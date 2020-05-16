package grondag.canvas;

import org.junit.jupiter.api.Test;

import grondag.canvas.chunk.occlusion.region.AreaFinder;
import grondag.canvas.chunk.occlusion.region.OcclusionBitPrinter;
import grondag.canvas.chunk.occlusion.region.PlaneFinder;

public class RegionSlicerTest {
	@Test
	void test() {
		final PlaneFinder slicer = new PlaneFinder(new AreaFinder());

		final long[] sample = new long[4 * 16];

		//final Random rand = ThreadLocalRandom.current();

		//		for(int i = 0; i < 4  * 16 * 64 * 2; i++) {
		//			final int x = rand.nextInt(16);
		//			final int y = rand.nextInt(16);
		//			final int z = rand.nextInt(16);
		//
		//			setZyx(sample, x, y, z);
		//		}

		for (int i = 0; i < 16; i++) {
			setZyx(sample, 15 - i, 15 - i, i);
		}

		OcclusionBitPrinter.printRegion("INPUT", sample, 0);

		slicer.buildAxisZ(sample, 0);

		System.out.println();
		System.out.println("OUTPUT - Z AXIS");

		for(int z = 0; z <= 16; z++) {
			System.out.println("Z = " + z);
			OcclusionBitPrinter.printShape(slicer.outputBits, z * 4);
			System.out.println();
		}

		slicer.buildAxisX(sample, 0);

		System.out.println();
		System.out.println("OUTPUT - X AXIS");

		for(int x = 0; x <= 16; x++) {
			System.out.println("X = " + x);
			OcclusionBitPrinter.printShape(slicer.outputBits, x * 4);
			System.out.println();
		}

		slicer.buildAxisY(sample, 0);

		System.out.println();
		System.out.println("OUTPUT - Y AXIS");

		for(int y = 0; y <= 16; y++) {
			System.out.println("Y = " + y);
			OcclusionBitPrinter.printShape(slicer.outputBits, y * 4);
			System.out.println();
		}
	}

	static void setZyx(long[] bits, int x, int y, int z) {
		final int key  = x | (y << 4) | (z << 8);
		bits[key >> 6] |= (1L << (key &  63));
	}
}
