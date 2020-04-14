package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion._Constants.CAMERA_PRECISION_BITS;
import static grondag.canvas.chunk.occlusion._Constants.CAMERA_PRECISION_UNITY;
import static grondag.canvas.chunk.occlusion._Constants.EMPTY_BITS;
import static grondag.canvas.chunk.occlusion._Constants.ENABLE_RASTER_OUTPUT;
import static grondag.canvas.chunk.occlusion._Constants.LOW_TILE_COUNT;
import static grondag.canvas.chunk.occlusion._Constants.MID_TILE_COUNT;
import static grondag.canvas.chunk.occlusion._Constants.PIXEL_HEIGHT;
import static grondag.canvas.chunk.occlusion._Constants.PIXEL_WIDTH;
import static grondag.canvas.chunk.occlusion._Data.cameraX;
import static grondag.canvas.chunk.occlusion._Data.cameraY;
import static grondag.canvas.chunk.occlusion._Data.cameraZ;
import static grondag.canvas.chunk.occlusion._Data.modelMatrix;
import static grondag.canvas.chunk.occlusion._Data.mvpMatrix;
import static grondag.canvas.chunk.occlusion._Data.offsetX;
import static grondag.canvas.chunk.occlusion._Data.offsetY;
import static grondag.canvas.chunk.occlusion._Data.offsetZ;
import static grondag.canvas.chunk.occlusion._Data.projectionMatrix;
import static grondag.canvas.chunk.occlusion._Data.xOrigin;
import static grondag.canvas.chunk.occlusion._Data.yOrigin;
import static grondag.canvas.chunk.occlusion._Data.zOrigin;

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
import grondag.canvas.render.CanvasWorldRenderer;

public abstract class TerrainOccluder {
	private  TerrainOccluder() {}

	public static void clearScene() {
		System.arraycopy(EMPTY_BITS, 0, _Data.lowTiles, 0, LOW_TILE_COUNT);
		System.arraycopy(EMPTY_BITS, 0, _Data.midTiles, 0, MID_TILE_COUNT);
	}

	public static void prepareChunk(BlockPos origin, int occlusionRange) {
		_Data.occlusionRange = occlusionRange;
		xOrigin = origin.getX();
		yOrigin = origin.getY();
		zOrigin = origin.getZ();

		final float offsetXf = (float) (xOrigin - cameraX);
		final float offsetYf = (float) (yOrigin - cameraY);
		final float offsetZf = (float) (zOrigin - cameraZ);

		mvpMatrix.loadIdentity();
		mvpMatrix.multiply(projectionMatrix);
		mvpMatrix.multiply(modelMatrix);
		mvpMatrix.multiply(Matrix4f.translate(offsetXf, offsetYf, offsetZf));

		offsetX = Math.round(offsetXf * CAMERA_PRECISION_UNITY);
		offsetY = Math.round(offsetYf * CAMERA_PRECISION_UNITY);
		offsetZ = Math.round(offsetZf * CAMERA_PRECISION_UNITY);
	}

	public static void outputRaster() {
		if (!ENABLE_RASTER_OUTPUT) {
			return;
		}

		final long t = System.currentTimeMillis();

		if (t >= _Indexer.nextTime) {
			_Indexer.nextTime = t + 1000;

			final NativeImage nativeImage = new NativeImage(PIXEL_WIDTH, PIXEL_HEIGHT, false);

			for (int x = 0; x < PIXEL_WIDTH; x++) {
				for (int y = 0; y < PIXEL_HEIGHT; y++) {
					nativeImage.setPixelRgba(x, y, _Indexer.testPixel(x, y) ? -1 :0xFF000000);
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

	public static boolean isChunkVisible() {
		CanvasWorldRenderer.innerTimer.start();
		final boolean result = _Indexer.isChunkVisibleInner();
		CanvasWorldRenderer.innerTimer.stop();

		//		if (CanvasWorldRenderer.innerTimer.last() > 200000) {
		//			isChunkVisibleInner();
		//		}

		return result;
	}

	public static void occlude(int[] visData) {
		final int occlusionRange = _Data.occlusionRange;
		final int limit= visData.length;

		if (limit > 1) {
			for (int i = 1; i < limit; i++) {
				final int box  = visData[i];
				if (occlusionRange > PackedBox.range(box)) {
					//					if (i > 8) {
					//						System.out.println(String.format("Occluded %d of %d at range %d", i - 1, limit - 1, range));
					//					}

					break;
				}

				_Indexer.occlude(
						PackedBox.x0(box),
						PackedBox.y0(box),
						PackedBox.z0(box),
						PackedBox.x1(box),
						PackedBox.y1(box),
						PackedBox.z1(box));
			}
		}
	}

	public static void prepareScene(Matrix4f projectionMatrix, Matrix4f modelMatrix, Camera camera) {
		_Data.projectionMatrix = projectionMatrix.copy();
		_Data.modelMatrix = modelMatrix.copy();
		final Vec3d vec3d = camera.getPos();
		cameraX = vec3d.getX();
		cameraY = vec3d.getY();
		cameraZ = vec3d.getZ();
	}

	public static boolean isBoxVisible(int packedBox) {
		final int x0  = PackedBox.x0(packedBox);
		final int y0  = PackedBox.y0(packedBox);
		final int z0  = PackedBox.z0(packedBox);
		final int x1  = PackedBox.x1(packedBox);
		final int y1  = PackedBox.y1(packedBox);
		final int z1  = PackedBox.z1(packedBox);


		_Indexer.computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		// rank tests by how directly they face - use distance from camera coordinates for this
		final int offsetX = _Data.offsetX;
		final int offsetY = _Data.offsetY;
		final int offsetZ = _Data.offsetZ;

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
				return offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast();
			case 0b010:
				return offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp();
			case 0b011:
				if (xTest > yTest) {
					return (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast()) || (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp());
				} else {
					return (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp()) || (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast());
				}
			case 0b100:
				return offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth();
			case 0b101:
				if (xTest > zTest) {
					return (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast()) || (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth());
				} else {
					return (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth()) || (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast());
				}
			case 0b110:
				if (yTest > zTest) {
					return (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp()) || (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth());
				} else {
					return (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth()) || (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp());
				}
			case 0b111:
				if (xTest > yTest) {
					if  (zTest > xTest) {
						// z first
						return (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth())
								|| (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast())
								|| (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp());
					} else {
						// x first
						return (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast())
								|| (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth())
								|| (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp());
					}
				} else if (zTest > yTest) {
					// z first
					return (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth())
							|| (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp())
							|| (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast());
				} else {
					// y first
					return (offsetY > by0 ? _Indexer.testDown() : _Indexer.testUp())
							|| (offsetZ > bz0 ? _Indexer.testNorth() : _Indexer.testSouth())
							|| (offsetX > bx0 ? _Indexer.testWest() : _Indexer.testEast());
				}
			}
		}
	}
}
