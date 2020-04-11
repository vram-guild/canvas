package grondag.canvas.chunk.occlusion;

/** Barycentric coordinates at screen origin */
public final class Edge {
	protected int a;
	protected int b;
	protected int c;
	protected int shape;
	protected final Triangle triangle;

	public Edge (Triangle triangle) {
		this.triangle = triangle;
	}

	public void prepare(int a, int b, int c) {
		this.a = a;
		this.b = b;
		this.c = c;
		shape = edgeFlag(a, b);
	}

	public int compute(int dx, int dy) {
		return c + a * dx + b * dy;
	}

	private static int edgeFlag(int a, int b) {
		if (a == 0) {
			return b > 0 ? EDGE_BOTTOM : EDGE_TOP;
		} else if (b == 0) {
			return a > 0 ? EDGE_LEFT : EDGE_RIGHT;
		}

		if (a > 0) {
			return b > 0 ? EDGE_BOTTOM_LEFT : EDGE_TOP_LEFT;
		}  else { // a < 0
			return b > 0 ? EDGE_BOTTOM_RIGHT : EDGE_TOP_RIGHT;
		}
	}

	// Edge classifications - refers to position in the triangle.
	// Things above top edge, for example, are outside the edge.
	protected static final int EDGE_TOP = 0;
	protected static final int EDGE_BOTTOM = 1;
	protected static final int EDGE_LEFT = 2;
	protected static final int EDGE_RIGHT = 3;
	protected static final int EDGE_TOP_LEFT = 4;
	protected static final int EDGE_TOP_RIGHT = 5;
	protected static final int EDGE_BOTTOM_LEFT = 6;
	protected static final int EDGE_BOTTOM_RIGHT = 7;
}