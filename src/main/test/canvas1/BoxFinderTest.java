package canvas1;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;

import grondag.canvas.chunk.occlusion.AreaFinder;
import grondag.canvas.chunk.occlusion.BoxFinder;
import grondag.canvas.chunk.occlusion.PackedBox;

class BoxFinderTest {
	final long[] words = new long[4096];

	final BoxFinder finder = new BoxFinder(new AreaFinder());
	final IntArrayList boxes =  finder.boxes;

	@Test
	void test() {
		fill(0, 0, 0, 16, 16, 16);

		finder.findBoxes(words, 0);

		assert boxes.size() == 1 && boxes.getInt(0) == PackedBox.FULL_BOX;

		Arrays.fill(words, 0);

		fill(0, 0, 0, 9, 9, 9);

		fill(8, 8, 8, 16, 16, 16);

		finder.findBoxes(words, 0);

		for (final int box : boxes) {
			System.out.println(PackedBox.toString(box));
		}

		assert boxes.size() == 2;
		assert boxes.getInt(0) == PackedBox.pack(0, 0, 0, 9, 9, 9, 0);
		assert boxes.getInt(1) == PackedBox.pack(8, 8, 8, 16, 16, 16, 0);

		Arrays.fill(words, 0);

		fill(0, 0, 0, 3, 3, 3);

		finder.findBoxes(words, 0);

		assert boxes.size() == 1 && boxes.getInt(0) == PackedBox.pack(0, 0, 0, 3, 3, 3, 0);
	}

	void fill (int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int x = x0; x < x1; x++) {
			for (int y = y0; y < y1; y++) {
				for (int z = z0; z < z1; z++) {
					final int index = x | (y << 4) | (z << 8);
					words[index >> 6] |=  (1L << (index & 63));
				}
			}
		}
	}
}
