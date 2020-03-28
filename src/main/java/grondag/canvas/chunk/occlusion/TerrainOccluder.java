package grondag.canvas.chunk.occlusion;

import java.io.File;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.resource.ResourceImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.render.CanvasWorldRenderer;

// Some elements are adapted from content found at
// https://fgiesen.wordpress.com/2013/02/17/optimizing-sw-occlusion-culling-index/
// by Fabian “ryg” Giesen. That content is in the public domain.

public class TerrainOccluder {
	private final long[] lowBins = new long[LOW_BIN_COUNT];
	private final long[] midBins = new long[MIDDLE_BIN_COUNT];
	private final long[] topBins = new long[TOP_BIN_COUNT];

	private Matrix4f projectionMatrix;
	private Matrix4f modelMatrix;
	private final Matrix4f mvpMatrix = new Matrix4f();

	private final ProjectionVector4f v000 = new ProjectionVector4f();
	private final ProjectionVector4f v001 = new ProjectionVector4f();
	private final ProjectionVector4f v010 = new ProjectionVector4f();
	private final ProjectionVector4f v011 = new ProjectionVector4f();
	private final ProjectionVector4f v100 = new ProjectionVector4f();
	private final ProjectionVector4f v101 = new ProjectionVector4f();
	private final ProjectionVector4f v110 = new ProjectionVector4f();
	private final ProjectionVector4f v111 = new ProjectionVector4f();

	private final ProjectionVector4f vNearClipA = new ProjectionVector4f();
	private final ProjectionVector4f vNearClipB = new ProjectionVector4f();

	private final ProjectionVector4f vClipLowXA = new ProjectionVector4f();
	private final ProjectionVector4f vClipLowXB = new ProjectionVector4f();
	private final ProjectionVector4f vClipLowYA = new ProjectionVector4f();
	private final ProjectionVector4f vClipLowYB = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighXA = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighXB = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighYA = new ProjectionVector4f();
	private final ProjectionVector4f vClipHighYB = new ProjectionVector4f();

	// Boumds of current triangle
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;

	private int minPixelX;
	private int minPixelY;
	private int maxPixelX;
	private int maxPixelY;

	// Barycentric coordinates at minX/minY corner
	private int wOrigin0;
	private int wOrigin1;
	private int wOrigin2;

	private int xOrigin;
	private int yOrigin;
	private int zOrigin;

	private double cameraX;
	private double cameraY;
	private double cameraZ;

	private int x0;
	private int y0;
	private int x1;
	private int y1;
	private int x2;
	private int y2;

	private int a0;
	private int b0;
	private int a1;
	private int b1;
	private int a2;
	private int b2;

	private int xLowStep0;
	private int yLowStep0;
	private int xLowStep1;
	private int yLowStep1;
	private int xLowStep2;
	private int yLowStep2;
	private int xyLowStep0;
	private int xyLowStep1;
	private int xyLowStep2;

	private int xMidStep0;
	private int yMidStep0;
	private int xMidStep1;
	private int yMidStep1;
	private int xMidStep2;
	private int yMidStep2;
	private int xyMidStep0;
	private int xyMidStep1;
	private int xyMidStep2;

	private int xTopStep0;
	private int yTopStep0;
	private int xTopStep1;
	private int yTopStep1;
	private int xTopStep2;
	private int yTopStep2;
	private int xyTopStep0;
	private int xyTopStep1;
	private int xyTopStep2;

	public void prepareScene(Matrix4f projectionMatrix, Matrix4f modelMatrix, Camera camera) {
		this.projectionMatrix = projectionMatrix.copy();
		this.modelMatrix = modelMatrix.copy();
		final Vec3d vec3d = camera.getPos();
		cameraX = vec3d.getX();
		cameraY = vec3d.getY();
		cameraZ = vec3d.getZ();
	}

	public void clearScene() {
		System.arraycopy(EMPTY_BITS, 0, lowBins, 0, LOW_BIN_COUNT);
		System.arraycopy(EMPTY_BITS, 0, midBins, 0, MIDDLE_BIN_COUNT);
		System.arraycopy(EMPTY_BITS, 0, topBins, 0, TOP_BIN_COUNT);
	}

	private float offsetX;
	private float offsetY;
	private float offsetZ;

	public void prepareChunk(BlockPos origin) {
		xOrigin = origin.getX();
		yOrigin = origin.getY();
		zOrigin = origin.getZ();

		// TODO: remove
		if (xOrigin == -16 &&  yOrigin == 0 && zOrigin == 0 ) {
			xOrigin = xOrigin;
		}

		offsetX = (float) (xOrigin - cameraX);
		offsetY = (float) (yOrigin - cameraY);
		offsetZ = (float) (zOrigin - cameraZ);

		mvpMatrix.loadIdentity();
		mvpMatrix.multiply(projectionMatrix);
		mvpMatrix.multiply(modelMatrix);
		mvpMatrix.multiply(Matrix4f.translate(offsetX, offsetY, offsetZ));
	}

	public boolean isChunkVisible()  {
		CanvasWorldRenderer.innerTimer.start();

		computeProjectedBoxBounds(0, 0, 0, 16, 16, 16);

		final boolean result =
				// if camera below top face can't be seen
				(offsetY < -16 && testQuad(v110, v010, v011, v111)) // up
				|| (offsetY > 0 && testQuad(v000, v100, v101, v001)) // down

				|| (offsetX < -16 && testQuad(v101, v100, v110, v111)) // east
				|| (offsetX > 0 && testQuad(v000, v001, v011, v010)) // west

				|| (offsetZ < -16 && testQuad(v001, v101, v111, v011)) // south
				|| (offsetZ > 0 && testQuad(v100, v000, v010, v110)); // north

		CanvasWorldRenderer.innerTimer.stop();

		return result;
	}

	public boolean isBoxVisible(int packedBox) {
		final int x0  = PackedBox.x0(packedBox);
		final int y0  = PackedBox.y0(packedBox);
		final int z0  = PackedBox.z0(packedBox);
		final int x1  = PackedBox.x1(packedBox);
		final int y1  = PackedBox.y1(packedBox);
		final int z1  = PackedBox.z1(packedBox);

		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		// if camera below top face can't be seen
		return (offsetY < -y1 && testQuad(v110, v010, v011, v111)) // up
				|| (offsetY > -y0 && testQuad(v000, v100, v101, v001)) // down

				|| (offsetX < -x1 && testQuad(v101, v100, v110, v111)) // east
				|| (offsetX > -x0 && testQuad(v000, v001, v011, v010)) // west

				|| (offsetZ < -z1 && testQuad(v001, v101, v111, v011)) // south
				|| (offsetZ > -z0 && testQuad(v100, v000, v010, v110)); // north
	}

	public void occludeChunk()  {
		computeProjectedBoxBounds(0, 0, 0, 16, 16, 16);

		if (offsetY < -16) drawQuad(v110, v010, v011, v111); // up
		if (offsetY > 0) drawQuad(v000, v100, v101, v001); // down
		if (offsetX < -16) drawQuad(v101, v100, v110, v111); // east
		if (offsetX > 0) drawQuad(v000, v001, v011, v010); // west
		if (offsetZ < -16) drawQuad(v001, v101, v111, v011); // south
		if (offsetZ > 0) drawQuad(v100, v000, v010, v110); // north
	}

	private void occlude(float x0, float y0, float z0, float x1, float y1, float z1) {
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		if (offsetY < -y1) drawQuad(v110, v010, v011, v111); // up
		if (offsetY > -y0) drawQuad(v000, v100, v101, v001); // down
		if (offsetX < -x1) drawQuad(v101, v100, v110, v111); // east
		if (offsetX > -x0) drawQuad(v000, v001, v011, v010); // west
		if (offsetZ < -z1) drawQuad(v001, v101, v111, v011); // south
		if (offsetZ > -z0) drawQuad(v100, v000, v010, v110); // north
	}

	public void occlude(int[] visData, int range) {
		final int limit= visData.length;

		if (limit > 1) {
			for (int i = 1; i < limit; i++) {
				final int box  = visData[i];

				if (range > PackedBox.range(box)) {
					break;
				}

				occlude(
						PackedBox.x0(box),
						PackedBox.y0(box),
						PackedBox.z0(box),
						PackedBox.x1(box),
						PackedBox.y1(box),
						PackedBox.z1(box));
			}
		}
	}

	private void computeProjectedBoxBounds(float x0, float y0, float z0, float x1, float y1, float z1) {

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

	private void drawQuad(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f v3) {
		final int split = v0.needsNearClip() | (v1.needsNearClip() << 1) | (v2.needsNearClip() << 2) | (v3.needsNearClip() << 3);

		switch (split) {

		// nominal case, all inside
		case 0b0000:
			drawTri(v0, v1, v2);
			drawTri(v0, v2, v3);
			break;

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

		default:
		case 0b1111:
			// all external, draw nothing
			break;
		}
	}

	private void drawSplitThree(ProjectionVector4f extA, ProjectionVector4f internal, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal, extA);
		vb.clipNear(internal, extB);

		drawTri(va, internal, vb);
	}

	private void drawSplitTwo(ProjectionVector4f extA, ProjectionVector4f internal0, ProjectionVector4f internal1, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal0, extA);
		vb.clipNear(internal1, extB);

		drawTri(va, internal0, internal1);
		drawTri(va, internal1, vb);
	}

	private void drawSplitOne(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f ext) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(v2, ext);
		vb.clipNear(v0, ext);

		drawTri(v0, v1, v2);
		drawTri(v0, v2, va);
		drawTri(v0, va, vb);
	}

	private boolean testQuad(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f v3) {
		final int split = v0.needsNearClip() | (v1.needsNearClip() << 1) | (v2.needsNearClip() << 2) | (v3.needsNearClip() << 3);

		switch (split) {

		// nominal case, all inside
		case 0b0000:
			return testTri(v0, v1, v2) || testTri(v0, v2, v3);

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
		case 0b1111:
			// all external, not in view
			return false;
		}
	}

	private boolean testSplitThree(ProjectionVector4f extA, ProjectionVector4f internal, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal, extA);
		vb.clipNear(internal, extB);

		return testTri(va, internal, vb);
	}

	private boolean testSplitTwo(ProjectionVector4f extA, ProjectionVector4f internal0, ProjectionVector4f internal1, ProjectionVector4f extB) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(internal0, extA);
		vb.clipNear(internal1, extB);

		return testTri(va, internal0, internal1) || testTri(va, internal1, vb);
	}

	private boolean testSplitOne(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2, ProjectionVector4f ext) {
		final ProjectionVector4f va = vNearClipA;
		final ProjectionVector4f vb = vNearClipB;

		va.clipNear(v2, ext);
		vb.clipNear(v0, ext);

		return testTri(v0, v1, v2) || testTri(v0, v2, va) || testTri(v0, va, vb);
	}

	private boolean prepareTriBounds(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		final int x0 = v0.ix();
		final int y0 = v0.iy();
		final int x1 = v1.ix();
		final int y1 = v1.iy();
		final int x2 = v2.ix();
		final int y2 = v2.iy();

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

		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;

		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;

		minPixelX = (minX + PRECISION_PIXEL_CENTER - 1) >> PRECISION_BITS;
		minPixelY = (minY + PRECISION_PIXEL_CENTER - 1) >> PRECISION_BITS;
		maxPixelX = (maxX - PRECISION_PIXEL_CENTER) >> PRECISION_BITS;
		maxPixelY = (maxY - PRECISION_PIXEL_CENTER) >> PRECISION_BITS;

		if (maxPixelX < 0 || minPixelX >= PIXEL_WIDTH) {
			return false;
		}

		if (minPixelX < 0) {
			minPixelX = 0;
		}

		if (maxPixelX > PIXEL_WIDTH - 1)  {
			maxPixelX = PIXEL_WIDTH  - 1;
		}

		if (maxPixelY < 0 || minPixelY >= PIXEL_HEIGHT) {
			return false;
		}

		if (minPixelY < 0) {
			minPixelY = 0;
		}

		if (maxPixelY > PIXEL_HEIGHT - 1)  {
			maxPixelY = PIXEL_HEIGHT - 1;
		}

		return true;
	}

	private long orient2d(long x0, long y0, long x1, long y1, long cx, long cy) {
		return ((x1 - x0) * (cy - y0) - (y1 - y0) * (cx - x0));
	}

	private void prepareTriScan() {
		final int x0 = this.x0;
		final int y0 = this.y0;
		final int x1 = this.x1;
		final int y1 = this.y1;
		final int x2 = this.x2;
		final int y2 = this.y2;

		final int a0 = (y1 - y2) << PRECISION_BITS;
		final int b0 = (x2 - x1) << PRECISION_BITS;
		final int a1 = (y2 - y0) << PRECISION_BITS;
		final int b1 = (x0 - x2) << PRECISION_BITS;
		final int a2 = (y0 - y1) << PRECISION_BITS;
		final int b2 = (x1 - x0) << PRECISION_BITS;


		final boolean isTopLeft0 = a0 > 0 || (a0 == 0 && b0 < 0);
		final boolean isTopLeft1 = a1 > 0 || (a1 == 0 && b1 < 0);
		final boolean isTopLeft2 = a2 > 0 || (a2 == 0 && b2 < 0);

		final long cx = (minPixelX << PRECISION_BITS) + PRECISION_PIXEL_CENTER;
		final long cy = (minPixelY << PRECISION_BITS) + PRECISION_PIXEL_CENTER;

		// Barycentric coordinates at minX/minY corner
		wOrigin0 = (int) orient2d(x1, y1, x2, y2, cx, cy) + (isTopLeft0 ? 0 : -1);
		wOrigin1 = (int) orient2d(x2, y2, x0, y0, cx, cy) + (isTopLeft1 ? 0 : -1);
		wOrigin2 = (int) orient2d(x0, y0, x1, y1, cx, cy) + (isTopLeft2 ? 0 : -1);

		this.a0 = a0;
		this.b0 = b0;
		this.a1 = a1;
		this.b1 = b1;
		this.a2 = a2;
		this.b2 = b2;

		xLowStep0 = a0 * 8;
		yLowStep0 = b0 * 8;
		xLowStep1 = a1 * 8;
		yLowStep1 = b1 * 8;
		xLowStep2 = a2 * 8;
		yLowStep2 = b2 * 8;
		xyLowStep0 = xLowStep0 + yLowStep0;
		xyLowStep1 = xLowStep1 + yLowStep1;
		xyLowStep2 = xLowStep2 + yLowStep2;

		xMidStep0 = a0 * 64;
		yMidStep0 = b0 * 64;
		xMidStep1 = a1 * 64;
		yMidStep1 = b1 * 64;
		xMidStep2 = a2 * 64;
		yMidStep2 = b2 * 64;
		xyMidStep0 = xMidStep0 + yMidStep0;
		xyMidStep1 = xMidStep1 + yMidStep1;
		xyMidStep2 = xMidStep2 + yMidStep2;

		xTopStep0 = a0 * 512;
		yTopStep0 = b0 * 512;
		xTopStep1 = a1 * 512;
		yTopStep1 = b1 * 512;
		xTopStep2 = a2 * 512;
		yTopStep2 = b2 * 512;
		xyTopStep0 = xTopStep0 + yTopStep0;
		xyTopStep1 = xTopStep1 + yTopStep1;
		xyTopStep2 = xTopStep2 + yTopStep2;
	}

	@SuppressWarnings("unused")
	private void drawTriReference(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		if (!prepareTriBounds(v0, v1, v2)) {
			return;
		}

		prepareTriScan();

		// Triangle setup
		final int a0 = this.a0;
		final int b0 = this.b0;
		final int a1 = this.a1;
		final int b1 = this.b1;
		final int a2 = this.a2;
		final int b2 = this.b2;

		// Barycentric coordinates at bounding box origin
		int w0_row = wOrigin0;
		int w1_row = wOrigin1;
		int w2_row = wOrigin2;

		// Rasterize
		for (int y = minPixelY; y <= maxPixelY; y++) {
			// Barycentric coordinates
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minPixelX; x <= maxPixelX; x++) {
				if ((w0 | w1 | w2) >= 0) {
					drawPixel(x, y);
				}

				// One step to the right
				w0 += a0;
				w1 += a1;
				w2 += a2;
			}

			// One row step
			w0_row += b0;
			w1_row += b1;
			w2_row += b2;
		}
	}

	private void drawTriFastNew(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		if (!prepareTriBounds(v0, v1, v2)) {
			return;
		}

		// TODO: keep?
		if (maxX - minX < 2 || maxY - minY < 2) {
			return;
		}

		prepareTriScan();

		drawTriInner();
	}

	private void drawTriInner() {
		final int bx0 = minX >> TOP_AXIS_SHIFT;
		final int bx1 = maxX >> TOP_AXIS_SHIFT;
		final int by0 = minY >> TOP_AXIS_SHIFT;
		final int by1 = maxY >> TOP_AXIS_SHIFT;

		if (bx0 == bx1 && by0 == by1) {
			drawTriTop(bx0, by0);
		} else {
			for (int by = by0; by <= by1; by++) {
				for (int bx = bx0; bx <= bx1; bx++) {
					drawTriTop(bx, by);
				}
			}
		}
	}

	private void drawTriTop(int topX, int topY) {
		final int index = (topY << TOP_Y_SHIFT) | topX;
		long word = topBins[index];

		if (word == -1L) {
			// if bin fully occluded nothing to do
			return;
		}

		final int binOriginX = topX << TOP_AXIS_SHIFT;
		final int binOriginY = topY << TOP_AXIS_SHIFT;
		final int minX = Math.max(this.minX, binOriginX);
		final int maxX = Math.min(this.maxX, binOriginX + TOP_BIN_PIXEL_WIDTH - 1);
		final int minY = Math.max(this.minY, binOriginY);
		final int maxY = Math.min(this.maxY, binOriginY + TOP_BIN_PIXEL_WIDTH - 1);

		final int midX0 = minX >>> MID_AXIS_SHIFT;
		final int midX1 = maxX >>> MID_AXIS_SHIFT;
		final int midY0 = minY >>> MID_AXIS_SHIFT;
			final int midY1 = maxY >>> MID_AXIS_SHIFT;

		if (midX0 == midX1 && midY0 == midY1) {
			if (isPixelClear(word, midX0, midY0))  {
				if (drawTriMid(midX0, midY0)) {
					topBins[index] = setPixelInWord(word, midX0, midY0)| word;
				}
			}

			return;
		}

		final int dx = binOriginX - this.minX;
		final int dy = binOriginY - this.minX;
		final int w0_row = wOrigin0 + dx * a0 + dy * b0;
		final int w1_row = wOrigin1 + dx * a1 + dy * b1;
		final int w2_row = wOrigin2 + dx * a2 + dy * b2;

		// if filling whole bin then do it quick
		if (((midX0 | midY0) & 7)== 0 && (midX1 & midY1 & 7) == 7) {
			if ((w0_row | w1_row | w2_row
					| (w0_row + xTopStep0) | (w1_row + xTopStep1) | (w2_row + xTopStep2)
					| (w0_row + yTopStep0) | (w1_row + yTopStep1) | (w2_row + yTopStep2)
					| (w0_row + xyTopStep0) | (w1_row + xyTopStep1) | (w2_row + xyTopStep2)) >= 0) {
				topBins[index] = -1;

				// PERF: disable unless need image output
				fillTopBinChildren(topX, topY);
				return;
			}
		}

		for (int midY = midY0; midY <= midY1; midY++) {
			final int w0 = w0_row;
			final int w1 = w1_row;
			final int w2 = w2_row;

			for (int midX = midX0; midX <= midX1; midX++) {
				if ((w0 | w1 | w2) >= 0 && isPixelClear(word, midX0, midY0))  {
					if (drawTriMid(midX0, midY0)) {
						word |= setPixelInWord(word, midX0, midY0)| word;
					}
				}

				// One step to the right

				//TODO: pick up here
				//				w0 += dy21;
				//				w1 += dy02;
				//				w2 += dy10;

			}
		}

		topBins[index] = word;
	}

	private void fillTopBinChildren(int topX, int topY) {
		final int midX0 = topX << 3;
		final int midY0 = topY << 3;
		final int midX1 = midX0 + 7;
		final int midY1 = midY0 + 7;

		for (int midY = midY0; midY <= midY1; midY++) {
			for (int midX = midX0; midX <= midX1; midX++) {
				final int index = (midY << MID_Y_SHIFT) | midX;

				if (midBins[index] != -1L) {
					midBins[index] = -1L;
					fillMidBinChildren(midX, midY);
				}
			}
		}
	}

	private void fillMidBinChildren(int midX, int midY) {
		final int lowX0 = midX << 3;
		final int lowY0 = midY << 3;
		final int lowX1 = lowX0 + 7;
		final int lowY1 = lowY0 + 7;

		for (int lowY = lowY0; lowY <= lowY1; lowY++) {
			for (int lowX = lowX0; lowX <= lowX1; lowX++) {
				lowBins[(lowY << LOW_Y_SHIFT) | lowX] = -1L;
			}
		}
	}

	private boolean drawTriMid(int bx0, int by0) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean triNeedsClipped() {
		return minX < -GUARD_SIZE || minY < -GUARD_SIZE || maxX > GUARD_WIDTH || maxY > GUARD_HEIGHT;
	}

	private void drawClippedLowX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowX() | (v1.needsClipLowX() << 1) | (v0.needsClipLowX() << 2);

		switch (split) {
		case 0b000:
			drawClippedLowY(v0, v1, v2);
			break;

		case 0b100:
			drawClippedLowXOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedLowXOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedLowXOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedLowXTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedLowXTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedLowXTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedLowXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v1, v0ext);
		vClipLowXB.clipLowX(v2, v0ext);
		drawClippedLowY(vClipLowXA, v1, vClipLowXB);
		drawClippedLowY(vClipLowXB, v1, v2);
	}

	private void drawClippedLowXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v2, v0ext);
		vClipLowXB.clipLowX(v2, v1ext);
		drawClippedLowY(v2, vClipLowXA, vClipLowXB);
	}

	private void drawClippedLowY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowY() | (v1.needsClipLowY() << 1) | (v0.needsClipLowY() << 2);

		switch (split) {
		case 0b000:
			drawClippedHighX(v0, v1, v2);
			break;

		case 0b100:
			drawClippedLowYOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedLowYOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedLowYOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedLowYTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedLowYTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedLowYTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedLowYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v1, v0ext);
		vClipLowYB.clipLowY(v2, v0ext);
		drawClippedHighX(vClipLowYA, v1, vClipLowYB);
		drawClippedHighX(vClipLowYB, v1, v2);
	}

	private void drawClippedLowYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v2, v0ext);
		vClipLowYB.clipLowY(v2, v1ext);
		drawClippedHighX(v2, vClipLowYA, vClipLowYB);
	}

	private void drawClippedHighX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighX() | (v1.needsClipHighX() << 1) | (v0.needsClipHighX() << 2);

		switch (split) {
		case 0b000:
			drawClippedHighY(v0, v1, v2);
			break;

		case 0b100:
			drawClippedHighXOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedHighXOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedHighXOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedHighXTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedHighXTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedHighXTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedHighXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v1, v0ext);
		vClipHighXB.clipHighX(v2, v0ext);
		drawClippedHighY(vClipHighXA, v1, vClipHighXB);
		drawClippedHighY(vClipHighXB, v1, v2);
	}

	private void drawClippedHighXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v2, v0ext);
		vClipHighXB.clipHighX(v2, v1ext);
		drawClippedHighY(v2, vClipHighXA, vClipHighXB);
	}

	private void drawClippedHighY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighY() | (v1.needsClipHighY() << 1) | (v0.needsClipHighY() << 2);

		switch (split) {
		case 0b000:
			drawTri(v0, v1, v2);
			break;

		case 0b100:
			drawClippedHighYOne(v0, v1, v2);
			break;

		case 0b010:
			drawClippedHighYOne(v1, v2, v0);
			break;

		case 0b001:
			drawClippedHighYOne(v2, v0, v1);
			break;

		case 0b110:
			drawClippedHighYTwo(v0, v1, v2);
			break;

		case 0b011:
			drawClippedHighYTwo(v1, v2, v0);
			break;

		case 0b101:
			drawClippedHighYTwo(v2, v0, v1);
			break;

		case 0b111:
			// NOOP
			break;
		}
	}

	private void drawClippedHighYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v1, v0ext);
		vClipHighYB.clipHighY(v2, v0ext);
		drawTri(vClipHighYA, v1, vClipHighYB);
		drawTri(vClipHighYB, v1, v2);
	}

	private void drawClippedHighYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v2, v0ext);
		vClipHighYB.clipHighY(v2, v1ext);
		drawTri(v2, vClipHighYA, vClipHighYB);
	}

	private void drawTri(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		if (!prepareTriBounds(v0, v1, v2)) {
			return;
		}

		if (triNeedsClipped()) {
			drawClippedLowX(v0, v1, v2);
			return;
		}

		prepareTriScan();

		// Triangle setup
		final int px0 = minPixelX;
		final int py0 = minPixelY;
		final int px1 = maxPixelX;
		final int py1 = maxPixelY;

		final int bx0 = px0 >> BIN_AXIS_SHIFT;
		final int bx1 = px1 >> BIN_AXIS_SHIFT;
		final int by0 = py0 >> BIN_AXIS_SHIFT;
		final int by1 = py1 >> BIN_AXIS_SHIFT;


		// Barycentric coordinates at bounding box origin
		int w0_row = wOrigin0;
		int w1_row = wOrigin1;
		int w2_row = wOrigin2;

		for (int by = by0; by <= by1; by++) {
			// Barycentric coordinates
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int bx = bx0; bx <= bx1; bx++) {
				final int x0 = bx == bx0 ? (px0 & BIN_PIXEL_INDEX_MASK) : 0;
				final int y0 = by == by0 ? (py0 & BIN_PIXEL_INDEX_MASK) : 0;
				final int x1 = bx == bx1 ? (px1 & BIN_PIXEL_INDEX_MASK) : 7;
				final int y1 = by == by1 ? (py1 & BIN_PIXEL_INDEX_MASK) : 7;

				drawBin(bx, by, x0, y0, x1, y1, w0, w1, w2);

				// Step to the right
				if (bx == bx0) {
					final int xSteps = 8 - (px0 & BIN_PIXEL_INDEX_MASK);
					w0 += a0 * xSteps;
					w1 += a1 * xSteps;
					w2 += a2 * xSteps;
				} else {
					w0 += xLowStep0;
					w1 += xLowStep1;
					w2 += xLowStep2;
				}
			}

			// Row step
			if (by == by0) {
				final int ySteps = 8 - (py0 & BIN_PIXEL_INDEX_MASK);
				w0_row += b0 * ySteps;
				w1_row += b1 * ySteps;
				w2_row += b2 * ySteps;
			} else {
				w0_row += yLowStep0;
				w1_row += yLowStep1;
				w2_row += yLowStep2;
			}
		}
	}

	private void drawPixel(int x, int y) {
		lowBins[wordIndex(x, y)] |= (1L << (pixelIndex(x, y)));
	}

	private boolean testTri(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		if (!prepareTriBounds(v0, v1, v2)) {
			return false;
		}

		if (triNeedsClipped()) {
			return testClippedLowX(v0, v1, v2);
		}

		if (minPixelX == maxPixelX) {
			if(minPixelY == maxPixelY) {
				return testPixel(minPixelX, minPixelY);
			} else {
				for(int y = minPixelY; y <= maxPixelY; y++) {
					if (testPixel(minPixelX, y)) {
						return true;
					}
				}

				return false;
			}
		} else if (minPixelY == maxPixelY) {
			for(int x = minPixelX; x <= maxPixelX; x++) {
				if (testPixel(x, minPixelY)) {
					return true;
				}
			}

			return false;
		}

		final boolean result = testTriFast(v0, v1, v2);

		return result;
	}

	private boolean testClippedLowX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowX() | (v1.needsClipLowX() << 1) | (v0.needsClipLowX() << 2);

		switch (split) {
		case 0b000:
			return testClippedLowY(v0, v1, v2);

		case 0b100:
			return testClippedLowXOne(v0, v1, v2);

		case 0b010:
			return testClippedLowXOne(v1, v2, v0);

		case 0b001:
			return testClippedLowXOne(v2, v0, v1);

		case 0b110:
			return testClippedLowXTwo(v0, v1, v2);

		case 0b011:
			return testClippedLowXTwo(v1, v2, v0);

		case 0b101:
			return testClippedLowXTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private boolean testClippedLowXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v1, v0ext);
		vClipLowXB.clipLowX(v2, v0ext);
		return testClippedLowY(vClipLowXA, v1, vClipLowXB)
				|| testClippedLowY(vClipLowXB, v1, v2);
	}

	private boolean testClippedLowXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowXA.clipLowX(v2, v0ext);
		vClipLowXB.clipLowX(v2, v1ext);
		return testClippedLowY(v2, vClipLowXA, vClipLowXB);
	}

	private boolean testClippedLowY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipLowY() | (v1.needsClipLowY() << 1) | (v0.needsClipLowY() << 2);

		switch (split) {
		case 0b000:
			return testClippedHighX(v0, v1, v2);

		case 0b100:
			return testClippedLowYOne(v0, v1, v2);

		case 0b010:
			return testClippedLowYOne(v1, v2, v0);

		case 0b001:
			return testClippedLowYOne(v2, v0, v1);

		case 0b110:
			return testClippedLowYTwo(v0, v1, v2);

		case 0b011:
			return testClippedLowYTwo(v1, v2, v0);

		case 0b101:
			return testClippedLowYTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private boolean testClippedLowYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v1, v0ext);
		vClipLowYB.clipLowY(v2, v0ext);
		return testClippedHighX(vClipLowYA, v1, vClipLowYB)
				|| testClippedHighX(vClipLowYB, v1, v2);
	}

	private boolean testClippedLowYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipLowYA.clipLowY(v2, v0ext);
		vClipLowYB.clipLowY(v2, v1ext);
		return testClippedHighX(v2, vClipLowYA, vClipLowYB);
	}

	private boolean testClippedHighX(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighX() | (v1.needsClipHighX() << 1) | (v0.needsClipHighX() << 2);

		switch (split) {
		case 0b000:
			return testClippedHighY(v0, v1, v2);

		case 0b100:
			return testClippedHighXOne(v0, v1, v2);

		case 0b010:
			return testClippedHighXOne(v1, v2, v0);

		case 0b001:
			return testClippedHighXOne(v2, v0, v1);

		case 0b110:
			return testClippedHighXTwo(v0, v1, v2);

		case 0b011:
			return testClippedHighXTwo(v1, v2, v0);

		case 0b101:
			return testClippedHighXTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private boolean testClippedHighXOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v1, v0ext);
		vClipHighXB.clipHighX(v2, v0ext);
		return testClippedHighY(vClipHighXA, v1, vClipHighXB)
				|| testClippedHighY(vClipHighXB, v1, v2);
	}

	private boolean testClippedHighXTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighXA.clipHighX(v2, v0ext);
		vClipHighXB.clipHighX(v2, v1ext);
		return testClippedHighY(v2, vClipHighXA, vClipHighXB);
	}

	private boolean testClippedHighY(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		// NB: order here is lexical not bitwise
		final int split = v2.needsClipHighY() | (v1.needsClipHighY() << 1) | (v0.needsClipHighY() << 2);

		switch (split) {
		case 0b000:
			return testTri(v0, v1, v2);

		case 0b100:
			return testClippedHighYOne(v0, v1, v2);

		case 0b010:
			return testClippedHighYOne(v1, v2, v0);

		case 0b001:
			return testClippedHighYOne(v2, v0, v1);

		case 0b110:
			return testClippedHighYTwo(v0, v1, v2);

		case 0b011:
			return testClippedHighYTwo(v1, v2, v0);

		case 0b101:
			return testClippedHighYTwo(v2, v0, v1);

		default:
		case 0b111:
			return false;
		}
	}

	private boolean testClippedHighYOne(ProjectionVector4f v0ext, ProjectionVector4f v1, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v1, v0ext);
		vClipHighYB.clipHighY(v2, v0ext);
		return testTri(vClipHighYA, v1, vClipHighYB)
				|| testTri(vClipHighYB, v1, v2);
	}

	private boolean testClippedHighYTwo(ProjectionVector4f v0ext, ProjectionVector4f v1ext, ProjectionVector4f v2) {
		vClipHighYA.clipHighY(v2, v0ext);
		vClipHighYB.clipHighY(v2, v1ext);
		return testTri(v2, vClipHighYA, vClipHighYB);
	}

	@SuppressWarnings("unused")
	private boolean testTriReference(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		prepareTriScan();

		// Triangle setup
		final int a0 = this.a0;
		final int b0 = this.b0;
		final int a1 = this.a1;
		final int b1 = this.b1;
		final int a2 = this.a2;
		final int b2 = this.b2;

		// Barycentric coordinates at bounding box origin
		int w0_row = wOrigin0;
		int w1_row = wOrigin1;
		int w2_row = wOrigin2;

		// Rasterize
		for (int y = minPixelY; y <= maxPixelY; y++) {
			// Barycentric coordinates
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minPixelX; x <= maxPixelX; x++) {
				// If p is on or inside all edges, render pixel.
				if ((w0 | w1 | w2) >= 0 && testPixel(x, y)) {
					return true;
				}

				// One step to the right
				w0 += a0;
				w1 += a1;
				w2 += a2;
			}

			// One row step
			w0_row += b0;
			w1_row += b1;
			w2_row += b2;
		}

		return false;
	}

	private boolean testTriFast(ProjectionVector4f v0, ProjectionVector4f v1, ProjectionVector4f v2) {
		prepareTriScan();

		final int px0 = minPixelX;
		final int py0 = minPixelY;
		final int px1 = maxPixelX;
		final int py1 = maxPixelY;

		final int bx0 = px0 >> BIN_AXIS_SHIFT;
		final int bx1 = px1 >> BIN_AXIS_SHIFT;
		final int by0 = py0 >> BIN_AXIS_SHIFT;
		final int by1 = py1 >> BIN_AXIS_SHIFT;

		// Barycentric coordinates at bounding box origin
		int w0_row = wOrigin0;
		int w1_row = wOrigin1;
		int w2_row = wOrigin2;

		for (int by = by0; by <= by1; by++) {
			// Barycentric coordinates
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int bx = bx0; bx <= bx1; bx++) {
				final int x0 = bx == bx0 ? (px0 & BIN_PIXEL_INDEX_MASK) : 0;
				final int y0 = by == by0 ? (py0 & BIN_PIXEL_INDEX_MASK) : 0;
				final int x1 = bx == bx1 ? (px1 & BIN_PIXEL_INDEX_MASK) : 7;
				final int y1 = by == by1 ? (py1 & BIN_PIXEL_INDEX_MASK) : 7;

				if (testBin(bx, by, x0, y0, x1, y1, w0, w1, w2)) {
					return true;
				}

				// Step to the right
				if (bx == bx0) {
					final int xSteps = 8 - (px0 & BIN_PIXEL_INDEX_MASK);
					w0 += a0 * xSteps;
					w1 += a1 * xSteps;
					w2 += a2 * xSteps;
				} else {
					w0 += xLowStep0;
					w1 += xLowStep1;
					w2 += xLowStep2;
				}
			}

			// Row step
			if (by == by0) {
				final int ySteps = 8 - (py0 & BIN_PIXEL_INDEX_MASK);
				w0_row += b0 * ySteps;
				w1_row += b1 * ySteps;
				w2_row += b2 * ySteps;
			} else {
				w0_row += yLowStep0;
				w1_row += yLowStep1;
				w2_row += yLowStep2;
			}
		}

		return false;
	}

	/**
	 *
	 * @param binX bin x index
	 * @param binY bin y index
	 * @param px0 bin-relative min x, 0-7, inclusive
	 * @param py0 bin-relative min y, 0-7, inclusive
	 * @param px1 bin-relative max x, 0-7, inclusive
	 * @param py1 bin-relative max y, 0-7, inclusive
	 * @param wo0 edge weight 0 at x0, y0
	 * @param wo1 edge weight 1 at x0, y0
	 * @param wo2 edge weight 2 at x0, y0
	 * @return
	 */
	private boolean testBin(
			int binX,
			int binY,
			int px0,
			int py0,
			int px1,
			int py1,
			int w0_row,
			int w1_row,
			int w2_row)
	{

		final long word = lowBins[(binY << LOW_Y_SHIFT) | binX];

		if (word == -1L)
			// if bin fully occluded always false
			return false;
		else if ((px0 | py0) == 0 && (px1 & py1) == 7) {
			// testing whole bin

			// if whole bin is inside then any open pixel counts
			// and must have an open pixel if made it to here

			// if whole bin is outside then test must fail

			final int w0 = w0_row;
			final int w1 = w1_row;
			final int w2 = w2_row;

			int flags = (w0 | w1 | w2) >= 0 ? 1 : 0;
			if (((w0 + xLowStep0) | (w1 + xLowStep1) | (w2 + xLowStep2)) >= 0) flags |= 2;
			if (((w0 + yLowStep0) | (w1 + yLowStep1) | (w2 + yLowStep2)) >= 0) flags |= 4;
			if (((w0 + xyLowStep0) | (w1 + xyLowStep1) | (w2 + xyLowStep2)) >= 0) flags |= 8;

			// PERF: need another way to handle corners and sub-bin tris/segments
			//			if(flags == 0) {
			//				// all corners outside
			//				return false;
			//			} else
			if (flags == 15) {
				// all corners inside and bin not fully occluded (per test at top)
				return true;
			}  else if (flags != 0 && word == 0) {
				// at least one corner inside and bin has no occlusion, must be true
				return true;
			}
		}


		// special case optimize for lines and points
		if (px0 == px1) {
			if(py0 == py1) {
				return (w0_row | w1_row | w2_row) >= 0 && testPixelInWordPreMasked(word, px0, py0);
			} else {
				int w0 = w0_row;
				int w1 = w1_row;
				int w2 = w2_row;
				final int b0 = this.b0;
				final int b1 = this.b1;
				final int b2 = this.b2;

				for(int y = py0; y <= py1; y++) {
					if ((w0 | w1 | w2) >= 0  && testPixelInWordPreMasked(word, px0, y)) {
						return true;
					}

					// One row step
					w0 += b0;
					w1 += b1;
					w2 += b2;
				}

				return false;
			}
		} else if (py0 == py1) {
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;
			final int a0 = this.a0;
			final int a1 = this.a1;
			final int a2 = this.a2;

			for(int x = px0; x <= px1; x++) {
				if ((w0 | w1 | w2) >= 0  && testPixelInWordPreMasked(word, x, py0)) {
					return true;
				}

				// One step to the right
				w0 += a0;
				w1 += a1;
				w2 += a2;
			}

			return false;
		}  else {
			final int a0 = this.a0;
			final int b0 = this.b0;
			final int a1 = this.a1;
			final int b1 = this.b1;
			final int a2 = this.a2;
			final int b2 = this.b2;

			// Rasterize
			for (int y = py0; y <= py1; y++) {
				int w0 = w0_row;
				int w1 = w1_row;
				int w2 = w2_row;

				for (int x = px0; x <= px1; x++) {
					// If p is on or inside all edges, render pixel.
					if ((w0 | w1 | w2) >= 0 && testPixelInWordPreMasked(word, x, y)) {
						return true;
					}

					// One step to the right
					w0 += a0;
					w1 += a1;
					w2 += a2;
				}

				// One row step
				w0_row += b0;
				w1_row += b1;
				w2_row += b2;
			}

			return false;
		}
	}

	/**
	 *
	 * @param binX bin x index
	 * @param binY bin y index
	 * @param px0 bin-relative min x, 0-7, inclusive
	 * @param py0 bin-relative min y, 0-7, inclusive
	 * @param px1 bin-relative max x, 0-7, inclusive
	 * @param py1 bin-relative max y, 0-7, inclusive
	 * @param wo0 edge weight 0 at x0, y0
	 * @param wo1 edge weight 1 at x0, y0
	 * @param wo2 edge weight 2 at x0, y0
	 * @return
	 */
	private void drawBin(
			int binX,
			int binY,
			int px0,
			int py0,
			int px1,
			int py1,
			int w0_row,
			int w1_row,
			int w2_row)
	{
		final int index = (binY << LOW_Y_SHIFT) | binX;
		long word = lowBins[index];

		if (word == -1L) {
			// if bin fully occluded nothing to do
			return;
		}

		// special case optimize for lines and points
		if (px0 == px1) {
			if(py0 == py1) {
				//				if (w0_row > 0 && w1_row > 0 && w2_row > 0)  {
				if ((w0_row | w1_row | w2_row) >= 0) {
					lowBins[index] = setPixelInWordPreMasked(word, px0, py0);
				}

				return;

			} else {
				int w0 = w0_row;
				int w1 = w1_row;
				int w2 = w2_row;
				final int b0 = this.b0;
				final int b1 = this.b1;
				final int b2 = this.b2;

				for(int y = py0; y <= py1; y++) {
					//					if (w0 > 0 && w1 > 0 && w2 > 0) {
					if ((w0 | w1 | w2) >= 0) {
						word = setPixelInWordPreMasked(word, px0, y);
					}

					// One row step
					w0 += b0;
					w1 += b1;
					w2 += b2;
				}

				lowBins[index] = word;
				return;
			}
		} else if (py0 == py1) {
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;
			final int a0 = this.a0;
			final int a1 = this.a1;
			final int a2 = this.a2;

			for(int x = px0; x <= px1; x++) {
				//				if (w0 > 0 && w1 > 0 && w2 > 0) {
				if ((w0 | w1 | w2) >= 0) {
					word = setPixelInWordPreMasked(word, x, py0);
				}

				// One step to the right
				w0 += a0;
				w1 += a1;
				w2 += a2;
			}

			lowBins[index] = word;
			return;

		} else if ((px0 | py0) == 0 && (px1 & py1) == 7) {
			// if filling whole bin then do it quick
			final int w0 = w0_row;
			final int w1 = w1_row;
			final int w2 = w2_row;

			//			if (w0 > 0 && w1 > 0 && w2 > 0
			//					&& w0 + xBinStep0 > 0 && w1 + xBinStep1 > 0 && w2 + xBinStep2 > 0
			//					&& w0 + yBinStep0 > 0 && w1 + yBinStep1 > 0 && w2 + yBinStep2 > 0
			//					&& w0 + xyBinStep0 > 0 && w1 + xyBinStep1 > 0 && w2 + xyBinStep2 > 0) {

			if ((w0 | w1 | w2
					| (w0 + xLowStep0) | (w1 + xLowStep1) | (w2 + xLowStep2)
					| (w0 + yLowStep0) | (w1 + yLowStep1) | (w2 + yLowStep2)
					| (w0 + xyLowStep0) | (w1 + xyLowStep1) | (w2 + xyLowStep2)) >= 0) {
				lowBins[index] = -1;
				return;
			}
		}

		final int a0 = this.a0;
		final int b0 = this.b0;
		final int a1 = this.a1;
		final int b1 = this.b1;
		final int a2 = this.a2;
		final int b2 = this.b2;

		// Rasterize
		for (int y = py0; y <= py1; y++) {
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = px0; x <= px1; x++) {
				// If p is on or inside all edges, render pixel.

				//				if (w0 > 0 && w1 > 0 && w2 > 0) {
				if ((w0 | w1 | w2) >= 0) {
					word = setPixelInWordPreMasked(word, x, y);
				}

				// One step to the right
				w0 += a0;
				w1 += a1;
				w2 += a2;
			}

			// One row step
			w0_row += b0;
			w1_row += b1;
			w2_row += b2;
		}

		lowBins[index] = word;
		return;
	}

	private static int wordIndex(int x, int y)  {
		return  ((y & BIN_PIXEL_INVERSE_MASK) << HEIGHT_WORD_RELATIVE_SHIFT) | (x >> BIN_AXIS_SHIFT);
	}

	private static int pixelIndex(int x, int y)  {
		return  ((y & BIN_PIXEL_INDEX_MASK) << BIN_AXIS_SHIFT) | (x & BIN_PIXEL_INDEX_MASK);
	}

	private static boolean isPixelClear(long word, int x, int y)  {
		return (word & (1L << pixelIndex(x, y))) == 0;
	}

	private static long setPixelInWord(long word, int x, int y) {
		return word | (1L << pixelIndex(x, y));
	}

	/** REQUIRES 0-7 inputs! */
	private static boolean testPixelInWordPreMasked(long word, int x, int y) {
		return (word & (1L << ((y << BIN_AXIS_SHIFT) | x))) == 0;
	}

	private static long setPixelInWordPreMasked(long word, int x, int y) {
		return word | (1L << ((y << BIN_AXIS_SHIFT) | x));
	}

	private boolean testPixel(int x, int y) {
		return (lowBins[wordIndex(x, y)] & (1L << (pixelIndex(x, y)))) == 0;
	}

	private long nextTime;
	private static final boolean DISABLE_RASTER_OUTPUT = !Configurator.debugOcclusionRaster;

	public void outputRaster() {
		if (DISABLE_RASTER_OUTPUT) {
			return;
		}

		final long t = System.currentTimeMillis();

		if (t >= nextTime) {
			nextTime = t + 1000;

			final NativeImage nativeImage = new NativeImage(PIXEL_WIDTH, PIXEL_HEIGHT, false);

			for (int x = 0; x < PIXEL_WIDTH; x++) {
				for (int y = 0; y < PIXEL_HEIGHT; y++) {
					nativeImage.setPixelRgba(x, y, testPixel(x, y) ? -1 :0xFF000000);
				}
			}

			nativeImage.mirrorVertically();

			final File file = new File(MinecraftClient.getInstance().runDirectory, "canvas_occlusion_raster.png");

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

	static final int BIN_AXIS_SHIFT = 3;
	static final int BIN_PIXEL_DIAMETER = 1 << BIN_AXIS_SHIFT;
	static final int BIN_PIXEL_INDEX_MASK = BIN_PIXEL_DIAMETER - 1;
	static final int BIN_PIXEL_INVERSE_MASK = ~BIN_PIXEL_INDEX_MASK;

	private static final int LOW_AXIS_SHIFT = BIN_AXIS_SHIFT;
	private static final int MID_AXIS_SHIFT = BIN_AXIS_SHIFT * 2;
	private static final int TOP_AXIS_SHIFT = BIN_AXIS_SHIFT * 3;

	private static final int TOP_WIDTH = 2;
	private static final int TOP_Y_SHIFT = Integer.bitCount(TOP_WIDTH - 1);
	private static final int TOP_HEIGHT = 1;

	private static final int MID_WIDTH = TOP_WIDTH  * 8;
	private static final int MID_Y_SHIFT = Integer.bitCount(MID_WIDTH - 1);
	private static final int MIDDLE_HEIGHT = TOP_HEIGHT  * 8;

	static final int PRECISION_BITS = 2;
	static final int PRECISION_FRACTION_MASK = (1 << PRECISION_BITS) - 1;
	static final int PRECISION_INTEGER_MASK = ~PRECISION_FRACTION_MASK;
	static final int PRECISION_PIXEL_CENTER = 1 << (PRECISION_BITS - 1);

	private static final int LOW_WIDTH = MID_WIDTH * 8;
	private static final int LOW_Y_SHIFT = Integer.bitCount(LOW_WIDTH - 1);
	private static final int PIXEL_WIDTH = LOW_WIDTH * BIN_PIXEL_DIAMETER;
	static final int HALF_PIXEL_WIDTH = PIXEL_WIDTH / 2;
	static final int PRECISION_WIDTH = PIXEL_WIDTH << PRECISION_BITS;
	static final int HALF_PRECISION_WIDTH = PRECISION_WIDTH / 2;

	private static final int LOW_HEIGHT = MIDDLE_HEIGHT * 8;
	private static final int PIXEL_HEIGHT = LOW_HEIGHT * BIN_PIXEL_DIAMETER;
	static final int HALF_PIXEL_HEIGHT = PIXEL_HEIGHT / 2;
	private static final int HEIGHT_WORD_RELATIVE_SHIFT = LOW_Y_SHIFT - BIN_AXIS_SHIFT;
	static final int PRECISION_HEIGHT = PIXEL_HEIGHT << PRECISION_BITS;
	static final int HALF_PRECISION_HEIGHT = PRECISION_HEIGHT / 2;

	static final int GUARD_SIZE = 256;
	static final int GUARD_WIDTH = PRECISION_WIDTH + GUARD_SIZE;
	static final int GUARD_HEIGHT = PRECISION_HEIGHT + GUARD_SIZE;

	private static final int LOW_BIN_COUNT = LOW_WIDTH * LOW_HEIGHT;
	private static final int MIDDLE_BIN_COUNT = MID_WIDTH * LOW_HEIGHT;
	private static final int TOP_BIN_COUNT = TOP_WIDTH * TOP_HEIGHT;

	private static final int TOP_BIN_PIXEL_WIDTH = PIXEL_WIDTH / TOP_WIDTH;
	private static final int MID_BIN_PIXEL_WIDTH = PIXEL_WIDTH / MID_WIDTH;
	private static final int LOW_BIN_PIXEL_WIDTH = PIXEL_WIDTH / LOW_WIDTH;

	private static final long[] EMPTY_BITS = new long[LOW_BIN_COUNT];
}
