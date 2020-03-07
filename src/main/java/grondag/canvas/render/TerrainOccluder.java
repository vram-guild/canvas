package grondag.canvas.render;

import java.util.BitSet;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.MathHelper;

import grondag.fermion.varia.Useful;

// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.

public class TerrainOccluder {
	static final int HALF_WIDTH = 128;
	static final int WIDTH = HALF_WIDTH * 2;
	static final int HALF_HEIGHT = 64;
	static final int HEIGHT = HALF_HEIGHT * 2;
	static final int SHIFT = Integer.bitCount(WIDTH - 1);

	private final BitSet bits = new BitSet();
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

	private boolean inFront(float z0, float z1, float z2, float z3) {
		return z0 < 1 && z1 < 1 && z2 < 1 && z3 < 1;
	}

	private boolean inView(int x0, int y0, float z0, int x1,  int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
		return inView(x0, y0, z0) || inView(x1, y1, z1) || inView(x2, y2, z2) || inView(x3, y3, z3);
	}

	private boolean inView(int x, int y, float z) {
		return (x | y) >= 0 && x < WIDTH && y < HEIGHT && z <= 1;
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

	private boolean testDown() {
		return testQuad(x000, y000, x100, y100, x101, y101, x001, y001);
	}

	private boolean testUp() {
		return testQuad(x110, y110, x010, y010, x011, y011, x111, y111);
	}

	private boolean testEast() {
		return testQuad(x101, y101, x100, y100, x110, y110, x111, y111);
	}

	private boolean testWest() {
		return testQuad(x000, y000, x001, y001, x011, y011, x010, y010);
	}

	private boolean testSouth() {
		return testQuad(x001, y001, x101, y101, x111, y111, x011, y011);
	}

	private boolean testNorth() {
		return testQuad(x100, y100, x000, y000, x010, y010, x110, y110);
	}

	float viewX;
	float viewY;
	float viewZ;

	public void prepareScene(Matrix4f mvpMatrix, float viewX, float viewY, float viewZ) {
		this.mvpMatrix = mvpMatrix;
		this.viewX = viewX;
		this.viewY = viewY;
		this.viewZ = viewZ;
		bits.clear();
	}

	private boolean inFrustum() {
		return !(zMin >= 1 || zMax <= 0 || xMin >= 1 || xMax <= -1 || yMin >= 1 || yMax <= -1);
	}

	public boolean isVisible(float x0, float y0, float z0, float x1, float y1, float z1) {
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);
		return inFrustum() && (testDown() || testUp() || testEast() || testWest() || testNorth() || testSouth());
	}

	public void occlude(float x0, float y0, float z0, float x1, float y1, float z1) {
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		if (inFrustum()) {
			drawDown();
			drawUp();
			drawEast();
			drawWest();
			drawSouth();
			drawNorth();
		}
	}

	private int scaleX(float val) {
		if (val < xMin) {
			xMin = val;
		} else if (val > xMax) {
			xMax = val;
		}

		return HALF_WIDTH +  MathHelper.floor(val * HALF_WIDTH);
	}

	private int scaleY(float val) {
		if (val < yMin) {
			yMin = val;
		} else if (val > yMax) {
			yMax = val;
		}

		return HALF_HEIGHT +  MathHelper.floor(val * HALF_HEIGHT);
	}

	private float checkZ(float val) {
		if (val < zMin) {
			zMin = val;
		} else if (val > zMax) {
			zMax = val;
		}

		return val;
	}

	private void computeProjectedBoxBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
		xMin = Integer.MAX_VALUE;
		yMin = Integer.MAX_VALUE;
		zMin = Float.MAX_VALUE;
		xMax = Integer.MIN_VALUE;
		yMax = Integer.MIN_VALUE;
		zMax = Float.MIN_VALUE;

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
	}

	/** assume CCW winding order */
	private void drawQuad(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3) {
		drawTri(x0, y0, x1, y1, x2, y2);
		drawTri(x0, y0, x2, y2, x3, y3);
	}

	/** assume CCW winding order */
	private boolean testQuad(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3) {
		return testTri(x0, y0, x1, y1, x2, y2) || testTri(x0, y0, x2, y2, x3, y3);
	}

	private int orient2d(int ax, int ay, int bx, int by, int cx, int cy) {
		return (bx-ax)*(cy-ay) - (by-ay)*(cx-ax);
	}

	private void drawTri(int x0, int y0, int x1, int y1, int x2, int y2) {
		// PERF: optimize comparisons

		// Compute triangle bounding box
		int minX = Useful.min(x0, x1, x2);
		int minY = Useful.min(y0, y1, y2);
		int maxX = Useful.max(x0, x1, x2);
		int maxY = Useful.max(y0, y1, y2);

		if ((maxX | maxY) < 0 || minX >= WIDTH  || minY >= HEIGHT) {
			return;
		}

		// Clip against screen bounds
		minX = Math.max(minX, 0);
		minY = Math.max(minY, 0);
		maxX = Math.min(maxX, WIDTH - 1);
		maxY = Math.min(maxY, HEIGHT - 1);

		// Triangle setup
		final int A01 = y0 - y1;
		final int B01 = x1 - x0;
		final int A12 = y1 - y2;
		final int B12 = x2 - x1;
		final int A20 = y2 - y0;
		final int B20 = x0 - x2;

		// Barycentric coordinates at minX/minY corner
		int w0_row = orient2d(x1, y1, x2, y2, minX, minY);
		int w1_row = orient2d(x2, y2, x0, y0, minX, minY);
		int w2_row = orient2d(x0, y0, x1, y1, minX, minY);

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
		if ((x | y) >=0 && x < WIDTH && y < HEIGHT) {
			bits.set((y << SHIFT) | x);
		}
	}

	boolean testTri(int x0, int y0, int x1, int y1, int x2, int y2) {
		// PERF: optimize comparisons

		// Compute triangle bounding box
		int minX = Useful.min(x0, x1, x2);
		int minY = Useful.min(y0, y1, y2);
		int maxX = Useful.max(x0, x1, x2);
		int maxY = Useful.max(y0, y1, y2);

		//		if ((maxX | maxY) < 0 || minX >= WIDTH  || minY >= HEIGHT) {
		//			return false;
		//		}

		// Clip against screen bounds
		minX = Math.max(minX, 0);
		minY = Math.max(minY, 0);
		maxX = Math.min(maxX, WIDTH - 1);
		maxY = Math.min(maxY, HEIGHT - 1);

		// Triangle setup
		final int A01 = y0 - y1;
		final int B01 = x1 - x0;
		final int A12 = y1 - y2;
		final int B12 = x2 - x1;
		final int A20 = y2 - y0;
		final int B20 = x0 - x2;

		// Barycentric coordinates at minX/minY corner
		int w0_row = orient2d(x1, y1, x2, y2, minX, minY);
		int w1_row = orient2d(x2, y2, x0, y0, minX, minY);
		int w2_row = orient2d(x0, y0, x1, y1, minX, minY);

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
		return (x | y) >=0 && x < WIDTH && y < HEIGHT && !bits.get((y << SHIFT) | x);
	}
}
