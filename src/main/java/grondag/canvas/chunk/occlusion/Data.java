package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.TILE_COUNT;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PROJECTED_VERTEX_STRIDE;

public class Data {
	static Matrix4L projectionMatrixL = new Matrix4L();
	static Matrix4L modelMatrixL = new Matrix4L();
	static final Matrix4L mvpMatrixL = new Matrix4L();

	static final long[] tiles = new long[TILE_COUNT];

	static long viewX;
	static long viewY;
	static long viewZ;

	static int offsetX;
	static int offsetY;
	static int offsetZ;

	static int occlusionRange;

	// Boumds of current triangle - pixel coordinates
	static int minPixelX;
	static int minPixelY;
	static int maxPixelX;
	static int maxPixelY;

	static int clipX0;
	static int clipY0;
	static int clipX1;
	static int clipY1;

	static int minX;
	static int maxX;
	static int minY;
	static int maxY;

	static int ax0;
	static int ay0;
	static int ax1;
	static int ay1;

	static int bx0;
	static int by0;
	static int bx1;
	static int by1;

	static int cx0;
	static int cy0;
	static int cx1;
	static int cy1;

	static int dx0;
	static int dy0;
	static int dx1;
	static int dy1;

	static int position0;
	static int position1;
	static int position2;
	static int position3;

	static int minTileOriginX;
	static int maxTileOriginX;
	static int maxTileOriginY;

	static int tileIndex;
	static int tileOriginX;
	static int tileOriginY;
	static int save_tileIndex;
	static int save_tileOriginX;
	static int save_tileOriginY;

	static final int[] events = new int[PIXEL_HEIGHT * 2];

	//	static int px000;
	//	static int py000;
	//	static float x000;
	//	static float y000;
	//	static float z000;
	//	static float w000;
	//
	//	static int px001;
	//	static int py001;
	//	static float x001;
	//	static float y001;
	//	static float z001;
	//	static float w001;
	//
	//	static int px010;
	//	static int py010;
	//	static float x010;
	//	static float y010;
	//	static float z010;
	//	static float w010;
	//
	//	static int px011;
	//	static int py011;
	//	static float x011;
	//	static float y011;
	//	static float z011;
	//	static float w011;
	//
	//	static int px100;
	//	static int py100;
	//	static float x100;
	//	static float y100;
	//	static float z100;
	//	static float w100;
	//
	//	static int px101;
	//	static int py101;
	//	static float x101;
	//	static float y101;
	//	static float z101;
	//	static float w101;
	//
	//	static int px110;
	//	static int py110;
	//	static float x110;
	//	static float y110;
	//	static float z110;
	//	static float w110;
	//
	//	static int px111;
	//	static int py111;
	//	static float x111;
	//	static float y111;
	//	static float z111;
	//	static float w111;

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
