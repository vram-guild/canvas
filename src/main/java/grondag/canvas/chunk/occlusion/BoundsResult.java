package grondag.canvas.chunk.occlusion;

public final class BoundsResult {
	private BoundsResult() {}

	public static final int IN_BOUNDS = 0;
	public static final int OUT_OF_BOUNDS_OR_TOO_SMALL = 1;
	public static final int NEEDS_CLIP = 2;
}