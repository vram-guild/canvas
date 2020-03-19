package grondag.canvas.chunk.occlusion;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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

	// TODO: remove
	public boolean isHacked = false;

	private Matrix4f projectionMatrix;
	private Matrix4f modelMatrix;
	private final Matrix4f mvpMatrix = new Matrix4f();

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

	private float x000f;
	private float x001f;
	private float x010f;
	private float x011f;
	private float x100f;
	private float x101f;
	private float x110f;
	private float x111f;

	private float y000f;
	private float y001f;
	private float y010f;
	private float y011f;
	private float y100f;
	private float y101f;
	private float y110f;
	private float y111f;

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

		// TODO: handle case when both face are back-facing
		if((faceFlags & Y_POSITIVE_FLAG) == 0) {
			drawDown();
		} else {
			drawUp();
		}
	}

	private void drawDown() {
		if (inFront(z000, z100, z101, z001) || inView(x000, y000, z000, x100, y100, z100, x101, y101, z101, x001, y001, z001)) {
			drawQuad(x000, y000, z000, x100, y100, z100, x101, y101, z101, x001, y001, z001);
		}
	}

	private void drawUp() {
		if (inFront(z110, z010, z011, z111) || inView(x110, y110, z110, x010, y010, z010, x011, y011, z011, x111, y111, z111)) {
			drawQuad(x110, y110, z110, x010, y010, z010, x011, y011, z011, x111, y111, z111);
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
			drawQuad(x101, y101, z101, x100, y100, z100, x110, y110, z110, x111, y111, z111);
		}
	}

	private void drawWest() {
		if (inFront(z000, z001, z011, z010) || inView(x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010)) {
			drawQuad(x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010);
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
			drawQuad(x001, y001, z001, x101, y101, z101, x111, y111, z111, x011, y011, z011);
		}
	}

	private void drawNorth() {
		if (inFront(z100, z000, z010, z110) || inView(x100, y100, z100, x000, y000, z000, x010, y010, z010, x110, y110, z110)) {
			drawQuad(x100, y100, z100, x000, y000, z000, x010, y010, z010, x110, y110, z110);
		}
	}

	private boolean testY(int faceFlags) {
		return (faceFlags & Y_POSITIVE_FLAG) == 0 ? testDown() : testUp();
	}

	private boolean testDown() {
		return testQuad(x000, y000, z000, x100, y100, z100, x101, y101, z101, x001, y001, z001);
	}

	private boolean testUp() {
		return testQuad(x110, y110, z110, x010, y010, z010, x011, y011, z011, x111, y111, z111);
	}

	private boolean testX(int faceFlags) {
		return (faceFlags & X_POSITIVE_FLAG) == 0 ? testWest() : testEast();
	}

	private boolean testEast() {
		return testQuad(x101, y101, z101, x100, y100, z100, x110, y110, z110, x111, y111, z111);
	}

	private boolean testWest() {
		return testQuad(x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010);
	}

	private boolean testZ(int faceFlags) {
		return (faceFlags & Z_POSITIVE_FLAG) == 0 ? testNorth() : testSouth();
	}

	private boolean testSouth() {
		return testQuad(x001, y001, z001, x101, y101, z101, x111, y111, z111, x011, y011, z011);
	}

	private boolean testNorth() {
		return testQuad(x100, y100, z100, x000, y000, z000, x010, y010, z010, x110, y110, z110);
	}

	public void prepareScene(Matrix4f projectionMatrix, Matrix4f modelMatrix, Camera camera) {
		this.projectionMatrix = projectionMatrix.copy();
		this.modelMatrix = modelMatrix.copy();
		final Vec3d vec3d = camera.getPos();
		cameraX = vec3d.getX();
		cameraY = vec3d.getY();
		cameraZ = vec3d.getZ();
		final Vector3f cp = camera.getHorizontalPlane();
		clipPlane.set(cp.getX(), cp.getY(), cp.getZ());
		globalClipDistance = (float) (cameraX * cp.getX() + cameraY * cp.getY() + cameraZ * cp.getZ());
		System.arraycopy(EMPTY_BITS, 0, bits, 0, WORD_COUNT);
	}

	final Vector3f clipPlane = new Vector3f();

	float globalClipDistance = 0;

	/**
	 * Distance to clipping plane relative to current chunk origin.
	 * Can be applied directly to bounding boxes without adding chunk offset.
	 */
	float chunkClipDistance = 0;

	private int xOrigin;
	private int yOrigin;
	private int zOrigin;

	private double cameraX;
	private double cameraY;
	private double cameraZ;

	private float offsetX;
	private float offsetY;
	private float offsetZ;

	public void prepareChunk(BlockPos origin) {
		xOrigin = origin.getX();
		yOrigin = origin.getY();
		zOrigin = origin.getZ();

		offsetX = (float) (xOrigin - cameraX);
		offsetY = (float) (yOrigin - cameraY);
		offsetZ = (float) (zOrigin - cameraZ);


		//		final Matrix4f modelViewMatrix = modelMatrix.copy();
		//		modelViewMatrix.multiply(Matrix4f.translate(offsetX, offsetY, offsetZ));

		//		final Matrix4f mv = new Matrix4f();
		mvpMatrix.loadIdentity();
		mvpMatrix.multiply(projectionMatrix);
		mvpMatrix.multiply(modelMatrix);
		mvpMatrix.multiply(Matrix4f.translate(offsetX, offsetY, offsetZ));

		//		mvpMatrix = projectionMatrix.copy();
		//		mvpMatrix.multiply(mv);

		//		mvpMatrix = modelMatrix.copy();
		//		mvpMatrix.multiply(Matrix4f.translate(offsetX, offsetY, offsetZ));
		//		mvpMatrix.multiply(projectionMatrix);

		offsetX = 0; //(float) (xOrigin - cameraX);
		offsetY = 0; //(float) (yOrigin - cameraY);
		offsetZ = 0; //(float) (zOrigin - cameraZ);

		chunkClipDistance = (float) ((cameraX - xOrigin) * clipPlane.getX()
				+ (cameraY - yOrigin) * clipPlane.getY()
				+ (cameraZ - zOrigin) * clipPlane.getZ());
		//		offsetX = (float) (cameraX - xOrigin);
		//		offsetY = (float) (cameraY - yOrigin);
		//		offsetZ = (float) (cameraZ - zOrigin);
	}

	//	private boolean inFrustum() {
	//		return !(zMin >= 1 || zMax <= 0 || xMin >= 1 || xMax <= -1 || yMin >= 1 || yMax <= -1);
	//	}

	private int checkAxis(float x0, float y0, float z0, float x1, float y1, float z1) {
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
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;


		final int faceFlags = computeProjectedBoxBounds(xo, yo, zo, xo + 16, yo + 16, zo + 16);

		//		if (xo == 16 && yo == 80 - 16 && zo == 0) {
		//			System.out.println("x000: " + x000);
		//			System.out.println("x001: " + x001);
		//			System.out.println("x010: " + x010);
		//			System.out.println("x011: " + x011);
		//			System.out.println("x100: " + x100);
		//			System.out.println("x101: " + x101);
		//			System.out.println("x110: " + x110);
		//			System.out.println("x111: " + x111);
		//			System.out.println("");
		//			System.out.println("y000: " + y000);
		//			System.out.println("y001: " + y001);
		//			System.out.println("y010: " + y010);
		//			System.out.println("y011: " + y011);
		//			System.out.println("y100: " + y100);
		//			System.out.println("y101: " + y101);
		//			System.out.println("y110: " + y110);
		//			System.out.println("y111: " + y111);
		//			System.out.println("");
		//		}

		//		return testX(faceFlags) || testZ(faceFlags) || testY(faceFlags);

		final boolean east = testEast();
		final boolean west = testWest();
		final boolean north = testNorth();
		final boolean south = testSouth();
		final boolean up = testUp();
		final boolean down = testDown();

		//		if (xo == 16 && yo == 80 - 16 && zo == 0) {
		//			System.out.println("E:" + east + "  W:" + west + "  N:" + north + "  S:" + south + "  U:" + up + "  D:" + down);
		//		}

		return east || west || north || south || up || down;
	}

	// TODO: remove
	int tickCounter = 0;

	public boolean isBoxVisible(int packedBox) {
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;

		// TODO: remove
		//		if (xo == 16 && yo == 80 - 16 && zo == 0) {
		//			System.out.println("boop");
		//		}

		//		final float x0 = xo + PackedBox.x0(packedBox);
		//		final float y0 = yo + PackedBox.y0(packedBox);
		//		final float z0 = zo + PackedBox.z0(packedBox);
		//		final float x1 = xo + PackedBox.x1(packedBox);
		//		final float y1 = yo + PackedBox.y1(packedBox);
		//		final float z1 = zo + PackedBox.z1(packedBox);

		final int faceFlags = computeProjectedBoxBounds(
				xo + PackedBox.x0(packedBox),
				yo + PackedBox.y0(packedBox),
				zo + PackedBox.z0(packedBox),
				xo + PackedBox.x1(packedBox),
				yo + PackedBox.y1(packedBox),
				zo + PackedBox.z1(packedBox));

		//return testX(faceFlags) || testZ(faceFlags) || testY(faceFlags);

		final boolean east = testEast();
		final boolean west = testWest();
		final boolean north = testNorth();
		final boolean south = testSouth();
		final boolean up = testUp();
		final boolean down = testDown();

		if (xOrigin == 16 && yOrigin == 80 && zOrigin == 0) {
			if (--tickCounter <= 0) {
				System.out.println("Camera Pos: " + cameraX + ", " + cameraY + ", " + cameraZ);
				System.out.println("Clip plane: " + clipPlane.toString()+ "   globalDist: " + globalClipDistance + "   chunkClipDist: " + chunkClipDistance);
				System.out.println("Box: " + PackedBox.toString(packedBox));
				System.out.println("origin: " + xOrigin +", " +  yOrigin + ", " + zOrigin);
				tickCounter = 200;
				//System.out.println(String.format("UP: %f %f %f   %f %f %f    %f %f %f    %f %f %f", x110f, y110f, z110, x010f, y010f, z010, x011f, y011f, z011, x111f, y111f, z111));
				System.out.println("E:" + east + "  W:" + west + "  N:" + north + "  S:" + south + "  U:" + up + "  D:" + down);
				System.out.println();
				System.out.println(String.format("UP: %d %d %f   %d %d %f    %d %d %f    %d %d %f", x110, y110, z110, x010, y010, z010, x011, y011, z011, x111, y111, z111));
				System.out.println("U CCW: " + isCcw(x110, y110, x010, y010, x011, y011));
				System.out.println("U Tri 1: " + testTri(x110, y110, z110, x010, y010, z010, x011, y011, z011));
				System.out.println("U Tri 2: " + testTri(x110, y110, z110, x011, y011, z011, x111, y111, z111));
				System.out.println();
				System.out.println(String.format("WEST: %d %d %f   %d %d %f    %d %d %f    %d %d %f", x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010));
				System.out.println("W CCW: " + isCcw(x000, y000, x001, y001, x011, y011));
				System.out.println("W Tri 1: " + testTri(x000, y000, z000, x001, y001, z001, x011, y011, z011));
				System.out.println("W Tri 2: " + testTri(x000, y000, z000, x011, y011, z011, x010, y010, z010));
				System.out.println();
			}
		}

		return east || west || north || south || up || down;
	}

	public void occludeChunk()  {
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;

		final int faceFlags = computeProjectedBoxBounds(xo, yo, zo, xo + 16, yo + 16, zo + 16);
		drawX(faceFlags);
		drawY(faceFlags);
		drawZ(faceFlags);
	}

	public void occlude(int[] visData, int squaredCameraDistance) {

		if (!isHacked) {
			return;
		} else {
			//			System.out.println("(" + xOrigin + "," + yOrigin + "," + zOrigin + ")");
		}

		final int limit= visData.length;
		final int range = squaredCameraDistance > 1024 ? PackedBox.OCCLUSION_RANGE_FAR : squaredCameraDistance < 256 ? PackedBox.OCCLUSION_RANGE_NEAR : PackedBox.OCCLUSION_RANGE_FAR;
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;

		if (limit > 1) {
			for (int i = 1; i < limit; i++) {
				final int box  = visData[i];

				if (range < PackedBox.range(box)) {
					break;
				}

				occlude(
						xo + PackedBox.x0(box),
						yo + PackedBox.y0(box),
						zo + PackedBox.z0(box),
						xo + PackedBox.x1(box),
						yo + PackedBox.y1(box),
						zo + PackedBox.z1(box));
			}
		}
	}

	private void occlude(float x0, float y0, float z0, float x1, float y1, float z1) {
		if (isHacked) {
			isHacked = true;
		}

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

		return HALF_WIDTH + val * HALF_WIDTH;
	}

	private float scaleY(float val) {
		if (val < yMin) {
			yMin = val;
		} else if (val > yMax) {
			yMax = val;
		}

		return HALF_HEIGHT + val * HALF_HEIGHT;
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

	private int computeProjectedBoxBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
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
		x000f = vec.getX();
		y000f = vec.getY();
		z000 = checkZ(vec.getZ());

		vec.set(x0, y0, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x001 = scaleX(vec.getX());
		y001 = scaleY(vec.getY());
		x001f = vec.getX();
		y001f = vec.getY();
		z001 = checkZ(vec.getZ());

		vec.set(x0, y1, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x010 = scaleX(vec.getX());
		y010 = scaleY(vec.getY());
		x010f = vec.getX();
		y010f = vec.getY();
		z010 = checkZ(vec.getZ());

		vec.set(x0, y1, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x011 = scaleX(vec.getX());
		y011 = scaleY(vec.getY());
		x011f = vec.getX();
		y011f = vec.getY();
		z011 = checkZ(vec.getZ());

		vec.set(x1, y0, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x100 = scaleX(vec.getX());
		y100 = scaleY(vec.getY());
		x100f = vec.getX();
		y100f = vec.getY();
		z100 = checkZ(vec.getZ());

		vec.set(x1, y0, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x101 = scaleX(vec.getX());
		y101 = scaleY(vec.getY());
		x101f = vec.getX();
		y101f = vec.getY();
		z101 = checkZ(vec.getZ());

		vec.set(x1, y1, z0, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x110 = scaleX(vec.getX());
		y110 = scaleY(vec.getY());
		x110f = vec.getX();
		y110f = vec.getY();
		z110 = checkZ(vec.getZ());

		vec.set(x1, y1, z1, 1);
		vec.transform(mvpMatrix);
		vec.normalizeProjectiveCoordinates();
		x111 = scaleX(vec.getX());
		y111 = scaleY(vec.getY());
		x111f = vec.getX();
		y111f = vec.getY();
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

	private int computeProjectedXBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
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

	private int computeProjectedYBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
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

	private int computeProjectedZBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
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

	private void drawQuad(int x0, int y0, float z0, int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
		if (isCcw(x0, y0, x1, y1, x2, y2))  {
			drawTri(x0, y0, z0, x1, y1, z1, x2, y2, z2);
			drawTri(x0, y0, z0, x2, y2, z2, x3, y3, z3);
		}
	}

	private boolean testQuad(int x0, int y0, float z0, int x1, int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
		if (isCcw(x0, y0, x1, y1, x2, y2))  {
			return testTri(x0, y0, z0, x1, y1, z1, x2, y2, z2) || testTri(x0, y0, z0, x2, y2, z2, x3, y3, z3);
		} else {
			return false;
		}
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
	float z0;
	float z1;
	float z2;

	private boolean prepareTriBounds(int x0, int y0, float z0, int x1, int y1, float z1, int x2, int y2, float z2) {
		this.z0 = z0;
		this.z1 = z1;
		this.z2 = z2;

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

	private void drawTri(int x0, int y0, float z0, int x1, int y1, float z1, int x2, int y2, float z2) {
		if (!prepareTriBounds(x0, y0, z0, x1, y1, z1, x2, y2, z2)) {
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

	boolean testTri(int x0, int y0, float z0, int x1, int y1, float z1, int x2, int y2, float z2) {
		if (!prepareTriBounds(x0, y0, z0, x1, y1, z1, x2, y2, z2)) {
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
