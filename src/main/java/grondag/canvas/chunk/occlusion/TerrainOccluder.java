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
	static final int HALF_WIDTH = 128;
	private static final int WIDTH = HALF_WIDTH * 2;
	static final int HALF_HEIGHT = 64;
	private static final int HEIGHT = HALF_HEIGHT * 2;
	private static final int SHIFT = Integer.bitCount(WIDTH - 1);

	private static final int WORD_COUNT = WIDTH * HEIGHT / 64;

	private static final long[] EMPTY_BITS = new long[WORD_COUNT];

	private static final long[] bits = new long[WORD_COUNT];

	private Matrix4f projectionMatrix;
	private Matrix4f modelMatrix;
	private final Matrix4f mvpMatrix = new Matrix4f();

	private final Lazy4f v000 = new Lazy4f();
	private final Lazy4f v001 = new Lazy4f();
	private final Lazy4f v010 = new Lazy4f();
	private final Lazy4f v011 = new Lazy4f();
	private final Lazy4f v100 = new Lazy4f();
	private final Lazy4f v101 = new Lazy4f();
	private final Lazy4f v110 = new Lazy4f();
	private final Lazy4f v111 = new Lazy4f();

	private final Lazy4f va = new Lazy4f();
	private final Lazy4f vb = new Lazy4f();

	// Boumds of current triangle
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;

	// Barycentric coordinates at minX/minY corner
	private int wOrigin0;
	private int wOrigin1;
	private int wOrigin2;

	private void drawDown() {
		drawQuad(v000, v100, v101, v001);
	}

	private void drawUp() {
		drawQuad(v110, v010, v011, v111);
	}

	private void drawEast() {
		drawQuad(v101, v100, v110, v111);
	}

	private void drawWest() {
		drawQuad(v000, v001, v011, v010);
	}

	private void drawSouth() {
		drawQuad(v001, v101, v111, v011);
	}

	private void drawNorth() {
		drawQuad(v100, v000, v010, v110);
	}

	private boolean testDown() {
		return testQuad(v000, v100, v101, v001);  // down
	}

	private boolean testUp() {
		return testQuad(v110, v010, v011, v111); // up
	}

	private boolean testEast() {
		return testQuad(v101, v100, v110, v111); // east
	}

	private boolean testWest() {
		return testQuad(v000, v001, v011, v010); // west
	}

	private boolean testSouth() {
		return testQuad(v001, v101, v111, v011); // south
	}

	private boolean testNorth() {
		return testQuad(v100, v000, v010, v110); // north
	}

	public void prepareScene(Matrix4f projectionMatrix, Matrix4f modelMatrix, Camera camera) {
		this.projectionMatrix = projectionMatrix.copy();
		this.modelMatrix = modelMatrix.copy();
		final Vec3d vec3d = camera.getPos();
		cameraX = vec3d.getX();
		cameraY = vec3d.getY();
		cameraZ = vec3d.getZ();
		System.arraycopy(EMPTY_BITS, 0, bits, 0, WORD_COUNT);
	}

	private int xOrigin;
	private int yOrigin;
	private int zOrigin;

	private double cameraX;
	private double cameraY;
	private double cameraZ;

	public void prepareChunk(BlockPos origin) {
		xOrigin = origin.getX();
		yOrigin = origin.getY();
		zOrigin = origin.getZ();

		final float offsetX = (float) (xOrigin - cameraX);
		final float offsetY = (float) (yOrigin - cameraY);
		final float offsetZ = (float) (zOrigin - cameraZ);

		mvpMatrix.loadIdentity();
		mvpMatrix.multiply(projectionMatrix);
		mvpMatrix.multiply(modelMatrix);
		mvpMatrix.multiply(Matrix4f.translate(offsetX, offsetY, offsetZ));
	}

	public boolean isChunkVisible()  {
		CanvasWorldRenderer.innerTimer.start();

		computeProjectedBoxBounds(0, 0, 0, 16, 16, 16);

		final boolean east = testEast();
		final boolean west = testWest();
		final boolean north = testNorth();
		final boolean south = testSouth();
		final boolean up = testUp();
		final boolean down = testDown();

		CanvasWorldRenderer.innerTimer.stop();

		return east || west || north || south || up || down;
	}

	public boolean isBoxVisible(int packedBox) {

		computeProjectedBoxBounds(
				PackedBox.x0(packedBox),
				PackedBox.y0(packedBox),
				PackedBox.z0(packedBox),
				PackedBox.x1(packedBox),
				PackedBox.y1(packedBox),
				PackedBox.z1(packedBox));

		final boolean east = testEast();
		final boolean west = testWest();
		final boolean north = testNorth();
		final boolean south = testSouth();
		final boolean up = testUp();
		final boolean down = testDown();


		return east || west || north || south || up || down;
	}

	public void occludeChunk()  {
		computeProjectedBoxBounds(0, 0, 0, 16, 16, 16);
		drawUp();
		drawDown();
		drawEast();
		drawWest();
		drawNorth();
		drawSouth();
	}

	public void occlude(int[] visData, int squaredCameraDistance) {
		final int limit= visData.length;
		final int range = squaredCameraDistance > 1024 ? PackedBox.OCCLUSION_RANGE_FAR : squaredCameraDistance < 512 ? PackedBox.OCCLUSION_RANGE_NEAR : PackedBox.OCCLUSION_RANGE_MEDIUM;

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

	private void occlude(float x0, float y0, float z0, float x1, float y1, float z1) {
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);
		drawUp();
		drawDown();
		drawEast();
		drawWest();
		drawNorth();
		drawSouth();
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

	private boolean testSplitQuad(int split, Lazy4f v0, Lazy4f v1, Lazy4f v2, Lazy4f v3) {
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

	private boolean prepareTriBounds(Lazy4f v0, Lazy4f v1, Lazy4f v2) {
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
		wOrigin0 = orient2d(x1, y1, x2, y2, minX, minY);
		wOrigin1 = orient2d(x2, y2, x0, y0, minX, minY);
		wOrigin2 = orient2d(x0, y0, x1, y1, minX, minY);

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

		// Barycentric coordinates at bounding box origin
		int w0_row = wOrigin0;
		int w1_row = wOrigin1;
		int w2_row = wOrigin2;

		// Rasterize
		for (int y = minY; y <= maxY; y++) {
			// Barycentric coordinates
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minX; x <= maxX; x++) {
				// If p is inside all edges, render pixel.
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
		final int addr = (y << SHIFT) | x;
		bits[addr >> 6] |= (1L << (addr & 63));
	}

	private boolean testTri(Lazy4f v0, Lazy4f v1, Lazy4f v2) {
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
		final int dy10 = y0 - y1;
		final int dx01 = x1 - x0;
		final int dy21 = y1 - y2;
		final int dx12 = x2 - x1;
		final int dy02 = y2 - y0;
		final int dx20 = x0 - x2;

		// Barycentric coordinates at bounding box origin
		int w0_row = wOrigin0;
		int w1_row = wOrigin1;
		int w2_row = wOrigin2;

		// Rasterize
		for (int y = minY; y <= maxY; y++) {
			// Barycentric coordinates
			int w0 = w0_row;
			int w1 = w1_row;
			int w2 = w2_row;

			for (int x = minX; x <= maxX; x++) {
				// If p is on or inside all edges, render pixel.
				if ((w0 | w1 | w2) >= 0 && testPixel(x, y)) {
					return true;
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

		return false;
	}

	private boolean testPixel(int x, int y) {
		final int addr = (y << SHIFT) | x;
		return (bits[addr >> 6] & (1L << (addr & 63))) == 0;
	}

	private int tickCounter;
	private static final boolean DISABLE_RASTER_OUTPUT = !Configurator.debugOcclusionRaster;

	public void outputRaster() {
		if (DISABLE_RASTER_OUTPUT || --tickCounter > 0) {
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
