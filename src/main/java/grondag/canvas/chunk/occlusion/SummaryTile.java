package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.TileEdge.INSIDE;
import static grondag.canvas.chunk.occlusion.TileEdge.INTERSECTING;
import static grondag.canvas.chunk.occlusion.TileEdge.OUTSIDE;

public abstract class SummaryTile extends AbstractTile {
	protected SummaryTile(Triangle triangle, int tileSize) {
		super(triangle, tileSize);
	}

	protected long fullCoverage;

	/**
	 *
	 * @return mask that inclueds edge coverage.
	 */
	@Override
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
			fullCoverage = -1L;
			return -1L;
		}

		long result = -1L;
		long full = -1L;

		if (c0 == INTERSECTING) {
			final long mask = te0.buildMask();
			result &= mask;
			full &= e0.position.shiftMask(mask);
		}

		if (c1 == INTERSECTING) {
			final long mask = te1.buildMask();
			result &= mask;
			full &= e1.position.shiftMask(mask);
		}

		if (c2 == INTERSECTING) {
			final long mask = te2.buildMask();
			result &= mask;
			full &= e2.position.shiftMask(mask);
		}

		fullCoverage = full;
		return result;
	}
}
