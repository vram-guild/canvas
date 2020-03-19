package canvas1;

import java.util.Random;

import io.netty.util.internal.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

import grondag.canvas.chunk.occlusion.PackedBox;

class PackedBoxTest {

	@Test
	void test() {

		assert PackedBox.pack(0, 0, 0, 16, 16, 16) == PackedBox.FULL_BOX;

		final long near = PackedBox.packSortable(2, 2, 2, 4, 4, 2);
		final long medium = PackedBox.packSortable(0, 0, 0, 7, 7, 7);
		final long far = PackedBox.packSortable(1, 1, 2, 13, 2, 15);

		assert medium > near;
		assert far > medium;
		assert (int) near == PackedBox.pack(2, 2, 2, 4, 4, 2);
		assert (int) medium == PackedBox.pack(0, 0, 0, 7, 7, 7);
		assert (int) far == PackedBox.pack(1, 1, 2, 13, 2, 15);
		assert PackedBox.range((int) near) == PackedBox.OCCLUSION_RANGE_NEAR;
		assert PackedBox.range((int) medium) == PackedBox.OCCLUSION_RANGE_MEDIUM;
		assert PackedBox.range((int) far) == PackedBox.OCCLUSION_RANGE_FAR;

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
