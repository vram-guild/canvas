package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Edge.EDGE_BOTTOM;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_BOTTOM_LEFT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_BOTTOM_RIGHT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_LEFT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_RIGHT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_TOP;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_TOP_LEFT;
import static grondag.canvas.chunk.occlusion.Edge.EDGE_TOP_RIGHT;

abstract class AbstractTile {
	protected final Edge e0;
	protected final Edge e1;
	protected final Edge e2;

	protected final TileEdge te0;
	protected final TileEdge te1;
	protected final TileEdge te2;

	protected  AbstractTile(Triangle triangle, int tileSize) {
		e0 = triangle.e0;
		e1 = triangle.e1;
		e2 = triangle.e2;
		te0 = new TileEdge(e0, tileSize);
		te1 = new TileEdge(e1, tileSize);
		te2 = new TileEdge(e2, tileSize);
	}

	/**
	 * Shifts mask 1 pixel towards positive half plane
	 * Use to construct full coverage masks
	 * @param mask
	 * @param edgeFlag
	 * @return
	 */
	protected final long shiftMask(int edgeFlag, long mask) {
		switch  (edgeFlag) {
		case EDGE_TOP:
			return (mask >>> 8);

		case EDGE_RIGHT:
			return (mask >>> 1) & 0x7F7F7F7F7F7F7F7FL;

		case EDGE_LEFT:
			return (mask << 1) & 0xFEFEFEFEFEFEFEFEL;

		case EDGE_TOP_LEFT:
			return (((mask << 1) & 0xFEFEFEFEFEFEFEFEL) >>> 8);

		case EDGE_BOTTOM:
			return mask << 8;

		case EDGE_BOTTOM_LEFT:
			return ((mask << 1) & 0xFEFEFEFEFEFEFEFEL) << 8;

		case EDGE_TOP_RIGHT:
			return ((mask >>> 1) & 0x7F7F7F7F7F7F7F7FL) >>> 8;

		case EDGE_BOTTOM_RIGHT:
			return ((mask >>> 1) & 0x7F7F7F7F7F7F7F7FL) << 8;

		default:
			assert false : "Edge flag out of bounds.";
		return mask;
		}
	}

	public final void moveTo(int tileX, int tileY) {
		te0.moveTo(tileX, tileY);
		te1.moveTo(tileX, tileY);
		te2.moveTo(tileX, tileY);
	}

	protected static final int COVERAGE_NONE = 0;
	protected static final int COVERAGE_PARTIAL = 1;
	// 8 bits away from partial coverage so partial and full results can be accumulated in one word and combined with their respective masks
	protected static final int COVERAGE_FULL = 1 << 8;
}