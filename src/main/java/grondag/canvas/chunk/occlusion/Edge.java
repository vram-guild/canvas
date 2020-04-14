package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.EdgePosition.BOTTOM;
import static grondag.canvas.chunk.occlusion.EdgePosition.BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.EdgePosition.BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.EdgePosition.LEFT;
import static grondag.canvas.chunk.occlusion.EdgePosition.RIGHT;
import static grondag.canvas.chunk.occlusion.EdgePosition.TOP;
import static grondag.canvas.chunk.occlusion.EdgePosition.TOP_LEFT;
import static grondag.canvas.chunk.occlusion.EdgePosition.TOP_RIGHT;

/** Barycentric coordinates at screen origin */
public final class Edge {
	protected int a;
	protected int b;
	protected int c;
	protected EdgePosition position;
	protected final int ordinal;

	public Edge (int ordinal) {
		this.ordinal = ordinal;
	}

	public void prepare(int a, int b, int c) {
		this.a = a;
		this.b = b;
		this.c = c;
		position = edgePosition(a, b);
	}

	public int compute(int x, int y) {
		return c + a * x + b * y;
	}

	private static EdgePosition edgePosition(int a, int b) {
		if (a == 0) {
			return b > 0 ? BOTTOM : TOP;
		} else if (b == 0) {
			return a > 0 ? LEFT : RIGHT;
		}

		if (a > 0) {
			return b > 0 ? BOTTOM_LEFT : TOP_LEFT;
		}  else { // a < 0
			return b > 0 ? BOTTOM_RIGHT : TOP_RIGHT;
		}
	}
}