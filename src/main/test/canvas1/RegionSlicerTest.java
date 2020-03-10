package canvas1;

import java.util.Random;

import io.netty.util.internal.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

import grondag.canvas.chunk.occlusion.RegionSlicer;

public class RegionSlicerTest {
	@Test
	void test() {
		final RegionSlicer slicer = new RegionSlicer();

		final long[] sample = new long[4 * 16];

		final Random rand = ThreadLocalRandom.current();

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

		System.out.println("INPUT");
		System.out.println();

		for(int z = 0; z < 16; z++) {
			System.out.println("Z = " + z);
			TestUtils.printShape(sample, z * 4);
			System.out.println();
		}


		slicer.copyAxisZ(sample, 0);
		slicer.buildAxisZ();

		System.out.println();
		System.out.println("OUTPUT");

		for(int z = 0; z <= 16; z++) {
			System.out.println("Z = " + z);
			TestUtils.printShape(slicer.outputBits, z * 4);
			System.out.println();
		}
	}

	static void setZyx(long[] bits, int x, int y, int z) {
		final int key  = x | (y << 4) | (z << 8);
		bits[key >> 6] |= (1L << (key &  63));
	}
}
