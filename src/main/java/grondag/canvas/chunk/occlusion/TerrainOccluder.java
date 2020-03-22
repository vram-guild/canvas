package grondag.canvas.chunk.occlusion;

import java.io.File;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.resource.ResourceImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.CanvasMod;
import grondag.canvas.render.CanvasWorldRenderer;

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

	//private final Vector4f vec = new Vector4f();
	//	private float xMin;
	//	private float yMin;
	//	private float xMax;
	//	private float yMax;
	//	private float zMin;
	//	private float zMax;
	//
	//	private int x000;
	//	private int x001;
	//	private int x010;
	//	private int x011;
	//	private int x100;
	//	private int x101;
	//	private int x110;
	//	private int x111;
	//
	//	private int y000;
	//	private int y001;
	//	private int y010;
	//	private int y011;
	//	private int y100;
	//	private int y101;
	//	private int y110;
	//	private int y111;

	private final Lazy4f v000 = new Lazy4f();
	private final Lazy4f v001 = new Lazy4f();
	private final Lazy4f v010 = new Lazy4f();
	private final Lazy4f v011 = new Lazy4f();
	private final Lazy4f v100 = new Lazy4f();
	private final Lazy4f v101 = new Lazy4f();
	private final Lazy4f v110 = new Lazy4f();
	private final Lazy4f v111 = new Lazy4f();

	//	private float x000f;
	//	private float x001f;
	//	private float x010f;
	//	private float x011f;
	//	private float x100f;
	//	private float x101f;
	//	private float x110f;
	//	private float x111f;
	//
	//	private float y000f;
	//	private float y001f;
	//	private float y010f;
	//	private float y011f;
	//	private float y100f;
	//	private float y101f;
	//	private float y110f;
	//	private float y111f;
	//
	//	private float z000;
	//	private float z001;
	//	private float z010;
	//	private float z011;
	//	private float z100;
	//	private float z101;
	//	private float z110;
	//	private float z111;

	//	private static final int AXIS_ALL = 0;
	//	private static final int AXIS_X = 1;
	//	private static final int AXIS_Y = 2;
	//	private static final int AXIS_Z = 3;

	//	private static final int X_POSITIVE_FLAG = 1;
	//	private static final int Y_POSITIVE_FLAG = 2;
	//	private static final int Z_POSITIVE_FLAG = 4;

	//	private boolean inFront(float z0, float z1, float z2, float z3) {
	//		return z0 < 1 && z1 < 1 && z2 < 1 && z3 < 1;
	//	}

	// TODO: still needed?
	//	private boolean inView(int x0, int y0, float z0, int x1,  int y1, float z1, int x2, int y2, float z2, int x3, int y3, float z3) {
	//		return inView(x0, y0, z0) || inView(x1, y1, z1) || inView(x2, y2, z2) || inView(x3, y3, z3);
	//	}
	//
	//	private boolean inView(int x, int y, float z) {
	//		return (x | y) >= 0 && x < WIDTH && y < HEIGHT && z <= 1;
	//	}

	//	private void drawY(int faceFlags) {
	//
	//		// TODO: handle case when both face are back-facing
	//		if((faceFlags & Y_POSITIVE_FLAG) == 0) {
	//			drawDown();
	//		} else {
	//			drawUp();
	//		}
	//	}

	private void drawDown() {
		//if (inFront(z000, z100, z101, z001) || inView(x000, y000, z000, x100, y100, z100, x101, y101, z101, x001, y001, z001)) {
		drawQuad(v000, v100, v101, v001);
		//}
	}

	private void drawUp() {
		//if (inFront(z110, z010, z011, z111) || inView(x110, y110, z110, x010, y010, z010, x011, y011, z011, x111, y111, z111)) {
		drawQuad(v110, v010, v011, v111);
		//}
	}

	//	private void drawX(int faceFlags) {
	//		if((faceFlags & X_POSITIVE_FLAG) == 0) {
	//			drawWest();
	//		} else {
	//			drawEast();
	//		}
	//	}

	private void drawEast() {
		//		if (inFront(z101, z100, z110, z111) || inView(x101, y101, z101, x100, y100, z100, x110, y110, z110, x111, y111, z111)) {
		drawQuad(v101, v100, v110, v111);
		//		}
	}

	private void drawWest() {
		//		if (inFront(z000, z001, z011, z010) || inView(x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010)) {
		drawQuad(v000, v001, v011, v010);
		//		}
	}

	//	private void drawZ(int faceFlags) {
	//		if((faceFlags & Z_POSITIVE_FLAG) == 0) {
	//			drawNorth();
	//		} else {
	//			drawSouth();
	//		}
	//	}

	private void drawSouth() {
		//		if (inFront(z001, z101, z111, z011) || inView(x001, y001, z001, x101, y101, z101, x111, y111, z111, x011, y011, z011)) {
		drawQuad(v001, v101, v111, v011);
		//		}
	}

	private void drawNorth() {
		//		if (inFront(z100, z000, z010, z110) || inView(x100, y100, z100, x000, y000, z000, x010, y010, z010, x110, y110, z110)) {
		drawQuad(v100, v000, v010, v110);
		//		}
	}

	//	private boolean testY(int faceFlags) {
	//		return (faceFlags & Y_POSITIVE_FLAG) == 0 ? testDown() : testUp();
	//	}

	private boolean testDown() {
		return testQuad(v000, v100, v101, v001);
	}

	private boolean testUp() {
		return testQuad(v110, v010, v011, v111);
	}

	//	private boolean testX(int faceFlags) {
	//		return (faceFlags & X_POSITIVE_FLAG) == 0 ? testWest() : testEast();
	//	}

	private boolean testEast() {
		return testQuad(v101, v100, v110, v111);
	}

	private boolean testWest() {
		return testQuad(v000, v001, v011, v010);
	}

	//	private boolean testZ(int faceFlags) {
	//		return (faceFlags & Z_POSITIVE_FLAG) == 0 ? testNorth() : testSouth();
	//	}

	private boolean testSouth() {
		return testQuad(v001, v101, v111, v011);
	}

	private boolean testNorth() {
		return testQuad(v100, v000, v010, v110);
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

	//	private int checkAxis(float x0, float y0, float z0, float x1, float y1, float z1) {
	//		if (x0 == x1) {
	//			return AXIS_X;
	//		} else if (y0 == y1) {
	//			return AXIS_Y;
	//		} else if (z0 == z1) {
	//			return AXIS_Z;
	//		} else {
	//			return AXIS_ALL;
	//		}
	//	}

	public boolean isChunkVisible()  {
		CanvasWorldRenderer.innerTimer.start();
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;


		computeProjectedBoxBounds(xo, yo, zo, xo + 16, yo + 16, zo + 16);

		//TODO: remove
		//		if ((xOrigin >> 4) == 6 && (yOrigin >> 4) == 4 && (zOrigin >> 4) == -1) {
		//			System.out.println("boop");
		//		}

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

		CanvasWorldRenderer.innerTimer.stop();

		return east || west || north || south || up || down;
	}

	// TODO: remove
	int tickCounter = 0;

	public boolean isBoxVisible(int packedBox) {
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;

		computeProjectedBoxBounds(
				xo + PackedBox.x0(packedBox),
				yo + PackedBox.y0(packedBox),
				zo + PackedBox.z0(packedBox),
				xo + PackedBox.x1(packedBox),
				yo + PackedBox.y1(packedBox),
				zo + PackedBox.z1(packedBox));

		//return testX(faceFlags) || testZ(faceFlags) || testY(faceFlags);

		//TODO: remove
		//		if ((xOrigin >> 4) == 6 && (yOrigin >> 4) == 4 && (zOrigin >> 4) == -1) {
		//			System.out.println("boop");
		//		}

		final boolean east = testEast();
		final boolean west = testWest();
		final boolean north = testNorth();
		final boolean south = testSouth();
		final boolean up = testUp();
		final boolean down = testDown();

		//		if (xOrigin == 16 && yOrigin == 80 && zOrigin == 0) {
		//			if (--tickCounter <= 0) {
		//				outputRaster();
		//
		//				System.out.println("Camera Pos: " + cameraX + ", " + cameraY + ", " + cameraZ);
		//				System.out.println("Clip plane: " + clipPlane.toString()+ "   globalDist: " + globalClipDistance + "   chunkClipDist: " + chunkClipDistance);
		//				System.out.println("Box: " + PackedBox.toString(packedBox));
		//				System.out.println("origin: " + xOrigin +", " +  yOrigin + ", " + zOrigin);
		//				tickCounter = 200;
		//				//System.out.println(String.format("UP: %f %f %f   %f %f %f    %f %f %f    %f %f %f", x110f, y110f, z110, x010f, y010f, z010, x011f, y011f, z011, x111f, y111f, z111));
		//				System.out.println("E:" + east + "  W:" + west + "  N:" + north + "  S:" + south + "  U:" + up + "  D:" + down);
		//				System.out.println();
		//				//				System.out.println(String.format("UP: %d %d %f   %d %d %f    %d %d %f    %d %d %f", x110, y110, z110, x010, y010, z010, x011, y011, z011, x111, y111, z111));
		//				//				System.out.println("U CCW: " + isCcw(x110, y110, x010, y010, x011, y011));
		//				//				System.out.println("U Tri 1: " + testTri(x110, y110, z110, x010, y010, z010, x011, y011, z011));
		//				//				System.out.println("U Tri 2: " + testTri(x110, y110, z110, x011, y011, z011, x111, y111, z111));
		//				//				System.out.println();
		//				//				System.out.println(String.format("WEST: %d %d %f   %d %d %f    %d %d %f    %d %d %f", x000, y000, z000, x001, y001, z001, x011, y011, z011, x010, y010, z010));
		//				//				System.out.println("W CCW: " + isCcw(x000, y000, x001, y001, x011, y011));
		//				//				System.out.println("W Tri 1: " + testTri(x000, y000, z000, x001, y001, z001, x011, y011, z011));
		//				//				System.out.println("W Tri 2: " + testTri(x000, y000, z000, x011, y011, z011, x010, y010, z010));
		//				//				System.out.println();
		//
		//				final Vector4f a = v110;
		//				final Vector4f b = v010;
		//				final Vector4f c = v011;
		//				final Vector4f d = v111;
		//
		//				System.out.println("UP pre-normalized");
		//				System.out.println("a: " + a.toString());
		//				System.out.println("b: " + b.toString());
		//				System.out.println("c: " + c.toString());
		//				System.out.println("d: " + d.toString());
		//
		//				//testTri2(v110, v010, v011);
		//				System.out.println();
		//
		//				final float fab  = a.getZ() / (a.getZ() - b.getZ());
		//				//System.out.println("a-b weight: " + f);
		//
		//				final float xab = a.getX() + (a.getX() - b.getX()) * fab;
		//				final float yab = a.getY() + (a.getY() - b.getY()) * fab;
		//				final float wab = a.getW() + (a.getW() - b.getW()) * fab;
		//				//System.out.println("xm, xy, xw: " + xm + ", " + ym + ", " + wm);
		//				System.out.println("normalized a: " + a.getX() / a.getW() + ", " + a.getY() / a.getW());
		//				System.out.println("normalized split ab: " + xab / wab + ", " + yab / wab);
		//
		//				final float fbc  = Math.abs(b.getZ()) / (Math.abs(b.getZ()) + Math.abs(c.getZ()));
		//				//System.out.println("b-c weight: " + f);
		//
		//				final float xbc = b.getX() + (b.getX() - c.getX()) * fbc;
		//				final float ybc = b.getY() + (b.getY() - c.getY()) * fbc;
		//				final float wbc = b.getW() + (b.getW() - c.getW()) * fbc;
		//
		//				//System.out.println("xm, xy, xw: " + xm + ", " + ym + ", " + wm);
		//				System.out.println("normalized split bc: " + xbc / wbc + ", " + ybc / wbc);
		//				System.out.println("normalized c: " + c.getX() / c.getW() + ", " + c.getY() / c.getW());
		//				System.out.println("normalized d: " + d.getX() / d.getW() + ", " + d.getY() / d.getW());
		//
		//				System.out.println();
		//
		//				//				System.out.println("ccw: " + isCcw(a.getX() / a.getW(), a.getY() / a.getW(), xab / wab, yab / wab, xbc / wbc, ybc / wbc));
		//
		//
		//			}
		//		}

		return east || west || north || south || up || down;
	}

	public void occludeChunk()  {
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;

		computeProjectedBoxBounds(xo, yo, zo, xo + 16, yo + 16, zo + 16);
		drawUp();
		drawDown();
		drawEast();
		drawWest();
		drawNorth();
		drawSouth();
	}

	public void occlude(int[] visData, int squaredCameraDistance) {

		//		if (!isHacked) {
		//			return;
		//		}


		final int limit= visData.length;
		final int range = squaredCameraDistance > 1024 ? PackedBox.OCCLUSION_RANGE_FAR : squaredCameraDistance < 512 ? PackedBox.OCCLUSION_RANGE_NEAR : PackedBox.OCCLUSION_RANGE_MEDIUM;
		final float xo = offsetX;
		final float yo = offsetY;
		final float zo = offsetZ;

		if (limit > 1) {
			for (int i = 1; i < limit; i++) {
				final int box  = visData[i];

				if (range > PackedBox.range(box)) {
					break;
				}

				//				if (xOrigin == 0 && yOrigin == 0 && zOrigin == 0 && tickCounter == 1) {
				//					System.out.println(PackedBox.toString(box));
				//				}

				occlude(
						xo + PackedBox.x0(box),
						yo + PackedBox.y0(box),
						zo + PackedBox.z0(box),
						xo + PackedBox.x1(box),
						yo + PackedBox.y1(box),
						zo + PackedBox.z1(box));
			}

			//			if (xOrigin == 0 && yOrigin == 0 && zOrigin == 0 && tickCounter == 1) {
			//				System.out.println();
			//			}
		}
	}

	private void occlude(float x0, float y0, float z0, float x1, float y1, float z1) {
		if (isHacked) {
			isHacked = true;
		}

		//		switch (checkAxis(x0, y0, z0, x1, y1, z1)) {
		//		default:
		//		case AXIS_ALL:
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);
		drawUp();
		drawDown();
		drawEast();
		drawWest();
		drawNorth();
		drawSouth();
		//			break;
		//
		//		case AXIS_X:
		//			drawX(computeProjectedXBounds(x0, y0, z0, x1, y1, z1));
		//			break;
		//
		//		case AXIS_Y:
		//			drawY(computeProjectedYBounds(x0, y0, z0, x1, y1, z1));
		//			break;
		//
		//		case AXIS_Z:
		//			drawZ(computeProjectedZBounds(x0, y0, z0, x1, y1, z1));
		//			break;
		//
		//		}
	}


	private void computeProjectedBoxBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
		//		final float x001;
		//		final float x010;
		//		final float x011;
		//		final float x100;
		//		final float x101;
		//		final float x110;
		//		final float x111;
		//
		//		final float y001;
		//		final float y010;
		//		final float y011;
		//		final float y100;
		//		final float y101;
		//		final float y110;
		//		final float y111;

		v000.set(x0, y0, z0, 1);
		v000.transform(mvpMatrix);

		v001.set(x0, y0, z1, 1);
		v001.transform(mvpMatrix);

		v010.set(x0, y1, z0, 1);
		v010.transform(mvpMatrix);

		v011.set(x0, y1, z1, 1);
		v011.transform(mvpMatrix);

		v100.set(x1, y0, z0, 1);
		v100.transform(mvpMatrix);

		v101.set(x1, y0, z1, 1);
		v101.transform(mvpMatrix);

		v110.set(x1, y1, z0, 1);
		v110.transform(mvpMatrix);

		v111.set(x1, y1, z1, 1);
		v111.transform(mvpMatrix);
	}

	//	private int computeProjectedXBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
	//		resetProjectedBounds();
	//
	//		float x000;
	//		float x001;
	//		float x011;
	//
	//		float y000;
	//		float y001;
	//		float y011;
	//
	//		final Vector4f vec = this.vec;
	//
	//		vec.set(x0, y0, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x000 = scaleX(vec.getX());
	//		y000 = scaleY(vec.getY());
	//		z000 = checkZ(vec.getZ());
	//
	//		vec.set(x0, y0, z1, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x001 = scaleX(vec.getX());
	//		y001 = scaleY(vec.getY());
	//		z001 = checkZ(vec.getZ());
	//
	//		vec.set(x0, y1, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x010 = MathHelper.floor(scaleX(vec.getX()));
	//		y010 = MathHelper.floor(scaleY(vec.getY()));
	//		z010 = checkZ(vec.getZ());
	//
	//		vec.set(x0, y1, z1, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x011 = scaleX(vec.getX());
	//		y011 = scaleY(vec.getY());
	//		z011 = checkZ(vec.getZ());
	//
	//		this.x000 = MathHelper.floor(x000);
	//		this.x001 = MathHelper.floor(x001);
	//		this.x011 = MathHelper.floor(x011);
	//
	//		this.y000 = MathHelper.floor(y000);
	//		this.y001 = MathHelper.floor(y001);
	//		this.y011 = MathHelper.floor(y011);
	//
	//		x100 = this.x000;
	//		y100 = this.y000;
	//		z100 = z000;
	//
	//		x101 = this.x001;
	//		y101 = this.y001;
	//		z101 = z001;
	//
	//		x110 = x010;
	//		y110 = y010;
	//		z110 = z010;
	//
	//		x111 = this.x011;
	//		y111 = this.y011;
	//		z111 = z011;
	//
	//		return isCcw(x000, y000, x001, y001, x011, y011) ? 0 : X_POSITIVE_FLAG;
	//	}
	//
	//	private int computeProjectedYBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
	//		resetProjectedBounds();
	//
	//		float x000;
	//		float x100;
	//		float x101;
	//
	//		float y000;
	//		float y100;
	//		float y101;
	//
	//		final Vector4f vec = this.vec;
	//
	//		vec.set(x0, y0, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x000 = scaleX(vec.getX());
	//		y000 = scaleY(vec.getY());
	//		z000 = checkZ(vec.getZ());
	//
	//		vec.set(x0, y0, z1, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x001 = MathHelper.floor(scaleX(vec.getX()));
	//		y001 = MathHelper.floor(scaleY(vec.getY()));
	//		z001 = checkZ(vec.getZ());
	//
	//		vec.set(x1, y0, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x100 = scaleX(vec.getX());
	//		y100 = scaleY(vec.getY());
	//		z100 = checkZ(vec.getZ());
	//
	//		vec.set(x1, y0, z1, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x101 = scaleX(vec.getX());
	//		y101 = scaleY(vec.getY());
	//		z101 = checkZ(vec.getZ());
	//
	//		this.x000 = MathHelper.floor(x000);
	//		this.x100 = MathHelper.floor(x100);
	//		this.x101 = MathHelper.floor(x101);
	//
	//		this.y000 = MathHelper.floor(y000);
	//		this.y100 = MathHelper.floor(y100);
	//		this.y101 = MathHelper.floor(y101);
	//
	//		x010 = this.x000;
	//		y010 = this.y000;
	//		z010 = z000;
	//
	//		x011 = x001;
	//		y011 = y001;
	//		z011 = z001;
	//
	//		x110 = this.x100;
	//		y110 = this.y100;
	//		z110 = z100;
	//
	//		x111 = this.x101;
	//		y111 = this.y101;
	//		z111 = z101;
	//
	//		return isCcw(x000, y000, x100, y100, x101, y101) ? 0 : Y_POSITIVE_FLAG;
	//	}
	//
	//	private int computeProjectedZBounds(float x0, float y0, float z0, float x1, float y1, float z1) {
	//		resetProjectedBounds();
	//
	//		float x000;
	//		float x010;
	//		float x100;
	//
	//		float y000;
	//		float y010;
	//		float y100;
	//
	//		final Vector4f vec = this.vec;
	//
	//		vec.set(x0, y0, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x000 = scaleX(vec.getX());
	//		y000 = scaleY(vec.getY());
	//		z000 = checkZ(vec.getZ());
	//
	//		vec.set(x0, y1, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x010 = scaleX(vec.getX());
	//		y010 = scaleY(vec.getY());
	//		z010 = checkZ(vec.getZ());
	//
	//		vec.set(x1, y0, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x100 = scaleX(vec.getX());
	//		y100 = scaleY(vec.getY());
	//		z100 = checkZ(vec.getZ());
	//
	//		vec.set(x1, y1, z0, 1);
	//		vec.transform(mvpMatrix);
	//		vec.normalizeProjectiveCoordinates();
	//		x110 = MathHelper.floor(scaleX(vec.getX()));
	//		y110 = MathHelper.floor(scaleY(vec.getY()));
	//		z110 = checkZ(vec.getZ());
	//
	//		this.x000 = MathHelper.floor(x000);
	//		this.x010 = MathHelper.floor(x010);
	//		this.x100 = MathHelper.floor(x100);
	//
	//		this.y000 = MathHelper.floor(y000);
	//		this.y010 = MathHelper.floor(y010);
	//		this.y100 = MathHelper.floor(y100);
	//
	//		x001 = this.x000;
	//		y001 = this.y000;
	//		z001 = z000;
	//
	//		x011 = this.x010;
	//		y011 = this.y010;
	//		z011 = z010;
	//
	//		x101 = this.x100;
	//		y101 = this.y100;
	//		z101 = z100;
	//
	//		x111 = x110;
	//		y111 = y110;
	//		z111 = z110;
	//
	//		return isCcw(x100, y100, x000, y000, x010, y010) ? 0 : Z_POSITIVE_FLAG;
	//	}

	private boolean isCcw(Lazy4f v0, Lazy4f v1, Lazy4f v2) {
		return (v1.px() - v0.px()) * (v2.py() - v0.py()) - (v2.px() - v0.px()) * (v1.py() - v0.py()) > 0;
	}

	private void drawQuad(Lazy4f v0, Lazy4f v1, Lazy4f v2, Lazy4f v3) {
		final int split = v0.externalFlag() | (v1.externalFlag() << 1) | (v2.externalFlag() << 2) | (v3.externalFlag() << 3);

		if (split != 0) {
			if (split != 0b1111) {
				drawSplitQuad(split, v0, v1,  v2, v3);
			}
		} else if (isCcw(v0, v1, v2))  {
			drawTri(v0, v1, v2);
			drawTri(v0, v2, v3);
		}
	}

	private void drawSplitQuad(int split, Lazy4f v0, Lazy4f v1, Lazy4f v2, Lazy4f v3) {
		// order so that external vertices come first
		switch (split) {

		// missing one corner, three tris
		case 0b0001:
			drawSplitOne(v1, v2, v3, v0);
			break;
		case 0b0010:
			drawSplitOne(v2, v3, v0, v1);
			break;
		case 0b0100:
			drawSplitOne(v3, v0, v1, v2);
			break;
		case 0b1000:
			drawSplitOne(v0, v1, v2, v3);
			break;

			// missing two corners, two tris
		case 0b0011:
			drawSplitTwo(v1, v2, v3, v0);
			break;
		case 0b0110:
			drawSplitTwo(v2, v3, v0, v1);
			break;
		case 0b1100:
			drawSplitTwo(v3, v0, v1, v2);
			break;
		case 0b1001:
			drawSplitTwo(v0, v1, v2, v3);
			break;

			// missing three corner, one tri
		case 0b0111:
			drawSplitThree(v2, v3, v0);
			break;
		case 0b1110:
			drawSplitThree(v3, v0, v1);
			break;
		case 0b1101:
			drawSplitThree(v0, v1, v2);
			break;
		case 0b1011:
			drawSplitThree(v1, v2, v3);
			break;
		}
	}

	private void drawSplitThree(Lazy4f extA, Lazy4f internal, Lazy4f extB) {
		final Lazy4f va = this.va;
		final Lazy4f vb = this.vb;

		va.interpolateClip(internal, extA);
		vb.interpolateClip(internal, extB);

		drawTri(va, internal, vb);
	}

	private void drawSplitTwo(Lazy4f extA, Lazy4f internal0, Lazy4f internal1, Lazy4f extB) {
		final Lazy4f va = this.va;
		final Lazy4f vb = this.vb;

		va.interpolateClip(internal0, extA);
		vb.interpolateClip(internal1, extB);

		drawTri(va, internal0, internal1);
		drawTri(va, internal1, vb);
	}

	private void drawSplitOne(Lazy4f v0, Lazy4f v1, Lazy4f v2, Lazy4f ext) {
		final Lazy4f va = this.va;
		final Lazy4f vb = this.vb;

		va.interpolateClip(v2, ext);
		vb.interpolateClip(v0, ext);

		drawTri(v0, v1, v2);
		drawTri(v0, v2, va);
		drawTri(v0, va, vb);
	}

	private boolean testQuad(Lazy4f v0, Lazy4f v1, Lazy4f v2, Lazy4f v3) {
		final int split = v0.externalFlag() | (v1.externalFlag() << 1) | (v2.externalFlag() << 2) | (v3.externalFlag() << 3);

		if (split != 0) {
			return split == 0b1111 ? false : testSplitQuad(split, v0, v1,  v2, v3);
		} else if (isCcw(v0, v1, v2))  {
			return testTri(v0, v1, v2) || testTri(v0, v2, v3);
		} else {
			return false;
		}
	}

	final Lazy4f va = new Lazy4f();
	final Lazy4f vb = new Lazy4f();

	private boolean testSplitQuad(int split, Lazy4f v0, Lazy4f v1, Lazy4f v2, Lazy4f v3) {
		// order so that external vertices come first
		switch (split) {

		// missing one corner, three tris
		case 0b0001:
			return testSplitOne(v1, v2, v3, v0);
		case 0b0010:
			return testSplitOne(v2, v3, v0, v1);
		case 0b0100:
			return testSplitOne(v3, v0, v1, v2);
		case 0b1000:
			return testSplitOne(v0, v1, v2, v3);

			// missing two corners, two tris
		case 0b0011:
			return testSplitTwo(v1, v2, v3, v0);
		case 0b0110:
			return testSplitTwo(v2, v3, v0, v1);
		case 0b1100:
			return testSplitTwo(v3, v0, v1, v2);
		case 0b1001:
			return testSplitTwo(v0, v1, v2, v3);

			// missing three corner, one tri
		case 0b0111:
			return testSplitThree(v2, v3, v0);
		case 0b1110:
			return testSplitThree(v3, v0, v1);
		case 0b1101:
			return testSplitThree(v0, v1, v2);
		case 0b1011:
			return testSplitThree(v1, v2, v3);

		default:
			return false;
		}
	}

	private boolean testSplitThree(Lazy4f extA, Lazy4f internal, Lazy4f extB) {
		final Lazy4f va = this.va;
		final Lazy4f vb = this.vb;

		va.interpolateClip(internal, extA);
		vb.interpolateClip(internal, extB);

		return testTri(va, internal, vb);
	}

	private boolean testSplitTwo(Lazy4f extA, Lazy4f internal0, Lazy4f internal1, Lazy4f extB) {
		final Lazy4f va = this.va;
		final Lazy4f vb = this.vb;

		va.interpolateClip(internal0, extA);
		vb.interpolateClip(internal1, extB);

		return testTri(va, internal0, internal1) || testTri(va, internal1, vb);
	}

	private boolean testSplitOne(Lazy4f v0, Lazy4f v1, Lazy4f v2, Lazy4f ext) {
		final Lazy4f va = this.va;
		final Lazy4f vb = this.vb;

		va.interpolateClip(v2, ext);
		vb.interpolateClip(v0, ext);

		return testTri(v0, v1, v2) || testTri(v0, v2, va) || testTri(v0, va, vb);
	}



	private int orient2d(int ax, int ay, int bx, int by, int cx, int cy) {
		return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
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

	int x0;
	int y0;
	int x1;
	int y1;
	int x2;
	int y2;

	private boolean isPointInTri(int x, int y) {
		//final int w0 = (y1 - y0) * (x - x0) + (-x1 + x0) * (y - y0);

		//w0_row = orient2d(x1, y1, x2, y2, minX, minY);
		// int ax = x1, int ay = y1, int bx = x2, int by = y2
		//(bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
		//(x2 - x1) * (y - y1) - (y2 - y1) * (x - x1)
		final int w0 = (x2 - x1) * (y - y1) - (y2 - y1) * (x - x1);

		//final int w1 = (y2 - y1) * (x - x1) + (-x2 + x1) * (y - y1);

		//w1_row = orient2d(x2, y2, x0, y0, minX, minY);
		// int ax = x2, int ay = y2, int bx = x0, int by = y0
		//(bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
		//(x0 - x2) * (y - y2) - (y0 - y2) * (x - x2)
		final int w1 = (x0 - x2) * (y - y2) - (y0 - y2) * (x - x2);

		//final int w2 = (y0 - y2) * (x - x2) + (-x0 + x2) * (y - y2);

		//w2_row = orient2d(x0, y0, x1, y1, minX, minY);
		// int ax = x0, int ay = y0, int bx = x1, int by = y1
		//(bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
		//(x1 - x0) * (y - y0) - (y1 - y0) * (x - x0)

		final int w2 = (x1 - x0) * (y - y0) - (y1 - y0) * (x - x0);



		return (w0 | w1 | w2) >= 0;
	}

	private boolean prepareTriBounds(Lazy4f v0, Lazy4f v1, Lazy4f v2) {
		x0 = v0.ix();
		y0 = v0.iy();
		x1 = v1.ix();
		y1 = v1.iy();
		x2 = v2.ix();
		y2 = v2.iy();

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

	private void drawTri(Lazy4f v0, Lazy4f v1, Lazy4f v2) {
		if (!prepareTriBounds(v0, v1, v2)) {
			return;
		}

		if (maxX - minX < 2 || maxY - minY < 2) {
			return;
		}

		final int x0 = v0.ix();
		final int y0 = v0.iy();
		final int x1 = v1.ix();
		final int y1 = v1.iy();
		final int x2 = v2.ix();
		final int y2 = v2.iy();

		// Triangle setup
		final float dy10 = y0 - y1;
		final float dx01 = x1 - x0;
		final float dy21 = y1 - y2;
		final float dx12 = x2 - x1;
		final float dy02 = y2 - y0;
		final float dx20 = x0 - x2;

		// Barycentric coordinates at start of row
		int w0_row = this.w0_row;
		int w1_row = this.w1_row;
		int w2_row = this.w2_row;

		// Rasterize
		for (int y = minY; y <= maxY; y++) {
			// Barycentric coordinates
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minX; x <= maxX; x++) {
				// If p is on or inside all edges, render pixel.
				//				if (isPointInTri(x, y)) {
				if (w0 > 0 && w1 > 0 && w2 > 0) {
					drawPixel(x, y);
				}

				// One step to the right
				w0 += dy21;
				w1 += dy02;
				w2 += dy10;
			}

			// One row step
			w0_row += dx12;
			w1_row += dx20;
			w2_row += dx01;
		}
	}

	private void drawPixel(int x, int y) {
		//		if (((x & INVERSE_WIDTH_MASK) | (y & INVERSE_HEIGHT_MASK)) == 0) {
		final int addr = (y << SHIFT) | x;
		bits[addr >> 6] |= (1L << (addr & 63));
		//		}
	}

	boolean testTri(Lazy4f v0, Lazy4f v1, Lazy4f v2) {
		if (!prepareTriBounds(v0, v1, v2)) {
			return false;
		}

		if (minX == maxX) {
			if(minY == maxY) {
				return testPixel(minX, minY);
			} else {
				for(int y = minY; y <= maxY; y++) {
					if (testPixel(minX, y)) {
						return true;
					}
				}

				return false;
			}
		} else if (minY == maxY) {
			for(int x = minX; x <= maxX; x++) {
				if (testPixel(x, minY)) {
					return true;
				}
			}

			return false;
		}

		final int x0 = v0.ix();
		final int y0 = v0.iy();
		final int x1 = v1.ix();
		final int y1 = v1.iy();
		final int x2 = v2.ix();
		final int y2 = v2.iy();

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
			//			// Barycentric coordinates at start of row
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minX; x <= maxX; x++) {
				// If p is on or inside all edges, render pixel.
				//				if (isPointInTri(x, y) && testPixel(x, y)) {
				if ((w0 | w1 | w2) >= 0 && testPixel(x, y)) {
					return true;
				}

				//				// One step to the right
				w0 += A12;
				w1 += A20;
				w2 += A01;
			}

			//			// One row step
			w0_row += B12;
			w1_row += B20;
			w2_row += B01;
		}

		return false;
	}

	private boolean testPixel(int x, int y) {
		final int addr = (y << SHIFT) | x;
		return (bits[addr >> 6] & (1L << (addr & 63))) == 0;
	}

	public void outputRaster() {
		if (--tickCounter > 0) {
			return;
		}

		tickCounter = 200;

		final NativeImage nativeImage = new NativeImage(WIDTH, HEIGHT, false);

		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				nativeImage.setPixelRgba(x, y, testPixel(x, y) ? -1 :0xFF000000);
			}
		}

		nativeImage.mirrorVertically();

		final File file = new File(MinecraftClient.getInstance().runDirectory, "occluder.png");

		ResourceImpl.RESOURCE_IO_EXECUTOR.execute(() -> {
			try {
				nativeImage.writeFile(file);
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Couldn't save occluder image", e);
			} finally {
				nativeImage.close();
			}

		});
	}
}
