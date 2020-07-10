package grondag.canvas.terrain.occlusion;

import static grondag.canvas.terrain.occlusion.Constants.CAMERA_PRECISION_BITS;
import static grondag.canvas.terrain.occlusion.Constants.CAMERA_PRECISION_UNITY;
import static grondag.canvas.terrain.occlusion.Constants.DOWN;
import static grondag.canvas.terrain.occlusion.Constants.EAST;
import static grondag.canvas.terrain.occlusion.Constants.EMPTY_BITS;
import static grondag.canvas.terrain.occlusion.Constants.NORTH;
import static grondag.canvas.terrain.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.terrain.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.terrain.occlusion.Constants.SOUTH;
import static grondag.canvas.terrain.occlusion.Constants.TILE_COUNT;
import static grondag.canvas.terrain.occlusion.Constants.UP;
import static grondag.canvas.terrain.occlusion.Constants.V000;
import static grondag.canvas.terrain.occlusion.Constants.V001;
import static grondag.canvas.terrain.occlusion.Constants.V010;
import static grondag.canvas.terrain.occlusion.Constants.V011;
import static grondag.canvas.terrain.occlusion.Constants.V100;
import static grondag.canvas.terrain.occlusion.Constants.V101;
import static grondag.canvas.terrain.occlusion.Constants.V110;
import static grondag.canvas.terrain.occlusion.Constants.V111;
import static grondag.canvas.terrain.occlusion.Constants.WEST;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.terrain.BuiltRenderRegion;
import grondag.canvas.terrain.occlusion.region.PackedBox;

public class TerrainOccluder {
	private final Matrix4L baseMvpMatrix = new Matrix4L();

	private final Rasterizer raster = new Rasterizer();

	private long viewX;
	private long viewY;
	private long viewZ;

	private int offsetX;
	private int offsetY;
	private int offsetZ;

	private int occlusionRange;

	private int positionVersion = -1;
	private int viewVersion = -1;
	private int regionVersion = -1;

	private final AtomicInteger occluderVersion = new AtomicInteger();
	private boolean forceRedraw = false;
	private boolean needsRedraw = false;

	public void copyFrom(TerrainOccluder source) {
		baseMvpMatrix.copyFrom(source.baseMvpMatrix);
		raster.copyFrom(source.raster);
		viewX = source.viewX;
		viewY = source.viewY;
		viewZ = source.viewZ;

		offsetX = source.offsetX;
		offsetY = source.offsetY;
		offsetZ = source.offsetZ;

		occlusionRange = source.occlusionRange;

		positionVersion = source.positionVersion;
		viewVersion = source.viewVersion;
		regionVersion = source.regionVersion;

		occluderVersion.set(source.occluderVersion.get());

		forceRedraw = source.forceRedraw;
		needsRedraw = source.needsRedraw;
	}

	/**
	 * Previously tested regions can reuse test results if their version matches.
	 * However, they must still be drawn (if visible) if indicated by {@link #clearSceneIfNeeded(int, int)}.
	 */
	public int version() {
		return occluderVersion.get();
	}

	/**
	 * Force update to new version if provided version matches current
	 * @param occluderVersion
	 */
	public void invalidate(int invalidVersion) {
		if (occluderVersion.compareAndSet(invalidVersion, invalidVersion + 1))  {
			forceRedraw = true;
		}
	}

	/**
	 * Force update to new version
	 */
	public void invalidate() {
		occluderVersion.incrementAndGet();
		forceRedraw = true;
	}

	public void prepareRegion(BlockPos origin, int occlusionRange) {
		this.occlusionRange = occlusionRange;

		// PERF: could perhaps reuse CameraRelativeCenter values in BuildRenderRegion that are used by Frustum
		offsetX = (int) ((origin.getX() << CAMERA_PRECISION_BITS) - viewX);
		offsetY = (int) ((origin.getY() << CAMERA_PRECISION_BITS) - viewY);
		offsetZ = (int) ((origin.getZ() << CAMERA_PRECISION_BITS) - viewZ);

		final Matrix4L mvpMatrix = raster.mvpMatrix;
		mvpMatrix.copyFrom(baseMvpMatrix);
		mvpMatrix.translate(offsetX, offsetY, offsetZ, CAMERA_PRECISION_BITS);
	}

	public void outputRaster() {
		final long t = System.currentTimeMillis();

		if (t >= raster.nextRasterOutputTime) {
			raster.nextRasterOutputTime = t + 1000;

			final NativeImage nativeImage = new NativeImage(PIXEL_WIDTH, PIXEL_HEIGHT, false);

			for (int x = 0; x < PIXEL_WIDTH; x++) {
				for (int y = 0; y < PIXEL_HEIGHT; y++) {
					nativeImage.setPixelColor(x, y, raster.testPixel(x, y) ? -1 :0xFF000000);
				}
			}

			nativeImage.mirrorVertically();

			@SuppressWarnings("resource")
			final File file = new File(MinecraftClient.getInstance().runDirectory, "canvas_occlusion_raster.png");

			Util.method_27958().execute(() -> {
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
	public void prepareScene(Camera camera, CanvasFrustum frustum, int regionVersion) {
		final int viewVersion = frustum.viewVersion();
		final int positionVersion = frustum.positionVersion();

		if (this.viewVersion != viewVersion) {
			final Matrix4L baseMvpMatrix = this.baseMvpMatrix;
			final Matrix4L tempMatrix = raster.mvpMatrix;
			final Matrix4fExt projectionMatrix = frustum.projectionMatrix();
			final Matrix4fExt modelMatrix = frustum.modelMatrix();

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
			this.viewVersion = viewVersion;
			this.positionVersion = positionVersion;
			this.regionVersion = regionVersion;
			System.arraycopy(EMPTY_BITS, 0, raster.tiles, 0, TILE_COUNT);
			forceRedraw = false;
			needsRedraw = true;
		} else if (this.positionVersion != positionVersion || this.regionVersion != regionVersion) {
			occluderVersion.incrementAndGet();
			this.viewVersion = viewVersion;
			this.positionVersion = positionVersion;
			this.regionVersion = regionVersion;
			System.arraycopy(EMPTY_BITS, 0, raster.tiles, 0, TILE_COUNT);
			needsRedraw = true;
		} else if (this.viewVersion != viewVersion) {
			this.viewVersion = viewVersion;
			System.arraycopy(EMPTY_BITS, 0, raster.tiles, 0, TILE_COUNT);
			needsRedraw = true;
		} else {
			needsRedraw = false;
		}


	}

	public boolean needsRedraw() {
		return needsRedraw;
	}

	/**
	 * Does not rely on winding order but instead the distance from
	 * plane with known facing to camera position.
	 */
	public boolean isBoxVisible(int packedBox) {
		final int x0  = PackedBox.x0(packedBox) - 1;
		final int y0  = PackedBox.y0(packedBox) - 1;
		final int z0  = PackedBox.z0(packedBox) - 1;
		final int x1  = PackedBox.x1(packedBox) + 1;
		final int y1  = PackedBox.y1(packedBox) + 1;
		final int z1  = PackedBox.z1(packedBox) + 1;

		final int offsetX = this.offsetX;
		final int offsetY = this.offsetY;
		final int offsetZ = this.offsetZ;

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

		return boxTests[outcome].apply(x0, y0, z0, x1, y1, z1);
	}

	public boolean isEmptyRegionVisible(BlockPos origin) {
		prepareRegion(origin, 0);
		return isBoxVisible(PackedBox.FULL_BOX);
	}

	/**
	 * Does not rely on winding order but instead the distance from
	 * plane with known facing to camera position.
	 */
	private void occludeInner(int packedBox) {
		final int x0  = PackedBox.x0(packedBox);
		final int y0  = PackedBox.y0(packedBox);
		final int z0  = PackedBox.z0(packedBox);
		final int x1  = PackedBox.x1(packedBox);
		final int y1  = PackedBox.y1(packedBox);
		final int z1  = PackedBox.z1(packedBox);

		final int offsetX = this.offsetX;
		final int offsetY = this.offsetY;
		final int offsetZ = this.offsetZ;

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

		boxDraws[outcome].apply(x0, y0, z0, x1, y1, z1);
	}

	public void occlude(int[] visData) {
		final int occlusionRange = this.occlusionRange;
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

	/**
	 * Returns value with face flags set when all such
	 * faces in the region are at least 64 blocks away camera.
	 * @param region
	 * @return
	 */
	int backfaceVisibilityFlags(BuiltRenderRegion region) {
		final int offsetX = this.offsetX;
		final int offsetY = this.offsetY;
		final int offsetZ = this.offsetZ;

		int outcome = 0;

		// if offsetY is positive, chunk origin is above camera
		// if offsetY is negative, chunk origin is below camera;
		/**
		 * offsets are origin - camera
		 * if looking directly at chunk center, two values will be -8
		 *
		 * pos face check: -8 < -(16) == false
		 * neg face check: -8 > -(0) == false
		 *
		 * if 32 blocks above/positive to origin two values will be -32
		 *
		 * pos face check: -32 < -(16) == true
		 * neg face check: -32 > -(0) == false
		 *
		 * if 32 blocks below/positive to origin two values will be 32
		 *
		 * pos face check: 32 < -(16) == false
		 * neg face check: 32 > -(0) == true
		 *
		 *
		 * if looking directly at chunk center, two values will be -8
		 *
		 * pos face check: -8 < -(16) == false
		 * neg face check: -8 > -(0) == false
		 *
		 * if 64 blocks above/positive to origin two values will be -64
		 * neg face check: -64 > -(16) == false
		 *
		 * neg face > -64
		 *
		 * if 64 blocks below/positive to origin two values will be 64
		 *
		 * pos face check: 64 < -16 == false
		 *
		 * pos face culled when offset > 48
		 * neg face culled when offset < -72
		 *
		 *
		 * pos face visible when offset <= 48
		 * neg face visible when offset >= -72
		 */
		if (offsetY < (48 << CAMERA_PRECISION_BITS)) {
			outcome |= UP;
		} else if (offsetY > -(72 << CAMERA_PRECISION_BITS)) {
			outcome |= DOWN;
		}

		if (offsetX < (48 << CAMERA_PRECISION_BITS)) {
			outcome |= EAST;
		} else if (offsetX > -(72 << CAMERA_PRECISION_BITS)) {
			outcome |= WEST;
		}

		if (offsetZ < (48 << CAMERA_PRECISION_BITS)) {
			outcome |= SOUTH;
		} else if (offsetZ > -(72 << CAMERA_PRECISION_BITS)) {
			outcome |= NORTH;
		}

		return outcome;
	}

	@FunctionalInterface interface BoxTest {
		boolean apply(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	@FunctionalInterface interface BoxDraw {
		void apply(int x0, int y0, int z0, int x1, int y1, int z1);
	}

	private final BoxTest[] boxTests = new BoxTest[128];
	private final BoxDraw[] boxDraws = new BoxDraw[128];

	{
		boxTests[0] = (x0, y0, z0, x1, y1, z1) -> {
			return false;
		};

		boxTests[UP] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V110, V010, V011, V111);
		};

		boxTests[DOWN] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			return raster.testQuad(V000, V100, V101, V001);
		};

		boxTests[EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V101, V100, V110, V111);
		};

		boxTests[WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			return raster.testQuad(V000, V001, V011, V010);
		};

		boxTests[NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.testQuad(V100, V000, V010, V110);
		};

		boxTests[SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V001, V101, V111, V011);
		};

		// NB: Split across two quads to give more evenly-sized test regions vs potentially one big and one very small
		boxTests[UP | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V010, V011, V111, V101) ||
					raster.testQuad(V101, V100, V110, V010);
		};

		boxTests[UP | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V111, V110, V010, V000) ||
					raster.testQuad(V000, V001, V011, V111);
		};

		boxTests[UP | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V011, V111, V110, V100) ||
					raster.testQuad(V100, V000, V010, V011);
		};

		boxTests[UP | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V110, V010, V011, V001) ||
					raster.testQuad(V001, V101, V111, V110);
		};

		boxTests[DOWN | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V001, V000, V100, V110) ||
					raster.testQuad(V110, V111, V101, V001);
		};

		boxTests[DOWN | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			return raster.testQuad(V100, V101, V001, V011) ||
					raster.testQuad(V011, V010, V000, V100);
		};

		boxTests[DOWN | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.testQuad(V101, V001, V000, V010) ||
					raster.testQuad(V010, V110, V100, V101);
		};

		boxTests[DOWN | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V000, V100, V101, V111) ||
					raster.testQuad(V111, V011, V001, V000);
		};

		boxTests[NORTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V000, V010, V110, V111) ||
					raster.testQuad(V111, V101, V100, V000);
		};

		boxTests[NORTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.testQuad(V110, V100, V000, V001) ||
					raster.testQuad(V001, V011, V010, V110);
		};

		boxTests[SOUTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V011, V001, V101, V100) ||
					raster.testQuad(V100, V110, V111, V011);
		};

		boxTests[SOUTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V101, V111, V011, V010) ||
					raster.testQuad(V010, V000, V001, V101);
		};

		// NB: When three faces are visible, omit nearest vertex and draw two quads instead of three.

		boxTests[UP | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V011, V111, V101, V100 ) ||
					raster.testQuad(V100, V000, V010, V011);
		};

		boxTests[UP | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V111, V110, V100, V000) ||
					raster.testQuad(V000, V001, V011, V111);


		};

		boxTests[UP | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.testQuad(V010, V011, V001, V101) ||
					raster.testQuad(V101, V100, V110, V010);
		};

		boxTests[UP | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V110, V010, V000, V001) ||
					raster.testQuad(V001, V101, V111, V110);
		};

		boxTests[DOWN | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V001, V000, V010, V110) ||
					raster.testQuad(V110, V111, V101, V001);
		};

		boxTests[DOWN | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			return raster.testQuad(V101, V001, V011, V010) ||
					raster.testQuad(V010, V110, V100, V101);
		};

		boxTests[DOWN | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V000, V100, V110, V111) ||
					raster.testQuad(V111, V011, V001, V000);
		};

		boxTests[DOWN | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			return raster.testQuad(V100, V101, V111, V011) ||
					raster.testQuad(V011, V010, V000, V100);
		};

		////

		boxDraws[0] = (x0, y0, z0, x1, y1, z1) -> {
			// NOOP
		};

		boxDraws[UP] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V011, V111);
		};

		boxDraws[DOWN] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.drawQuad(V000, V100, V101, V001);
		};

		boxDraws[EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V101, V100, V110, V111);
		};

		boxDraws[WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.drawQuad(V000, V001, V011, V010);
		};

		boxDraws[NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V100, V000, V010, V110);
		};

		boxDraws[SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V001, V101, V111, V011);
		};

		// NB: Split across two quads to give more evenly-sized test regions vs potentially one big and one very small
		boxDraws[UP | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V010, V011, V111, V101); raster.drawQuad(V101, V100, V110, V010);
		};

		boxDraws[UP | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V111, V110, V010, V000);
			raster.drawQuad(V000, V001, V011, V111);
		};

		boxDraws[UP | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V011, V111, V110, V100);
			raster.drawQuad(V100, V000, V010, V011);
		};

		boxDraws[UP | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V011, V001);
			raster.drawQuad(V001, V101, V111, V110);
		};

		boxDraws[DOWN | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V001, V000, V100, V110);
			raster.drawQuad(V110, V111, V101, V001);
		};

		boxDraws[DOWN | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.drawQuad(V100, V101, V001, V011);
			raster.drawQuad(V011, V010, V000, V100);
		};

		boxDraws[DOWN | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V101, V001, V000, V010);
			raster.drawQuad(V010, V110, V100, V101);
		};

		boxDraws[DOWN | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V000, V100, V101, V111);
			raster.drawQuad(V111, V011, V001, V000);
		};

		boxDraws[NORTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V000, V010, V110, V111);
			raster.drawQuad(V111, V101, V100, V000);
		};

		boxDraws[NORTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V110, V100, V000, V001);
			raster.drawQuad(V001, V011, V010, V110);
		};

		boxDraws[SOUTH | EAST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V011, V001, V101, V100);
			raster.drawQuad(V100, V110, V111, V011);
		};

		boxDraws[SOUTH | WEST] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V101, V111, V011, V010);
			raster.drawQuad(V010, V000, V001, V101);
		};

		// NB: When three faces are visible, omit nearest vertex and draw two quads instead of three.

		boxDraws[UP | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V011, V111, V101, V100 );
			raster.drawQuad(V100, V000, V010, V011);
		};

		boxDraws[UP | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V111, V110, V100, V000);
			raster.drawQuad(V000, V001, V011, V111);


		};

		boxDraws[UP | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V010, V011, V001, V101);
			raster.drawQuad(V101, V100, V110, V010);
		};

		boxDraws[UP | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V110, V010, V000, V001);
			raster.drawQuad(V001, V101, V111, V110);
		};

		boxDraws[DOWN | EAST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V001, V000, V010, V110);
			raster.drawQuad(V110, V111, V101, V001);
		};

		boxDraws[DOWN | WEST | NORTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V110, x1, y1, z0);
			raster.drawQuad(V101, V001, V011, V010);
			raster.drawQuad(V010, V110, V100, V101);
		};

		boxDraws[DOWN | EAST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V001, x0, y0, z1);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V110, x1, y1, z0);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V000, V100, V110, V111);
			raster.drawQuad(V111, V011, V001, V000);
		};

		boxDraws[DOWN | WEST | SOUTH] = (x0, y0, z0, x1, y1, z1) -> {
			raster.setupVertex(V000, x0, y0, z0);
			raster.setupVertex(V010, x0, y1, z0);
			raster.setupVertex(V011, x0, y1, z1);
			raster.setupVertex(V100, x1, y0, z0);
			raster.setupVertex(V101, x1, y0, z1);
			raster.setupVertex(V111, x1, y1, z1);
			raster.drawQuad(V100, V101, V111, V011);
			raster.drawQuad(V011, V010, V000, V100);
		};
	}
}