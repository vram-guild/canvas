package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.AbstractTerrainOccluder.BIN_AXIS_SHIFT;
import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_FULL;
import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_NONE;
import static grondag.canvas.chunk.occlusion.AbstractTile.COVERAGE_PARTIAL;
import static grondag.canvas.chunk.occlusion.TerrainOccluder.OFFSET_FULL;
import static grondag.canvas.chunk.occlusion.TerrainOccluder.OFFSET_PARTIAL;

public abstract class TileHandler {
	private final AbstractSummaryTile tile;
	private final long[] bins;

	TileHandler(AbstractSummaryTile tile, long[] bins) {
		this.tile = tile;
		this.bins = bins;
	}

	protected abstract int index(final int midX, final int midY);

	protected abstract int drawTriLow(int i, int baseY);

	protected abstract void doFullTiles(int midX, int midY, long l);

	public int drawTri(final int tileX, final int tileY) {
		tile.moveTo(tileX, tileY);
		final long newWordPartial = tile.computeCoverage();
		final int index = index(tileX, tileY) << 1; // shift because two words per index
		final long oldWordPartial = bins[index + OFFSET_PARTIAL];

		if (newWordPartial == 0) {
			return oldWordPartial == 0 ? COVERAGE_NONE : COVERAGE_PARTIAL;
		}

		final long oldWordFull = bins[index + OFFSET_FULL];
		final long newWordFull = tile.fullCoverage;
		long wordFull = oldWordFull;
		long wordPartial = oldWordPartial;

		if (newWordFull != 0) {
			doFullTiles(tileX, tileY, newWordFull & ~oldWordFull);
			wordFull |= newWordFull;
			wordPartial |= newWordFull;

			if (wordFull == -1L) {
				bins[index + OFFSET_FULL] = -1L;
				bins[index + OFFSET_PARTIAL] = -1L;
				return COVERAGE_FULL;
			}
		}

		long coverage = newWordPartial & ~wordFull;
		if (coverage != 0) {

			final int baseX = tileX << BIN_AXIS_SHIFT;
			int baseY = tileY << BIN_AXIS_SHIFT;

			for (int y = 0; y < 8; y++) {
				final int bits = (int) coverage & 0xFF;
				int setBits = 0;

				switch (bits & 0xF) {
				case 0b0000:
					break;

				case 0b0001:
					setBits |= drawTriLow(baseX + 0, baseY);
					break;

				case 0b0010:
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					break;

				case 0b0011:
					setBits |= drawTriLow(baseX + 0, baseY);
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					break;

				case 0b0100:
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					break;

				case 0b0101:
					setBits |= drawTriLow(baseX + 0, baseY);
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					break;

				case 0b0110:
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					break;

				case 0b0111:
					setBits |= drawTriLow(baseX + 0, baseY);
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					break;

				case 0b1000:
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;

				case 0b1001:
					setBits |= drawTriLow(baseX + 0, baseY);
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;

				case 0b1010:
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;

				case 0b1011:
					setBits |= drawTriLow(baseX + 0, baseY);
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;

				case 0b1100:
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;

				case 0b1101:
					setBits |= drawTriLow(baseX + 0, baseY);
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;

				case 0b1110:
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;

				case 0b1111:
					setBits |= drawTriLow(baseX + 0, baseY);
					setBits |= drawTriLow(baseX + 1, baseY) << 1;
					setBits |= drawTriLow(baseX + 2, baseY) << 2;
					setBits |= drawTriLow(baseX + 3, baseY) << 3;
					break;
				}

				switch (bits >> 4) {
				case 0b0000:
					break;

				case 0b0001:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					break;

				case 0b0010:
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					break;

				case 0b0011:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					break;

				case 0b0100:
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					break;

				case 0b0101:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					break;

				case 0b0110:
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					break;

				case 0b0111:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					break;

				case 0b1000:
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;

				case 0b1001:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;

				case 0b1010:
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;

				case 0b1011:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;

				case 0b1100:
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;

				case 0b1101:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;

				case 0b1110:
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;

				case 0b1111:
					setBits |= drawTriLow(baseX + 4, baseY) << 4;
					setBits |= drawTriLow(baseX + 5, baseY) << 5;
					setBits |= drawTriLow(baseX + 6, baseY) << 6;
					setBits |= drawTriLow(baseX + 7, baseY) << 7;
					break;
				}

				if (setBits != 0) {
					final long fullMask = ((long) setBits >> 8) << (y << 3);
					wordFull |= fullMask;
					wordPartial |= fullMask | ((setBits & 0xFFL) << (y << 3));
				}

				++baseY;
				coverage >>= 8;
			}
		}

		wordPartial |= wordFull;
		bins[index + OFFSET_FULL] = wordFull;
		bins[index + OFFSET_PARTIAL] = wordPartial;

		return wordFull == -1L ? COVERAGE_FULL : wordPartial != 0 ? COVERAGE_PARTIAL : COVERAGE_NONE;
	}
}
