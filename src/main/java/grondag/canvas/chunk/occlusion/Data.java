package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.LOW_TILE_COUNT;
import static grondag.canvas.chunk.occlusion.Constants.MID_TILE_COUNT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PROJECTED_VERTEX_STRIDE;

import net.minecraft.client.util.math.Matrix4f;

import grondag.canvas.mixinterface.Matrix4fExt;

public class Data {
	static Matrix4f projectionMatrix;
	static Matrix4f modelMatrix;
	static final Matrix4f mvpMatrix = new Matrix4f();
	static final Matrix4fExt mvpMatrixExt =  (Matrix4fExt)(Object) mvpMatrix;

	static final long[] lowTiles = new long[LOW_TILE_COUNT];
	static final long[] midTiles = new long[MID_TILE_COUNT];

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

	static int x0;
	static int y0;
	static int x1;
	static int y1;
	static int x2;
	static int y2;

	static int px0;
	static int py0;
	static int px1;
	static int py1;
	static int px2;
	static int py2;

	static int a0;
	static int b0;
	static int position0;

	static int a1;
	static int b1;
	static int position1;

	static int a2;
	static int b2;
	static int position2;

	static int midTileX;
	static int midTileY;
	static int save_midTileX;
	static int save_midTileY;

	static int lowTileX;
	static int lowTileY;
	static int save_lowTileX;
	static int save_lowTileY;

	static int positionLow;
	static int save_positionLow;
	static int positionHi;
	static int save_positionHi;

	// all coordinates are full precision and corner-oriented unless otherwise noted
	static int lowTileA0;
	static int lowTileB0;
	static int lowSpanA0;
	static int lowSpanB0;
	static int lowExtent0;
	static int lowCornerW0;
	static int save_lowCornerW0;
	static int save_x0y0Low0;

	static int lowTileA1;
	static int lowTileB1;
	static int lowSpanA1;
	static int lowSpanB1;
	static int lowExtent1;
	static int lowCornerW1;
	static int save_lowCornerW1;
	static int save_x0y0Low1;

	static int lowTileA2;
	static int lowTileB2;
	static int lowSpanA2;
	static int lowSpanB2;
	static int lowExtent2;
	static int lowCornerW2;
	static int save_lowCornerW2;
	static int save_x0y0Low2;

	static int hiTileA0;
	static int hiTileB0;
	static int hiSpanA0;
	static int hiSpanB0;
	static int hiExtent0;
	static int hiCornerW0;
	static int positionHi0;
	static int save_hiCornerW0;
	static int save_positionHi0;

	static int hiTileA1;
	static int hiTileB1;
	static int hiSpanA1;
	static int hiSpanB1;
	static int hiExtent1;
	static int hiCornerW1;
	static int positionHi1;
	static int save_hiCornerW1;
	static int save_positionHi1;

	static int hiTileA2;
	static int hiTileB2;
	static int hiSpanA2;
	static int hiSpanB2;
	static int hiExtent2;
	static int hiCornerW2;
	static int positionHi2;
	static int save_hiCornerW2;
	static int save_positionHi2;

	static final short[] event0 = new short[PIXEL_HEIGHT];
	static final short[] event1 = new short[PIXEL_HEIGHT];
	static final short[] event2 = new short[PIXEL_HEIGHT];

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
