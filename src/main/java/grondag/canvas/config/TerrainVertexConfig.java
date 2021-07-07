package grondag.canvas.config;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormats;

public enum TerrainVertexConfig {
	DEFAULT(
		CanvasVertexFormats.COMPACT_MATERIAL,
		CanvasVertexFormats.COMPACT_MATERIAL.quadStrideInts
	),

	FETCH(
		CanvasVertexFormats.VF_MATERIAL,
		// VF quads use vertex stride because of indexing
		CanvasVertexFormats.VF_MATERIAL.vertexStrideInts
	),

	REGION(
		CanvasVertexFormats.REGION_MATERIAL,
		CanvasVertexFormats.REGION_MATERIAL.quadStrideInts
	);

	TerrainVertexConfig(
		CanvasVertexFormat vertexFormat,
		int quadStrideInts
	) {
		this.vertexFormat = vertexFormat;
		this.quadStrideInts = quadStrideInts;
	}

	public final CanvasVertexFormat vertexFormat;

	/** Controls allocation in vertex collectors. */
	public final int quadStrideInts;
}
