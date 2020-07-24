package grondag.canvas;

import java.util.Random;

import io.netty.util.internal.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

import grondag.canvas.terrain.occlusion.region.OcclusionBitPrinter;
import grondag.canvas.terrain.occlusion.region.area.Area;
import grondag.canvas.terrain.occlusion.region.area.AreaFinder;
import grondag.canvas.terrain.occlusion.region.area.AreaSample;

class AreaFinderTest {

	@Test
	void test() {
		final AreaFinder finder = new AreaFinder();
		final AreaSample sample = new AreaSample();

		final Random rand = ThreadLocalRandom.current();

		for(int i = 0; i < 512; i++) {
			final int x = rand.nextInt(16);
			final int y = rand.nextInt(16);
			sample.fill(x, y, x, y);
		}

		System.out.println("INPUT");
		OcclusionBitPrinter.printShape(sample.bits, 0);

		System.out.println();
		System.out.println("OUTPUT");
		finder.find(sample.bits, 0, a -> {});

		for (final Area a : finder.areas) {
			a.printShape();
		}
	}
}
