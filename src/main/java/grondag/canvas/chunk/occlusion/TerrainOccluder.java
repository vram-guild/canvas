package grondag.canvas.chunk.occlusion;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.

public class TerrainOccluder {
	static final int HALF_WIDTH = 128;
	static final int WIDTH = HALF_WIDTH * 2;
	static final int HALF_HEIGHT = 64;
	static final int HEIGHT = HALF_HEIGHT * 2;
	static final int SHIFT = Integer.bitCount(WIDTH - 1);

	static final int WORD_COUNT = WIDTH * HEIGHT / 64;
	static final int INVERSE_WIDTH_MASK = ~(WIDTH - 1);
	static final int INVERSE_HEIGHT_MASK = ~(HEIGHT - 1);

	static final long[] EMPTY_BITS = new long[WORD_COUNT];

	static final long[] bits = new long[WORD_COUNT];

	private Matrix4f mvpMatrix;

	private final Vector4f vec = new Vector4f();
	private float xMin;
	private float yMin;
	private float xMax;
	private float yMax;
	private float zMin;
	private float zMax;

	private int x000;
	private int x001;
	private int x010;
	private int x011;
	private int x100;
	private int x101;
	private int x110;
	private int x111;

	private int y000;
	private int y001;
	private int y010;
	private int y011;
	private int y100;
	private int y101;
	private int y110;
	private int y111;

	private float z000;
	private float z001;
	private float z010;
	private float z011;
	private float z100;
	private float z101;
	private float z110;
	private float z111;

	private static final int AXIS_ALL = 0;
	private static final int AXIS_X = 1;
	private static final int AXIS_Y = 2;
	private static final int AXIS_Z = 3;

	private static final int X_POSITIVE_FLAG = 1;
	private static final int Y_POSITIVE_FLAG = 2;
	private static final int Z_POSITIVE_FLAG = 4;

	private boolean inFront(float z0, float z1, float z2, float z3) {
		return z0 < 1 && z1 < 1 && z2 < 1 && z3 < 1;
	}

	// TODO: still needed?
	private boolean inView(int x0, int y0, float z0, int x1,  int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
		return inView(x0, y0, z0) || inView(x1, y1, z1) || inView(x2, y2, z2) || inView(x3, y3, z3);
	}

	private boolean inView(int x, int y, float z) {
		return (x | y) >= 0 && x < WIDTH && y < HEIGHT && z <= 1;
	}

	private void drawY(int faceFlags) {
		if((faceFlags & Y_POSITIVE_FLAG) == 0) {
			drawDown();
		} else {
			drawUp();
		}
	}

	private void drawDown() {
		if (inFront(z000, z100, z101, z001) || inView(x000, y000, z000, x100, y100, z100, x101, y101, z101, x001, y001, z001)) {
			drawQuad(x000, y000, x100, y100, x101, y101, x001, y001);
		}
	}

	private void drawUp() {
		if (inFront(z110, z010, z011, z111) || inView(x110, y110, z110, x010, y010, z010, x011, y011, z011, x111, y111, z111)) {
			drawQuad(x110, y110, x010, y010, x011, y011, x111, y111);
		}
	}

	private void drawX(int faceFlags) {
		if((faceFlags & X_POSITIVE_FLAG) == 0) {
			drawWest();
		} else {
			drawEast();
		}
	}

	private void drawEast() {
		if (inFront(z101, z100, z110, z111) || inView(x101, y101, z101, x100, y100, z100, x110, y110, z110, x111, y111, z111)) {
			drawQuad(x101, y101, x100, y100, x110, y110, x111, y111);
		}
	}

	private void drawWest() {
		if (inFront(z000, z001, z011, z010) || inView(x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010)) {
			drawQuad(x000, y000, x001, y001, x011, y011, x010, y010);
		}
	}

	private void drawZ(int faceFlags) {
		if((faceFlags & Z_POSITIVE_FLAG) == 0) {
			drawNorth();
		} else {
			drawSouth();
		}
	}

	private void drawSouth() {
		if (inFront(z001, z101, z111, z011) || inView(x001, y001, z001, x101, y101, z101, x111, y111, z111, x011, y011, z011)) {
			drawQuad(x001, y001, x101, y101, x111, y111, x011, y011);
		}
	}

	private void drawNorth() {
		if (inFront(z100, z000, z010, z110) || inView(x100, y100, z100, x000, y000, z000, x010, y010, z010, x110, y110, z110)) {
			drawQuad(x100, y100, x000, y000, x010, y010, x110, y110);
		}
	}

	private boolean testY(int faceFlags) {
		return (faceFlags & Y_POSITIVE_FLAG) == 0 ? testDown() : testUp();
	}

	private boolean testDown() {
		return testQuad(x000, y000, x100, y100, x101, y101, x001, y001);
	}

	private boolean testUp() {
		return testQuad(x110, y110, x010, y010, x011, y011, x111, y111);
	}

	private boolean testX(int faceFlags) {
		return (faceFlags & X_POSITIVE_FLAG) == 0 ? testWest() : testEast();
	}

	private boolean testEast() {
		return testQuad(x101, y101, x100, y100, x110, y110, x111, y111);
	}

	private boolean testWest() {
		return testQuad(x000, y000, x001, y001, x011, y011, x010, y010);
	}

	private boolean testZ(int faceFlags) {
		return (faceFlags & Z_POSITIVE_FLAG) == 0 ? testNorth() : testSouth();
	}

	private boolean testSouth() {
		return testQuad(x001, y001, x101, y101, x111, y111, x011, y011);
	}

	private boolean testNorth() {
		return testQuad(x100, y100, x000, y000, x010, y010, x110, y110);
	}

	public void prepareScene(Matrix4f mvpMatrix) {
		this.mvpMatrix = mvpMatrix;
		System.arraycopy(EMPTY_BITS, 0, bits, 0, WORD_COUNT);
	}

	private int xOrigin;
	private int yOrigin;
	private int zOrigin;

	public void prepareChunk(BlockPos origin) {
		xOrigin = origin.getX();
		yOrigin = origin.getY();
		zOrigin = origin.getZ();
	}

	//	private boolean inFrustum() {
	//		return !(zMin >= 1 || zMax <= 0 || xMin >= 1 || xMax <= -1 || yMin >= 1 || yMax <= -1);
	//	}

	private int checkAxis(int x0, int y0, int z0, int x1, int y1, int z1) {
		if (x0 == x1) {
			return AXIS_X;
		} else if (y0 == y1) {
			return AXIS_Y;
		} else if (z0 == z1) {
			return AXIS_Z;
		} else {
			return AXIS_ALL;
		}
	}

	public boolean isChunkVisible()  {
		final int faceFlags = computeProjectedBoxBounds(xOrigin, yOrigin, zOrigin, xOrigin + 16, yOrigin + 16, zOrigin + 16);
		return testX(faceFlags) || testZ(faceFlags) || testY(faceFlags);
	}

	public boolean isBoxVisible(int packedBox) {
		final int xo = xOrigin;
		final int yo = yOrigin;
		final int zo = zOrigin;

		final int faceFlags = computeProjectedBoxBounds(
				xo + PackedBox.x0(packedBox),
				yo + PackedBox.y0(packedBox),
				zo + PackedBox.z0(packedBox),
				xo + PackedBox.x1(packedBox),
				yo + PackedBox.y1(packedBox),
				zo + PackedBox.z1(packedBox));

		return testX(faceFlags) || testZ(faceFlags) || testY(faceFlags);
	}

	public void occludeChunk()  {
		final int faceFlags = computeProjectedBoxBounds(xOrigin, yOrigin, zOrigin, xOrigin + 16, yOrigin + 16, zOrigin + 16);
		drawX(faceFlags);
		drawY(faceFlags);
		drawZ(faceFlags);
	}

	public void occlude(int x0, int y0, int z0, int x1, int y1, int z1) {
		switch (checkAxis(x0, y0, z0, x1, y1, z1)) {
		default:
		case AXIS_ALL:
			final int faceFlags = computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);
			drawX(faceFlags);
			drawY(faceFlags);
			drawZ(faceFlags);
			break;

		case AXIS_X:
			drawX(computeProjectedXBounds(x0, y0, z0, x1, y1, z1));
			break;

		case AXIS_Y:
			drawY(computeProjectedYBounds(x0, y0, z0, x1, y1, z1));
			break;

		case AXIS_Z:
			drawZ(computeProjectedZBounds(x0, y0, z0, x1, y1, z1));
			break;

		}
	}

	private float scaleX(float val) {
		if (val < xMin) {
			xMin = val;
		} else if (val > xMax) {
			xMax = val;
		}

		return HALF_WIDTH +  val * HALF_WIDTH;
	}

	private float scaleY(float val) {
		if (val < yMin) {
			yMin = val;
		} else if (val > yMax) {
			yMax = val;
		}

		return HALF_HEIGHT +  val * HALF_HEIGHT;
	}

	private float checkZ(float val) {
		if (val < zMin) {
			zMin = val;
		} else if (val > zMax) {
			zMax = val;
		}

		return val;
	}

	private void resetProjectedBounds() {
		xMin = Integer.MAX_VALUE;
		yMin = Integer.MAX_VALUE;
		zMin = Float.MAX_VALUE;
		xMax = Integer.MIN_VALUE;
		yMax = Integer.MIN_VALUE;
		zMax = Float.MIN_VALUE;
	}

	private int computeProjectedBoxBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
		resetProjectedBounds();
		float x001;
		float x010;
		float x011;
		float x100;
		float x101;
		float x110;
		float x111;

		float y001;
		float y010;
		float y011;
		float y100;
		float y101;
		float y110;
		float y111;

		final Vector4f vec = this.vec;

		vec.set(x0, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x000 = MathHelper.floor(scaleX(vec.getX()));
		y000 = MathHelper.floor(scaleY(vec.getY()));
		z000 = checkZ(vec.getZ());

		vec.set(x0, y0, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x001 = scaleX(vec.getX());
		y001 = scaleY(vec.getY());
		z001 = checkZ(vec.getZ());

		vec.set(x0, y1, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x010 = scaleX(vec.getX());
		y010 = scaleY(vec.getY());
		z010 = checkZ(vec.getZ());

		vec.set(x0, y1, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x011 = scaleX(vec.getX());
		y011 = scaleY(vec.getY());
		z011 = checkZ(vec.getZ());

		vec.set(x1, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x100 = scaleX(vec.getX());
		y100 = scaleY(vec.getY());
		z100 = checkZ(vec.getZ());

		vec.set(x1, y0, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x101 = scaleX(vec.getX());
		y101 = scaleY(vec.getY());
		z101 = checkZ(vec.getZ());

		vec.set(x1, y1, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x110 = scaleX(vec.getX());
		y110 = scaleY(vec.getY());
		z110 = checkZ(vec.getZ());

		vec.set(x1, y1, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x111 = scaleX(vec.getX());
		y111 = scaleY(vec.getY());
		z111 = checkZ(vec.getZ());

		this.x001 = MathHelper.floor(x001);
		this.x010 = MathHelper.floor(x010);
		this.x011 = MathHelper.floor(x011);
		this.x100 = MathHelper.floor(x100);
		this.x101 = MathHelper.floor(x101);
		this.x110 = MathHelper.floor(x110);
		this.x111 = MathHelper.floor(x111);

		this.y001 = MathHelper.floor(y001);
		this.y010 = MathHelper.floor(y010);
		this.y011 = MathHelper.floor(y011);
		this.y100 = MathHelper.floor(y100);
		this.y101 = MathHelper.floor(y101);
		this.y110 = MathHelper.floor(y110);
		this.y111 = MathHelper.floor(y111);

		int result = isCcw(x110, y110, x010, y010, x011, y011) ? Y_POSITIVE_FLAG : 0;

		if (isCcw(x101, y101, x100, y100, x110, y110)) {
			result |= X_POSITIVE_FLAG;
		}

		if (isCcw(x001, y001, x101, y101, x111, y111)) {
			result |= Z_POSITIVE_FLAG;
		}

		return result;
	}

	private int computeProjectedXBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
		resetProjectedBounds();

		float x000;
		float x001;
		float x011;

		float y000;
		float y001;
		float y011;

		final Vector4f vec = this.vec;

		vec.set(x0, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x000 = scaleX(vec.getX());
		y000 = scaleY(vec.getY());
		z000 = checkZ(vec.getZ());

		vec.set(x0, y0, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x001 = scaleX(vec.getX());
		y001 = scaleY(vec.getY());
		z001 = checkZ(vec.getZ());

		vec.set(x0, y1, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x010 = MathHelper.floor(scaleX(vec.getX()));
		y010 = MathHelper.floor(scaleY(vec.getY()));
		z010 = checkZ(vec.getZ());

		vec.set(x0, y1, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x011 = scaleX(vec.getX());
		y011 = scaleY(vec.getY());
		z011 = checkZ(vec.getZ());

		this.x000 = MathHelper.floor(x000);
		this.x001 = MathHelper.floor(x001);
		this.x011 = MathHelper.floor(x011);

		this.y000 = MathHelper.floor(y000);
		this.y001 = MathHelper.floor(y001);
		this.y011 = MathHelper.floor(y011);

		x100 = this.x000;
		y100 = this.y000;
		z100 = z000;

		x101 = this.x001;
		y101 = this.y001;
		z101 = z001;

		x110 = x010;
		y110 = y010;
		z110 = z010;

		x111 = this.x011;
		y111 = this.y011;
		z111 = z011;

		return isCcw(x000, y000, x001, y001, x011, y011) ? 0 : X_POSITIVE_FLAG;
	}

	private int computeProjectedYBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
		resetProjectedBounds();

		float x000;
		float x100;
		float x101;

		float y000;
		float y100;
		float y101;

		final Vector4f vec = this.vec;

		vec.set(x0, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x000 = scaleX(vec.getX());
		y000 = scaleY(vec.getY());
		z000 = checkZ(vec.getZ());

		vec.set(x0, y0, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x001 = MathHelper.floor(scaleX(vec.getX()));
		y001 = MathHelper.floor(scaleY(vec.getY()));
		z001 = checkZ(vec.getZ());

		vec.set(x1, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x100 = scaleX(vec.getX());
		y100 = scaleY(vec.getY());
		z100 = checkZ(vec.getZ());

		vec.set(x1, y0, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x101 = scaleX(vec.getX());
		y101 = scaleY(vec.getY());
		z101 = checkZ(vec.getZ());

		this.x000 = MathHelper.floor(x000);
		this.x100 = MathHelper.floor(x100);
		this.x101 = MathHelper.floor(x101);

		this.y000 = MathHelper.floor(y000);
		this.y100 = MathHelper.floor(y100);
		this.y101 = MathHelper.floor(y101);

		x010 = this.x000;
		y010 = this.y000;
		z010 = z000;

		x011 = x001;
		y011 = y001;
		z011 = z001;

		x110 = this.x100;
		y110 = this.y100;
		z110 = z100;

		x111 = this.x101;
		y111 = this.y101;
		z111 = z101;

		return isCcw(x000, y000, x100, y100, x101, y101) ? 0 : Y_POSITIVE_FLAG;
	}

	private int computeProjectedZBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
		resetProjectedBounds();

		float x000;
		float x010;
		float x100;

		float y000;
		float y010;
		float y100;

		final Vector4f vec = this.vec;

		vec.set(x0, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x000 = scaleX(vec.getX());
		y000 = scaleY(vec.getY());
		z000 = checkZ(vec.getZ());

		vec.set(x0, y1, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x010 = scaleX(vec.getX());
		y010 = scaleY(vec.getY());
		z010 = checkZ(vec.getZ());

		vec.set(x1, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x100 = scaleX(vec.getX());
		y100 = scaleY(vec.getY());
		z100 = checkZ(vec.getZ());

		vec.set(x1, y1, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x110 = MathHelper.floor(scaleX(vec.getX()));
		y110 = MathHelper.floor(scaleY(vec.getY()));
		z110 = checkZ(vec.getZ());

		this.x000 = MathHelper.floor(x000);
		this.x010 = MathHelper.floor(x010);
		this.x100 = MathHelper.floor(x100);

		this.y000 = MathHelper.floor(y000);
		this.y010 = MathHelper.floor(y010);
		this.y100 = MathHelper.floor(y100);

		x001 = this.x000;
		y001 = this.y000;
		z001 = z000;

		x011 = this.x010;
		y011 = this.y010;
		z011 = z010;

		x101 = this.x100;
		y101 = this.y100;
		z101 = z100;

		x111 = x110;
		y111 = y110;
		z111 = z110;

		return isCcw(x100, y100, x000, y000, x010, y010) ? 0 : Z_POSITIVE_FLAG;
	}

	private boolean isCcw(float x0, float y0, float x1, float y1, float x2, float y2) {
		return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0) > 0;
	}

	private void drawQuad(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3) {
		drawTri(x0, y0, x1, y1, x2, y2);
		drawTri(x0, y0, x2, y2, x3, y3);
	}

	private boolean testQuad(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3) {
		return testTri(x0, y0, x1, y1, x2, y2) || testTri(x0, y0, x2, y2, x3, y3);
	}

	private int orient2d(int ax, int ay, int bx, int by, int cx, int cy) {
		return (bx-ax)*(cy-ay) - (by-ay)*(cx-ax);
	}

	int minX;
	int minY;
	int maxX;
	int maxY;
	int w0_row;
	int w1_row;
	int w2_row;

	private boolean prepareTriBounds(int x0, int y0, int x1, int y1, int x2, int y2) {


		int minX = x0;
		int maxX = x0;

		if (x1 < minX) {
			minX = x1;
		} else if (x1 > maxX) {
			maxX = x1;
		}

		if (x2 < minX) {
			minX = x2;
		} else if (x2 > maxX) {
			maxX = x2;
		}

		if (maxX < 0 || minX >= WIDTH) {
			return false;
		}

		if (minX < 0) {
			minX = 0;
		}

		if (maxX > WIDTH - 1)  {
			maxX = WIDTH - 1;
		}

		int minY = y0;
		int maxY = y0;

		if (y1 < minY) {
			minY = y1;
		} else if (y1 > maxY) {
			maxY = y1;
		}

		if (y2 < minY) {
			minY = y2;
		} else if (y2 > maxY) {
			maxY = y2;
		}

		if (maxY < 0 || minY >= HEIGHT) {
			return false;
		}

		if (minY < 0) {
			minY = 0;
		}

		if (maxY > HEIGHT - 1)  {
			maxY = HEIGHT - 1;
		}

		// Barycentric coordinates at minX/minY corner
		w0_row = orient2d(x1, y1, x2, y2, minX, minY);
		w1_row = orient2d(x2, y2, x0, y0, minX, minY);
		w2_row = orient2d(x0, y0, x1, y1, minX, minY);

		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;

		return true;
	}

	private void drawTri(int x0, int y0, int x1, int y1, int x2, int y2) {
		if (!prepareTriBounds(x0, y0, x1, y1, x2, y2)) {
			return;
		}

		final int minX = this.minX;
		final int maxX = this.maxX;
		final int minY = this.minY;
		final int maxY = this.maxY;

		// Triangle setup
		final int A01 = y0 - y1;
		final int B01 = x1 - x0;
		final int A12 = y1 - y2;
		final int B12 = x2 - x1;
		final int A20 = y2 - y0;
		final int B20 = x0 - x2;

		// Barycentric coordinates at minX/minY corner
		int w0_row = this.w0_row;
		int w1_row = this.w1_row;
		int w2_row = this.w2_row;

		// Rasterize
		for (int y = minY; y <= maxY; y++) {
			// Barycentric coordinates at start of row
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minX; x <= maxX; x++) {
				// If p is on or inside all edges, render pixel.
				if ((w0 | w1 | w2) >= 0) {
					drawPixel(x, y);
				}

				// One step to the right
				w0 += A12;
				w1 += A20;
				w2 += A01;
			}

			// One row step
			w0_row += B12;
			w1_row += B20;
			w2_row += B01;
		}
	}

	private void drawPixel(int x, int y) {
		//		if (((x & INVERSE_WIDTH_MASK) | (y & INVERSE_HEIGHT_MASK)) == 0) {
		final int addr = (y << SHIFT) | x;
		bits[addr >> 6] |= (1 << (addr & 63));
		//		}
	}

	boolean testTri(int x0, int y0, int x1, int y1, int x2, int y2) {
		if (!prepareTriBounds(x0, y0, x1, y1, x2, y2)) {
			return false;
		}

		final int minX = this.minX;
		final int maxX = this.maxX;
		final int minY = this.minY;
		final int maxY = this.maxY;

		// Triangle setup
		final int A01 = y0 - y1;
		final int B01 = x1 - x0;
		final int A12 = y1 - y2;
		final int B12 = x2 - x1;
		final int A20 = y2 - y0;
		final int B20 = x0 - x2;

		// Barycentric coordinates at minX/minY corner
		int w0_row = this.w0_row;
		int w1_row = this.w1_row;
		int w2_row = this.w2_row;

		// Rasterize
		for (int y = minY; y <= maxY; y++) {
			// Barycentric coordinates at start of row
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minX; x <= maxX; x++) {
				// If p is on or inside all edges, render pixel.
				if ((w0 | w1 | w2) >= 0) {
					if (testPixel(x, y)) {
						return true;
					}
				}

				// One step to the right
				w0 += A12;
				w1 += A20;
				w2 += A01;
			}

			// One row step
			w0_row += B12;
			w1_row += B20;
			w2_row += B01;
		}

		return false;
	}

	private boolean testPixel(int x, int y) {
		//if (((x & INVERSE_WIDTH_MASK) | (y & INVERSE_HEIGHT_MASK)) == 0) {
		final int addr = (y << SHIFT) | x;
		return (bits[addr >> 6] & (1 << (addr & 63))) == 0;
		//		} else {
		//			return false;
		//		}
	}
}
