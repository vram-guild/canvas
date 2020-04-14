package grondag.canvas.chunk.occlusion;

/**
 * Edge classifications - refers to position in the triangle.
 * Pixels above top edge, for example, are outside the edge.
 */
public enum EdgePosition {
	TOP(false, false, true, false),
	BOTTOM(false, false, false, true),
	LEFT(true, false, false, false),
	RIGHT(false, true, false, false),
	TOP_LEFT(true, false, true, false),
	TOP_RIGHT(false, true, true, false),
	BOTTOM_LEFT(true, false, false, true),
	BOTTOM_RIGHT(false, true, false, true);

	public final boolean isRight;
	public final boolean isLeft;
	public final boolean isTop;
	public final boolean isBottom;

	private EdgePosition(boolean isLeft, boolean isRight, boolean isTop, boolean isBottom) {
		this.isRight = isRight;
		this.isLeft = isLeft;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}
}
