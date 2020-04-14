package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PROJECTED_VERTEX_STRIDE;
import static grondag.canvas.chunk.occlusion._Constants.LOW_TILE_COUNT;
import static grondag.canvas.chunk.occlusion._Constants.LOW_TILE_PIXEL_DIAMETER;
import static grondag.canvas.chunk.occlusion._Constants.MID_TILE_COUNT;
import static grondag.canvas.chunk.occlusion._Constants.MID_TILE_PIXEL_DIAMETER;
import static grondag.canvas.chunk.occlusion._Indexer.lowIndex;
import static grondag.canvas.chunk.occlusion._Indexer.midIndex;

import net.minecraft.client.util.math.Matrix4f;

import grondag.canvas.mixinterface.Matrix4fExt;

public class _Data {
	static Matrix4f projectionMatrix;
	static Matrix4f modelMatrix;
	static final Matrix4f mvpMatrix = new Matrix4f();
	static final Matrix4fExt mvpMatrixExt =  (Matrix4fExt)(Object) mvpMatrix;

	static final long[] lowTiles = new long[LOW_TILE_COUNT];
	static final long[] midTiles = new long[MID_TILE_COUNT];

	static final Edge e0 = new Edge(0);
	static final Edge e1 = new Edge(1);
	static final Edge e2 = new Edge(2);

	static final AbstractTile lowTile = new AbstractTile(LOW_TILE_PIXEL_DIAMETER) {
		@Override
		public int tileIndex() {
			return lowIndex(tileX,  tileY);
		}
	};

	static final AbstractTile midTile = new AbstractTile(MID_TILE_PIXEL_DIAMETER) {
		@Override
		public int tileIndex() {
			return midIndex(tileX,  tileY);
		}
	};

	static int xOrigin;
	static int yOrigin;
	static int zOrigin;

	static double cameraX;
	static double cameraY;
	static double cameraZ;

	static int offsetX;
	static int offsetY;
	static int offsetZ;

	static int occlusionRange;

	// Boumds of current triangle - pixel coordinates
	static int minPixelX;
	static int minPixelY;
	static int maxPixelX;
	static int maxPixelY;
	static int scale;

	// TODO: make static vars instead of array
	static final int V000 = 0;
	static final int V001 = V000 + PROJECTED_VERTEX_STRIDE;
	static final int V010 = V001 + PROJECTED_VERTEX_STRIDE;
	static final int V011 = V010 + PROJECTED_VERTEX_STRIDE;
	static final int V100 = V011 + PROJECTED_VERTEX_STRIDE;
	static final int V101 = V100 + PROJECTED_VERTEX_STRIDE;
	static final int V110 = V101 + PROJECTED_VERTEX_STRIDE;
	static final int V111 = V110 + PROJECTED_VERTEX_STRIDE;

	static final int V_NEAR_CLIP_A = V111 + PROJECTED_VERTEX_STRIDE;
	static final int V_NEAR_CLIP_B = V_NEAR_CLIP_A + PROJECTED_VERTEX_STRIDE;
	static final int V_LOW_X_CLIP_A = V_NEAR_CLIP_B + PROJECTED_VERTEX_STRIDE;
	static final int V_LOW_X_CLIP_B = V_LOW_X_CLIP_A + PROJECTED_VERTEX_STRIDE;
	static final int V_LOW_Y_CLIP_A = V_LOW_X_CLIP_B + PROJECTED_VERTEX_STRIDE;
	static final int V_LOW_Y_CLIP_B = V_LOW_Y_CLIP_A + PROJECTED_VERTEX_STRIDE;
	static final int V_HIGH_X_CLIP_A = V_LOW_Y_CLIP_B + PROJECTED_VERTEX_STRIDE;
	static final int V_HIGH_X_CLIP_B = V_HIGH_X_CLIP_A + PROJECTED_VERTEX_STRIDE;
	static final int V_HIGH_Y_CLIP_A = V_HIGH_X_CLIP_B + PROJECTED_VERTEX_STRIDE;
	static final int V_HIGH_Y_CLIP_B = V_HIGH_Y_CLIP_A + PROJECTED_VERTEX_STRIDE;

	static final int VERTEX_DATA_LENGTH = V_HIGH_Y_CLIP_B + PROJECTED_VERTEX_STRIDE;

	static final int[] vertexData = new int[VERTEX_DATA_LENGTH];

}
