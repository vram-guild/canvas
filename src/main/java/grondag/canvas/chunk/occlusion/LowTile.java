package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.AbstractTerrainOccluder.lowIndex;
import static grondag.canvas.chunk.occlusion.TileEdge.INSIDE;
import static grondag.canvas.chunk.occlusion.TileEdge.INTERSECTING;
import static grondag.canvas.chunk.occlusion.TileEdge.OUTSIDE;

class LowTile extends AbstractTile {
	protected LowTile(Triangle triangle) {
		super(triangle, TerrainOccluder.LOW_BIN_PIXEL_DIAMETER);
	}

	@Override
	public long computeCoverage() {
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

	@Override
	public int tileIndex() {
		return lowIndex(tileX,  tileY);
	}
}