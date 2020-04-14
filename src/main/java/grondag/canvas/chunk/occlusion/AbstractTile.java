package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.TileEdge.INSIDE;
import static grondag.canvas.chunk.occlusion.TileEdge.INTERSECTING;
import static grondag.canvas.chunk.occlusion.TileEdge.OUTSIDE;
import static grondag.canvas.chunk.occlusion._Data.a0;
import static grondag.canvas.chunk.occlusion._Data.a1;
import static grondag.canvas.chunk.occlusion._Data.a2;
import static grondag.canvas.chunk.occlusion._Data.b0;
import static grondag.canvas.chunk.occlusion._Data.b1;
import static grondag.canvas.chunk.occlusion._Data.b2;
import static grondag.canvas.chunk.occlusion._Data.c0;
import static grondag.canvas.chunk.occlusion._Data.c1;
import static grondag.canvas.chunk.occlusion._Data.c2;
import static grondag.canvas.chunk.occlusion._Data.position0;
import static grondag.canvas.chunk.occlusion._Data.position1;
import static grondag.canvas.chunk.occlusion._Data.position2;

abstract class AbstractTile {
	protected final TileEdge te0;
	protected final TileEdge te1;
	protected final TileEdge te2;
	public final int diameter;
	protected final int tileShift;

	protected int tileX;
	protected int tileY;

	protected int save_tileX;
	protected int save_tileY;
	protected int save_completedFlags;

	protected  AbstractTile(int tileSize) {
		diameter = tileSize;
		tileShift =  Integer.bitCount(diameter - 1);
		te0 = new TileEdge(0, this) {
			@Override
			int a() {
				return a0;
			}

			@Override
			int b() {
				return b0;
			}

			@Override
			int c() {
				return c0;
			}

			@Override
			EdgePosition pos() {
				return position0;
			}
		};

		te1 = new TileEdge(1, this) {
			@Override
			int a() {
				return a1;
			}

			@Override
			int b() {
				return b1;
			}

			@Override
			int c() {
				return c1;
			}

			@Override
			EdgePosition pos() {
				return position1;
			}
		};

		te2 = new TileEdge(2, this) {
			@Override
			int a() {
				return a2;
			}

			@Override
			int b() {
				return b2;
			}

			@Override
			int c() {
				return c2;
			}

			@Override
			EdgePosition pos() {
				return position2;
			}
		};
	}


	public void prepare() {
		te0.prepare();
		te1.prepare();
		te2.prepare();
	}

	public final void moveTo(int tileX, int tileY) {
		te0.makeDirty();
		te1.makeDirty();
		te2.makeDirty();
		this.tileX = tileX;
		this.tileY = tileY;
	}

	public void moveToParentOrigin(AbstractTile parent) {
		tileX = parent.tileX <<  _Constants.TILE_AXIS_SHIFT;
		tileY = parent.tileY <<  _Constants.TILE_AXIS_SHIFT;

		te0.updateFromParent(parent.te0);
		te1.updateFromParent(parent.te1);
		te2.updateFromParent(parent.te2);
	}

	public final void moveRight() {
		++tileX;
		te0.moveRight();
		te1.moveRight();
		te2.moveRight();
	}

	public final void moveLeft() {
		--tileX;
		te0.moveLeft();
		te1.moveLeft();
		te2.moveLeft();
	}

	public final void moveUp() {
		++tileY;
		te0.moveUp();
		te1.moveUp();
		te2.moveUp();
	}

	public final long computeCoverage() {
		final int c0 = te0.position();

		if (c0 == OUTSIDE) {
			return 0L;
		}

		final int c1 = te1.position();

		if (c1 == OUTSIDE) {
			return 0L;
		}

		final int c2 = te2.position();

		if (c2 == OUTSIDE) {
			return 0L;
		}

		if ((c0 | c1 | c2) == INSIDE)  {
			return -1L;
		}

		long result = -1L;

		if (c0 == INTERSECTING) {
			result &= te0.buildMask();
		}

		if (c1 == INTERSECTING) {
			result &= te1.buildMask();
		}

		if (c2 == INTERSECTING) {
			result &= te2.buildMask();
		}

		return result;
	}


	public int x() {
		return tileX << tileShift;
	}

	public int y() {
		return tileY << tileShift;
	}

	public void push() {
		save_tileX = tileX;
		save_tileY = tileY;
		te0.push();
		te1.push();
		te2.push();
	}

	public void pop() {
		tileX = save_tileX;
		tileY = save_tileY;
		te0.pop();
		te1.pop();
		te2.pop();
	}

	public abstract int tileIndex();

	protected static final int COVERAGE_NONE_OR_SOME = 0;
	protected static final int COVERAGE_FULL = 1;
}