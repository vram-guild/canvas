package grondag.canvas.chunk.occlusion;

import java.util.Arrays;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class AreaFinder {
	private static final Area[] AREA;

	private static final int AREA_COUNT;

	static {
		final IntOpenHashSet keys = new IntOpenHashSet();

		keys.add(AreaUtil.areaKey(0, 0, 15, 15));

		keys.add(AreaUtil.areaKey(1, 0, 15, 15));
		keys.add(AreaUtil.areaKey(0, 0, 14, 15));
		keys.add(AreaUtil.areaKey(0, 1, 15, 15));
		keys.add(AreaUtil.areaKey(0, 0, 15, 14));

		for (int dx = 14; dx >= 0; --dx) {
			for (int dy = 14; dy >= 0; --dy) {
				for (int x0 = 0; x0 < 15; x0++) {
					final int x1 = x0 + dx;

					if (x1 > 15) {
						break;
					}

					for (int y0 = 0; y0 < 15; y0++) {
						final int y1 = y0 + dy;

						if(y1 > 15) {
							break;
						}

						keys.add(AreaUtil.areaKey(x0, y0, x1, y1));
					}
				}
			}
		}

		AREA_COUNT = keys.size();

		AREA = new Area[AREA_COUNT];

		int i = 0;

		for(final int k : keys) {
			AREA[i++] = new Area(k);
		}

		Arrays.sort(AREA, (a, b) -> {
			final int result = Integer.compare(b.areaSize, a.areaSize);

			// within same area size, prefer more compact rectangles
			return result == 0 ? Integer.compare(a.edgeCount, b.edgeCount) : result;
		});
	}

	final long[] bits = new long[4];

	public void find(long[] bitsIn, Consumer<Area> consumer) {
		final long[] bits = this.bits;
		System.arraycopy(bitsIn, 0, bits, 0, 4);

		long hash = AreaUtil.areaHash(bits);

		for(final Area r : AREA) {
			if (r.matchesHash(hash) && r.matches(bits)) {
				consumer.accept(r);
				AreaUtil.clearAreaFromWords(r, bits);
				hash = AreaUtil.areaHash(bits);

				if (hash == 0 || r.areaSize < 4) {
					break;
				}
			}
		}
	}
}