package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.Constants.CAMERA_PRECISION_BITS;
import static grondag.canvas.chunk.occlusion.Constants.CAMERA_PRECISION_UNITY;
import static grondag.canvas.chunk.occlusion.Constants.EMPTY_BITS;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion.Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion.Constants.TILE_COUNT;
import static grondag.canvas.chunk.occlusion.Data.forceRedraw;
import static grondag.canvas.chunk.occlusion.Data.needsRedraw;
import static grondag.canvas.chunk.occlusion.Data.occluderVersion;
import static grondag.canvas.chunk.occlusion.Data.offsetX;
import static grondag.canvas.chunk.occlusion.Data.offsetY;
import static grondag.canvas.chunk.occlusion.Data.offsetZ;
import static grondag.canvas.chunk.occlusion.Data.viewX;
import static grondag.canvas.chunk.occlusion.Data.viewY;
import static grondag.canvas.chunk.occlusion.Data.viewZ;

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

	public static void occlude(int[] visData) {
		final int occlusionRange = Data.occlusionRange;
		final int limit= visData.length;

		if (limit > 1) {
			for (int i = 1; i < limit; i++) {
				final int box  = visData[i];
				if (occlusionRange > PackedBox.range(box)) {
					break;
				}

				Indexer.occlude(
						PackedBox.x0(box),
						PackedBox.y0(box),
						PackedBox.z0(box),
						PackedBox.x1(box),
						PackedBox.y1(box),
						PackedBox.z1(box));
			}
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

		Indexer.computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		// rank tests by how directly they face - use distance from camera coordinates for this
		final int offsetX = Data.offsetX;
		final int offsetY = Data.offsetY;
		final int offsetZ = Data.offsetZ;

		int testBits = 0;
		int nearBits  = 0;
		int xTest =  0;
		int yTest = 0;
		int zTest = 0;

		final int bx0 = -(x0 << CAMERA_PRECISION_BITS);
		final int bx1 = -(x1 << CAMERA_PRECISION_BITS);
		final int by0 = -(y0 << CAMERA_PRECISION_BITS);
		final int by1 = -(y1 << CAMERA_PRECISION_BITS);
		final int bz0 = -(z0 << CAMERA_PRECISION_BITS);
		final int bz1 = -(z1 << CAMERA_PRECISION_BITS);

		// PERF: improve branch prediction

		// if camera below top face can't be seen
		if (offsetY < by1) {
			// UP
			if (offsetY > by1 - CAMERA_PRECISION_UNITY) {
				// if very close to plane then don't test - precision may give inconsistent results
				nearBits |= 2;
			}

			yTest = -offsetY - by1;
			testBits |= 2;
		} else if (offsetY > by0) {
			// DOWN
			if (offsetY < by0 + CAMERA_PRECISION_UNITY) {
				nearBits |= 2;
			}

			yTest = offsetY;
			testBits |= 2;
		}

		if (offsetX < bx1) {
			// EAST;
			if (offsetX > bx1 - CAMERA_PRECISION_UNITY) {
				nearBits |= 1;
			}

			xTest = -offsetX - bx1;
			testBits |= 1;
		} else if (offsetX > bx0) {
			// WEST
			if (offsetX < bx0 + CAMERA_PRECISION_UNITY) {
				nearBits |= 1;
			}

			xTest = offsetX;
			testBits |= 1;
		}

		if (offsetZ < bz1) {
			// SOUTH
			if (offsetZ > bz1 - CAMERA_PRECISION_UNITY) {
				nearBits |= 4;
			}

			zTest = -offsetZ - bz1;
			testBits |= 4;
		} else if (offsetZ > bz0) {
			// NORTH
			if (offsetZ < bz0 + CAMERA_PRECISION_UNITY) {
				nearBits |= 4;
			}

			zTest = offsetZ;
			testBits |= 4;
		}

		// if only valid tests are very near, assume visible to avoid false negatives due to precision
		if (nearBits != 0 && (testBits & ~nearBits) == 0) {
			return true;
		} else {
			switch (testBits)  {
			default:
			case 0b000:
				return false;
			case 0b001:
				return offsetX > bx0 ? Indexer.testWest() : Indexer.testEast();
			case 0b010:
				return offsetY > by0 ? Indexer.testDown() : Indexer.testUp();
			case 0b011:
				if (xTest > yTest) {
					return (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast()) || (offsetY > by0 ? Indexer.testDown() : Indexer.testUp());
				} else {
					return (offsetY > by0 ? Indexer.testDown() : Indexer.testUp()) || (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast());
				}
			case 0b100:
				return offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth();
			case 0b101:
				if (xTest > zTest) {
					return (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast()) || (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth());
				} else {
					return (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth()) || (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast());
				}
			case 0b110:
				if (yTest > zTest) {
					return (offsetY > by0 ? Indexer.testDown() : Indexer.testUp()) || (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth());
				} else {
					return (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth()) || (offsetY > by0 ? Indexer.testDown() : Indexer.testUp());
				}
			case 0b111:
				if (xTest > yTest) {
					if  (zTest > xTest) {
						// z first
						return (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth())
								|| (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast())
								|| (offsetY > by0 ? Indexer.testDown() : Indexer.testUp());
					} else {
						// x first
						return (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast())
								|| (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth())
								|| (offsetY > by0 ? Indexer.testDown() : Indexer.testUp());
					}
				} else if (zTest > yTest) {
					// z first
					return (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth())
							|| (offsetY > by0 ? Indexer.testDown() : Indexer.testUp())
							|| (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast());
				} else {
					// y first
					return (offsetY > by0 ? Indexer.testDown() : Indexer.testUp())
							|| (offsetZ > bz0 ? Indexer.testNorth() : Indexer.testSouth())
							|| (offsetX > bx0 ? Indexer.testWest() : Indexer.testEast());
				}
			}
		}
	}
}
