package grondag.canvas.chunk.occlusion;

public class AreaUtil {

	public static int areaKey(int x0, int y0, int x1, int y1) {
		return x0 | (y0 << 5) | (x1 << 10) | (y1 << 15);
	}

	public static void printArea(int areaKey) {
		final int x0 = areaKey & 31;
		final int y0 = (areaKey >> 5) & 31;
		final int x1 = (areaKey >> 10) & 31;
		final int y1 = (areaKey >> 15) & 31;

		final int x = x1 - x0 + 1;
		final int y = y1 - y0 + 1;
		final int a = x * y;
		System.out.println(String.format("%d x %d, area %d, (%d, %d) to (%d, %d)", x, y, a, x0, y0, x1, y1));
	}

	public static long areaWordHashMask (int x, int y) {
		return  1L << ((y << 3) | x);
	}

	public static long areaHash(long words[]) {
		long result = AreaUtil.areaWordHash(words[0], 0);
		result |= AreaUtil.areaWordHash(words[1], 1);
		result |= AreaUtil.areaWordHash(words[2], 2);
		result |= AreaUtil.areaWordHash(words[3], 3);

		return result;
	}

	public static long areaWordHash(long bits, int y) {
		long result = 0;

		long testMask = 0b11 | (0b11 << 16);

		long outputMask = areaWordHashMask(0, y << 1);

		for (int x = 0; x < 8; x++) {
			if ((bits & testMask) != 0) {
				result |= outputMask;
			}

			testMask <<= 2;
			outputMask <<= 1;
		}

		testMask <<= 16;

		for (int x = 0; x < 8; x++) {
			if ((bits & testMask) != 0) {
				result |= outputMask;
			}

			testMask <<= 2;
			outputMask <<= 1;
		}

		//System.out.println(Long.toBinaryString(result));

		return result;
	}

	public static boolean sampleIncludes(long template, long sample) {
		return (template & sample) == template;
	}
}
