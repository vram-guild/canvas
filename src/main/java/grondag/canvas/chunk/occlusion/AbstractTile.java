package grondag.canvas.chunk.occlusion;

abstract class AbstractTile {
	protected final Edge e0;
	protected final Edge e1;
	protected final Edge e2;

	protected final TileEdge te0;
	protected final TileEdge te1;
	protected final TileEdge te2;
	public final int diameter;
	protected final int tileShift;

	protected int tileX;
	protected int tileY;
	protected int completedFlags;

	protected  AbstractTile(Triangle triangle, int tileSize) {
		e0 = triangle.e0;
		e1 = triangle.e1;
		e2 = triangle.e2;
		diameter = tileSize;
		tileShift =  Integer.bitCount(diameter - 1);
		te0 = new TileEdge(e0, this);
		te1 = new TileEdge(e1, this);
		te2 = new TileEdge(e2, this);
	}


	public void prepare() {
		te0.prepare();
		te1.prepare();
		te2.prepare();
	}

	public final void moveTo(int tileX, int tileY) {
		completedFlags = 0;
		this.tileX = tileX << tileShift;
		this.tileY = tileY << tileShift;
	}

	public final void moveRight() {
		++tileX;

		if ((completedFlags & 1) != 0) {
			te0.moveRight();
		}

		if ((completedFlags & 2) != 0) {
			te1.moveRight();
		}

		if ((completedFlags & 4) != 0) {
			te2.moveRight();
		}
	}

	public final void moveLeft() {
		--tileX;

		if ((completedFlags & 1) != 0) {
			te0.moveLeft();
		}

		if ((completedFlags & 2) != 0) {
			te1.moveLeft();
		}

		if ((completedFlags & 4) != 0) {
			te2.moveLeft();
		}
	}

	public final void moveUp() {
		++tileY;

		if ((completedFlags & 1) != 0) {
			te0.moveUp();
		}

		if ((completedFlags & 2) != 0) {
			te1.moveUp();
		}

		if ((completedFlags & 4) != 0) {
			te2.moveUp();
		}
	}

	protected static final int COVERAGE_NONE = 0;
	protected static final int COVERAGE_PARTIAL = 1;
	// 8 bits away from partial coverage so partial and full results can be accumulated in one word and combined with their respective masks
	protected static final int COVERAGE_FULL = 1 << 8;

	public boolean isDirty(int ordinalFlag) {
		if  ((completedFlags & ordinalFlag) == 0) {
			completedFlags |= ordinalFlag;
			return true;
		} else {
			return false;
		}
	}

	protected abstract long computeCoverage();
}