package canvas1;

import java.util.Random;

import io.netty.util.internal.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

import grondag.canvas.chunk.occlusion.PackedBox;

class PackedBoxTest {

	@Test
	void test() {

		assert PackedBox.pack(0, 0, 0, 16, 16, 16) == PackedBox.FULL_BOX;

		final Random r = ThreadLocalRandom.current();

		for (int i = 0; i < 500; i++) {

			final int x0 = r.nextInt(16);
			final int y0 = r.nextInt(16);
			final int z0 = r.nextInt(16);
			final int x1 = Math.min(16, x0 + r.nextInt(15) + 1);
			final int y1 = Math.min(16, y0 + r.nextInt(15) + 1);
			final int z1 = Math.min(16, z0 + r.nextInt(15) + 1);

			final int bounds = PackedBox.pack(x0, y0, z0, x1, y1, z1);

			assert PackedBox.x0(bounds) == x0;
			assert PackedBox.y0(bounds) ==  y0;
			assert PackedBox.z0(bounds) ==  z0;
			assert PackedBox.x1(bounds) ==  x1;
			assert PackedBox.y1(bounds) ==  y1;
			assert PackedBox.z1(bounds) ==  z1;
		}
	}
}
