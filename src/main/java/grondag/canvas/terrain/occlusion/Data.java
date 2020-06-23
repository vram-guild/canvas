package grondag.canvas.terrain.occlusion;

import static grondag.canvas.terrain.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.terrain.occlusion.Constants.TILE_COUNT;
import static grondag.canvas.terrain.occlusion.Constants.VERTEX_DATA_LENGTH;

import java.util.concurrent.atomic.AtomicInteger;

public class Data {
	static final Matrix4L baseMvpMatrix = new Matrix4L();
	static final Matrix4L mvpMatrix = new Matrix4L();

	static final int[] events = new int[PIXEL_HEIGHT * 2];
	static final int[] vertexData = new int[VERTEX_DATA_LENGTH];
	static final long[] tiles = new long[TILE_COUNT];

	static long viewX;
	static long viewY;
	static long viewZ;

	static int offsetX;
	static int offsetY;
	static int offsetZ;

	static int occlusionRange;

	static int positionVersion = -1;
	static int viewVersion = -1;
	static int regionVersion = -1;

	static AtomicInteger occluderVersion = new AtomicInteger();
	static boolean forceRedraw = false;
	static boolean needsRedraw = false;

	// Boumds of current triangle - pixel coordinates
	static int minPixelX;
	static int minPixelY;
	static int maxPixelX;
	static int maxPixelY;

	static int clipX0;
	static int clipY0;
	static int clipX1;
	static int clipY1;

	static int position0;
	static int position1;
	static int position2;
	static int position3;

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

	static int minTileOriginX;
	static int maxTileOriginX;
	static int maxTileOriginY;

	static int tileIndex;
	static int tileOriginX;
	static int tileOriginY;
	static int save_tileIndex;
	static int save_tileOriginX;
	static int save_tileOriginY;

	// For abandoned traversal scheme
	//	static final int[] tileEvents = new int[TILE_HEIGHT * 2];
	//	static int upTileIndex;
	//	static int upTileOriginX;
	//	static int upTileOriginY;
}
