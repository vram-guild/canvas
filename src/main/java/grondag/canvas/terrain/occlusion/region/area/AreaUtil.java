package grondag.canvas.terrain.occlusion.region.area;

public class AreaUtil {

	public static int areaKey(int x0, int y0, int x1, int y1) {
		return x0 | (y0 << 4) | (x1 << 8) | (y1 << 12);
	}

	public static int x0(int areaKey) {
		return areaKey & 15;
	}

	public static int y0(int areaKey) {
		return (areaKey >> 4) & 15;
	}

	public static int x1(int areaKey) {
		return (areaKey >> 8) & 15;
	}

	public static int y1(int areaKey) {
		return (areaKey >> 12) & 15;
	}

	public static int size(int areaKey) {
		final int x0 = x0(areaKey);
		final int y0 = y0(areaKey);
		final int x1 = x1(areaKey);
		final int y1 = y1(areaKey);

		return (x1 - x0 + 1) * (y1 - y0 + 1);
	}

	public static int edgeCount(int areaKey) {
		final int x0 = x0(areaKey);
		final int y0 = y0(areaKey);
		final int x1 = x1(areaKey);
		final int y1 = y1(areaKey);

		final int x = x1 - x0 + 1;
		final int y = y1 - y0 + 1;
		return x + y;
	}

	public static void printArea(int areaKey) {
		final int x0 = x0(areaKey);
		final int y0 = y0(areaKey);
		final int x1 = x1(areaKey);
		final int y1 = y1(areaKey);

		final int x = x1 - x0 + 1;
		final int y = y1 - y0 + 1;
		final int a = x * y;
		System.out.println(String.format("%d x %d, area %d, (%d, %d) to (%d, %d)", x, y, a, x0, y0, x1, y1));
	}
}
