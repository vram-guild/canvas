package grondag.canvas.chunk.occlusion;

public abstract class AbstractSummaryTile extends AbstractTile {
	protected AbstractSummaryTile(Triangle triangle, int tileSize) {
		super(triangle, tileSize);
	}

	protected long fullCoverage;
}
