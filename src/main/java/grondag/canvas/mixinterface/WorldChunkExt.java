package grondag.canvas.mixinterface;

import grondag.canvas.chunk.ChunkColorCache;

public interface WorldChunkExt {
	ChunkColorCache canvas_colorCache();

	void canvas_clearColorCache();
}
