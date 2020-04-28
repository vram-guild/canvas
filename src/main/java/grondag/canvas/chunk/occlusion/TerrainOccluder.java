package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.CAMERA_PRECISION_BITS;
import static grondag.canvas.chunk.occlusion.Constants.CAMERA_PRECISION_UNITY;
import static grondag.canvas.chunk.occlusion.Constants.EMPTY_BITS;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.TILE_COUNT;
import static grondag.canvas.chunk.occlusion.Constants.V000;
import static grondag.canvas.chunk.occlusion.Constants.V001;
import static grondag.canvas.chunk.occlusion.Constants.V010;
import static grondag.canvas.chunk.occlusion.Constants.V011;
import static grondag.canvas.chunk.occlusion.Constants.V100;
import static grondag.canvas.chunk.occlusion.Constants.V101;
import static grondag.canvas.chunk.occlusion.Constants.V110;
import static grondag.canvas.chunk.occlusion.Constants.V111;
import static grondag.canvas.chunk.occlusion.Data.forceRedraw;
import static grondag.canvas.chunk.occlusion.Data.needsRedraw;
import static grondag.canvas.chunk.occlusion.Data.occluderVersion;
import static grondag.canvas.chunk.occlusion.Data.offsetX;
import static grondag.canvas.chunk.occlusion.Data.offsetY;
import static grondag.canvas.chunk.occlusion.Data.offsetZ;
import static grondag.canvas.chunk.occlusion.Data.viewX;
import static grondag.canvas.chunk.occlusion.Data.viewY;
import static grondag.canvas.chunk.occlusion.Data.viewZ;
import static grondag.canvas.chunk.occlusion.Quad.setupVertex;
import static grondag.canvas.chunk.occlusion.Rasterizer.drawQuad;
import static grondag.canvas.chunk.occlusion.Rasterizer.testQuad;

import java.io.File;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.resource.ResourceImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.CanvasMod;
import grondag.canvas.chunk.occlusion.region.PackedBox;
import grondag.canvas.render.CanvasFrustum;

public abstract class TerrainOccluder {
	private  TerrainOccluder() {}

	/**
	 * Previously tested regions can reuse test results if their version matches.
	 * However, they must still be drawn (if visible) if indicated by {@link #clearSceneIfNeeded(int, int)}.
	 */
	public static int version() {
		return occluderVersion.get();
	}

	/**
	 * Force update to new version if provided version matches current
	 * @param occluderVersion
	 */
	public static void invalidate(int invalidVersion) {
		if (occluderVersion.compareAndSet(invalidVersion, invalidVersion + 1))  {
			forceRedraw = true;
		}
	}

	/**
	 * Force update to new version
	 */
	public static void invalidate() {
		occluderVersion.incrementAndGet();
		forceRedraw = true;
	}

	public static void prepareChunk(BlockPos origin, int occlusionRange) {
		Data.occlusionRange = occlusionRange;

		offsetX = (int) ((origin.getX() << CAMERA_PRECISION_BITS) - viewX);
		offsetY = (int) ((origin.getY() << CAMERA_PRECISION_BITS) - viewY);
		offsetZ = (int) ((origin.getZ() << CAMERA_PRECISION_BITS) - viewZ);

		final Matrix4L mvpMatrix = Data.mvpMatrix;
		mvpMatrix.copyFrom(Data.baseMvpMatrix);
		mvpMatrix.translate(offsetX, offsetY, offsetZ, CAMERA_PRECISION_BITS);
	}

	public static void outputRaster() {
		final long t = System.currentTimeMillis();

		if (t >= Indexer.nextRasterOutputTime) {
			Indexer.nextRasterOutputTime = t + 1000;

			final NativeImage nativeImage = new NativeImage(PIXEL_WIDTH, PIXEL_HEIGHT, false);

			for (int x = 0; x < PIXEL_WIDTH; x++) {
				for (int y = 0; y < PIXEL_HEIGHT; y++) {
					nativeImage.setPixelRgba(x, y, Indexer.testPixel(x, y) ? -1 :0xFF000000);
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


	/**
	 * Check if needs redrawn and prep for redraw if  so.
	 * When false, regions should be drawn only if their occluder version is not current.
	 *
	 * Also checks for invalidation of occluder version using positionVersion.
	 *
	 * @param projectionMatrix
	 * @param modelMatrix
	 * @param camera
	 * @param frustum
	 * @param regionVersion  Needed because chunk camera position update whenever a chunk boundary is crossed by Frustum doesn't care.
	 */
	public static void prepareScene(Matrix4f projectionMatrix, Matrix4f modelMatrix, Camera camera, CanvasFrustum frustum, int regionVersion) {
		final int viewVersion = frustum.viewVersion();
		final int positionVersion = frustum.positionVersion();

		if (Data.viewVersion != viewVersion) {
			final Matrix4L baseMvpMatrix = Data.baseMvpMatrix;
			final Matrix4L tempMatrix = Data.mvpMatrix;

			baseMvpMatrix.loadIdentity();

			tempMatrix.copyFrom(projectionMatrix);
			baseMvpMatrix.multiply(tempMatrix);

			tempMatrix.copyFrom(modelMatrix);
			baseMvpMatrix.multiply(tempMatrix);

			final Vec3d vec3d = camera.getPos();
			viewX = Math.round(vec3d.getX() * CAMERA_PRECISION_UNITY);
			viewY = Math.round(vec3d.getY() * CAMERA_PRECISION_UNITY);
			viewZ = Math.round(vec3d.getZ() * CAMERA_PRECISION_UNITY);
		}

		if (forceRedraw) {
			Data.viewVersion = viewVersion;
			Data.positionVersion = positionVersion;
			Data.regionVersion = regionVersion;
			System.arraycopy(EMPTY_BITS, 0, Data.tiles, 0, TILE_COUNT);
			forceRedraw = false;
			needsRedraw = true;
		} else if (Data.positionVersion != positionVersion || Data.regionVersion != regionVersion) {
			occluderVersion.incrementAndGet();
			Data.viewVersion = viewVersion;
			Data.positionVersion = positionVersion;
			Data.regionVersion = regionVersion;
			System.arraycopy(EMPTY_BITS, 0, Data.tiles, 0, TILE_COUNT);
			needsRedraw = true;
		} else if (Data.viewVersion != viewVersion) {
			Data.viewVersion = viewVersion;
			System.arraycopy(EMPTY_BITS, 0, Data.tiles, 0, TILE_COUNT);
			needsRedraw = true;
		} else {
			needsRedraw = false;
		}


	}

	public static boolean needsRedraw() {
		return needsRedraw;
	}

	public static boolean isBoxVisible(int packedBox) {
		final int x0  = PackedBox.x0(packedBox) - 1;
		final int y0  = PackedBox.y0(packedBox) - 1;
		final int z0  = PackedBox.z0(packedBox) - 1;
		final int x1  = PackedBox.x1(packedBox) + 1;
		final int y1  = PackedBox.y1(packedBox) + 1;
		final int z1  = PackedBox.z1(packedBox) + 1;

		final int offsetX = Data.offsetX;
		final int offsetY = Data.offsetY;
		final int offsetZ = Data.offsetZ;

		int outcome = 0;

		// if camera below top face can't be seen
		if (offsetY < -(y1 << CAMERA_PRECISION_BITS)) {
			outcome |= UP;
		} else if (offsetY > -(y0 << CAMERA_PRECISION_BITS)) {
			outcome |= DOWN;
		}

		if (offsetX < -(x1 << CAMERA_PRECISION_BITS)) {
			outcome |= EAST;
		} else if (offsetX > -(x0 << CAMERA_PRECISION_BITS)) {
			outcome |= WEST;
		}

		if (offsetZ < -(z1 << CAMERA_PRECISION_BITS)) {
			outcome |= SOUTH;
		} else if (offsetZ > -(z0 << CAMERA_PRECISION_BITS)) {
			outcome |= NORTH;
		}

		return BOX_TESTS[outcome].apply(x0, y0, z0, x1, y1, z1);
	}

	private static void occludeInner(int packedBox) {
		final int x0  = PackedBox.x0(packedBox);
		final int y0  = PackedBox.y0(packedBox);
		final int z0  = PackedBox.z0(packedBox);
		final int x1  = PackedBox.x1(packedBox);
		final int y1  = PackedBox.y1(packedBox);
		final int z1  = PackedBox.z1(packedBox);

		final int offsetX = Data.offsetX;
		final int offsetY = Data.offsetY;
		final int offsetZ = Data.offsetZ;

		int outcome = 0;

		// if camera below top face can't be seen
		if (offsetY < -(y1 << CAMERA_PRECISION_BITS)) {
			outcome |= UP;
		} else if (offsetY > -(y0 << CAMERA_PRECISION_BITS)) {
			outcome |= DOWN;
		}

		if (offsetX < -(x1 << CAMERA_PRECISION_BITS)) {
			outcome |= EAST;
		} else if (offsetX > -(x0 << CAMERA_PRECISION_BITS)) {
			outcome |= WEST;
		}

		if (offsetZ < -(z1 << CAMERA_PRECISION_BITS)) {
			outcome |= SOUTH;
		} else if (offsetZ > -(z0 << CAMERA_PRECISION_BITS)) {
			outcome |= NORTH;
		}

		BOX_DRAWS[outcome].apply(x0, y0, z0, x1, y1, z1);
	}

	public static void occlude(int[] visData) {
		final int occlusionRange = Data.occlusionRange;
		final int limit= visData.length;

		if (limit > 1) {
			for (int i = 1; i < limit; i++) {
				final int box  = visData[i];
				if (occlusionRange > PackedBox.range(box)) {
					break;
				}

				occludeInner(box);
			}
		}
	}

	@FunctionalInterface interface BoxTest {
		boolean apply(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	@FunctionalInterface interface BoxDraw {
		void apply(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	static final int UP = 1;
	static final int DOWN = 2;
	static final int EAST = 4;
	static final int WEST = 8;
	static final int NORTH = 16;
	static final int SOUTH = 32;

	private static BoxTest[] BOX_TESTS = new BoxTest[128];
	private static BoxDraw[] BOX_DRAWS = new BoxDraw[128];

	static {
		BOX_TESTS[0] = (x0, y0, z0, x1, y1, z1) -> {
			return false;
		};

		BOX_TESTS[UP] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V110, V010, V011, V111);
		};

		BOX_TESTS[DOWN] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			return testQuad(V000, V100, V101, V001);
		};

		BOX_TESTS[EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V101, V100, V110, V111);
		};

		BOX_TESTS[WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			return testQuad(V000, V001, V011, V010);
		};

		BOX_TESTS[NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			return testQuad(V100, V000, V010, V110);
		};

		BOX_TESTS[SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V001, V101, V111, V011);
		};

		// NB: Split across two quads to give more evenly-sized test regions vs potentially one big and one very small
		BOX_TESTS[UP | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V010, V011, V111, V101) ||
					testQuad(V101, V100, V110, V010);
		};

		BOX_TESTS[UP | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V111, V110, V010, V000) ||
					testQuad(V000, V001, V011, V111);
		};

		BOX_TESTS[UP | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V011, V111, V110, V100) ||
					testQuad(V100, V000, V010, V011);
		};

		BOX_TESTS[UP | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V110, V010, V011, V001) ||
					testQuad(V001, V101, V111, V110);
		};

		BOX_TESTS[DOWN | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V001, V000, V100, V110) ||
					testQuad(V110, V111, V101, V001);
		};

		BOX_TESTS[DOWN | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			return testQuad(V100, V101, V001, V011) ||
					testQuad(V011, V010, V000, V100);
		};

		BOX_TESTS[DOWN | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			return testQuad(V101, V001, V000, V010) ||
					testQuad(V010, V110, V100, V101);
		};

		BOX_TESTS[DOWN | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V000, V100, V101, V111) ||
					testQuad(V111, V011, V001, V000);
		};

		BOX_TESTS[NORTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V000, V010, V110, V111) ||
					testQuad(V111, V101, V100, V000);
		};

		BOX_TESTS[NORTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			return testQuad(V110, V100, V000, V001) ||
					testQuad(V001, V011, V010, V110);
		};

		BOX_TESTS[SOUTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V011, V001, V101, V100) ||
					testQuad(V100, V110, V111, V011);
		};

		BOX_TESTS[SOUTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V101, V111, V011, V010) ||
					testQuad(V010, V000, V001, V101);
		};

		// NB: When three faces are visible, omit nearest vertex and draw two quads instead of three.

		BOX_TESTS[UP | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V011, V111, V101, V100 ) ||
					testQuad(V100, V000, V010, V011);
		};

		BOX_TESTS[UP | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V111, V110, V100, V000) ||
					testQuad(V000, V001, V011, V111);


		};

		BOX_TESTS[UP | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			return testQuad(V010, V011, V001, V101) ||
					testQuad(V101, V100, V110, V010);
		};

		BOX_TESTS[UP | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V110, V010, V000, V001) ||
					testQuad(V001, V101, V111, V110);
		};

		BOX_TESTS[DOWN | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V001, V000, V010, V110) ||
					testQuad(V110, V111, V101, V001);
		};

		BOX_TESTS[DOWN | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			return testQuad(V101, V001, V011, V010) ||
					testQuad(V010, V110, V100, V101);
		};

		BOX_TESTS[DOWN | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V000, V100, V110, V111) ||
					testQuad(V111, V011, V001, V000);
		};

		BOX_TESTS[DOWN | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			return testQuad(V100, V101, V111, V011) ||
					testQuad(V011, V010, V000, V100);
		};

		////

		BOX_DRAWS[0] = (x0, y0, z0, x1, y1, z1) -> {
			// NOOP
		};

		BOX_DRAWS[UP] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V110, V010, V011, V111);
		};

		BOX_DRAWS[DOWN] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			drawQuad(V000, V100, V101, V001);
		};

		BOX_DRAWS[EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V101, V100, V110, V111);
		};

		BOX_DRAWS[WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			drawQuad(V000, V001, V011, V010);
		};

		BOX_DRAWS[NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			drawQuad(V100, V000, V010, V110);
		};

		BOX_DRAWS[SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V001, V101, V111, V011);
		};

		// NB: Split across two quads to give more evenly-sized test regions vs potentially one big and one very small
		BOX_DRAWS[UP | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V010, V011, V111, V101); drawQuad(V101, V100, V110, V010);
		};

		BOX_DRAWS[UP | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V111, V110, V010, V000);
			drawQuad(V000, V001, V011, V111);
		};

		BOX_DRAWS[UP | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V011, V111, V110, V100);
			drawQuad(V100, V000, V010, V011);
		};

		BOX_DRAWS[UP | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V110, V010, V011, V001);
			drawQuad(V001, V101, V111, V110);
		};

		BOX_DRAWS[DOWN | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V001, V000, V100, V110);
			drawQuad(V110, V111, V101, V001);
		};

		BOX_DRAWS[DOWN | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			drawQuad(V100, V101, V001, V011);
			drawQuad(V011, V010, V000, V100);
		};

		BOX_DRAWS[DOWN | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			drawQuad(V101, V001, V000, V010);
			drawQuad(V010, V110, V100, V101);
		};

		BOX_DRAWS[DOWN | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V000, V100, V101, V111);
			drawQuad(V111, V011, V001, V000);
		};

		BOX_DRAWS[NORTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V000, V010, V110, V111);
			drawQuad(V111, V101, V100, V000);
		};

		BOX_DRAWS[NORTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			drawQuad(V110, V100, V000, V001);
			drawQuad(V001, V011, V010, V110);
		};

		BOX_DRAWS[SOUTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V011, V001, V101, V100);
			drawQuad(V100, V110, V111, V011);
		};

		BOX_DRAWS[SOUTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V101, V111, V011, V010);
			drawQuad(V010, V000, V001, V101);
		};

		// NB: When three faces are visible, omit nearest vertex and draw two quads instead of three.

		BOX_DRAWS[UP | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V011, V111, V101, V100 );
			drawQuad(V100, V000, V010, V011);
		};

		BOX_DRAWS[UP | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V111, V110, V100, V000);
			drawQuad(V000, V001, V011, V111);


		};

		BOX_DRAWS[UP | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			drawQuad(V010, V011, V001, V101);
			drawQuad(V101, V100, V110, V010);
		};

		BOX_DRAWS[UP | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V110, V010, V000, V001);
			drawQuad(V001, V101, V111, V110);
		};

		BOX_DRAWS[DOWN | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V001, V000, V010, V110);
			drawQuad(V110, V111, V101, V001);
		};

		BOX_DRAWS[DOWN | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V001, x0, y0, z1);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V110, x1, y1, z0);
			drawQuad(V101, V001, V011, V010);
			drawQuad(V010, V110, V100, V101);
		};

		BOX_DRAWS[DOWN | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V001, x0, y0, z1);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V110, x1, y1, z0);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V000, V100, V110, V111);
			drawQuad(V111, V011, V001, V000);
		};

		BOX_DRAWS[DOWN | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			setupVertex(V000, x0, y0, z0);
			setupVertex(V010, x0, y1, z0);
			setupVertex(V011, x0, y1, z1);
			setupVertex(V100, x1, y0, z0);
			setupVertex(V101, x1, y0, z1);
			setupVertex(V111, x1, y1, z1);
			drawQuad(V100, V101, V111, V011);
			drawQuad(V011, V010, V000, V100);
		};
	}
}
