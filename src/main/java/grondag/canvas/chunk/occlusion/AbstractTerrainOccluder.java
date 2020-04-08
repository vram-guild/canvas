package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PROJECTED_VERTEX_STRIDE;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PX;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.PV_PY;
import static grondag.canvas.chunk.occlusion.ProjectedVertexData.setupVertex;

import java.io.File;

import com.google.common.base.Strings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.resource.ResourceImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.render.CanvasWorldRenderer;

public abstract class AbstractTerrainOccluder {
	protected final long[] lowBins = new long[LOW_BIN_COUNT];
	protected final long[] midBins = new long[MID_BIN_WORDS];
	protected final long[] topBins = new long[TOP_BIN_WORDS];

	protected Matrix4f projectionMatrix;
	protected Matrix4f modelMatrix;
	protected final Matrix4f mvpMatrix = new Matrix4f();
	protected final Matrix4fExt mvpMatrixExt =  (Matrix4fExt)(Object) mvpMatrix;

	protected static final int V000 = 0;
	protected static final int V001 = V000 + PROJECTED_VERTEX_STRIDE;
	protected static final int V010 = V001 + PROJECTED_VERTEX_STRIDE;
	protected static final int V011 = V010 + PROJECTED_VERTEX_STRIDE;
	protected static final int V100 = V011 + PROJECTED_VERTEX_STRIDE;
	protected static final int V101 = V100 + PROJECTED_VERTEX_STRIDE;
	protected static final int V110 = V101 + PROJECTED_VERTEX_STRIDE;
	protected static final int V111 = V110 + PROJECTED_VERTEX_STRIDE;

	protected static final int V_NEAR_CLIP_A = V111 + PROJECTED_VERTEX_STRIDE;
	protected static final int V_NEAR_CLIP_B = V_NEAR_CLIP_A + PROJECTED_VERTEX_STRIDE;
	protected static final int V_LOW_X_CLIP_A = V_NEAR_CLIP_B + PROJECTED_VERTEX_STRIDE;
	protected static final int V_LOW_X_CLIP_B = V_LOW_X_CLIP_A + PROJECTED_VERTEX_STRIDE;
	protected static final int V_LOW_Y_CLIP_A = V_LOW_X_CLIP_B + PROJECTED_VERTEX_STRIDE;
	protected static final int V_LOW_Y_CLIP_B = V_LOW_Y_CLIP_A + PROJECTED_VERTEX_STRIDE;
	protected static final int V_HIGH_X_CLIP_A = V_LOW_Y_CLIP_B + PROJECTED_VERTEX_STRIDE;
	protected static final int V_HIGH_X_CLIP_B = V_HIGH_X_CLIP_A + PROJECTED_VERTEX_STRIDE;
	protected static final int V_HIGH_Y_CLIP_A = V_HIGH_X_CLIP_B + PROJECTED_VERTEX_STRIDE;
	protected static final int V_HIGH_Y_CLIP_B = V_HIGH_Y_CLIP_A + PROJECTED_VERTEX_STRIDE;

	protected static final int VERTEX_DATA_LENGTH = V_HIGH_Y_CLIP_B + PROJECTED_VERTEX_STRIDE;

	protected final int[] vertexData = new int[VERTEX_DATA_LENGTH];

	// Boumds of current triangle - edge coordinates
	protected int minX;
	protected int minY;
	protected int maxX;
	protected int maxY;

	// Boumds of current triangle - pixel coordinates
	protected int minPixelX;
	protected int minPixelY;
	protected int maxPixelX;
	protected int maxPixelY;

	protected int xOrigin;
	protected int yOrigin;
	protected int zOrigin;

	protected double cameraX;
	protected double cameraY;
	protected double cameraZ;

	protected int offsetX;
	protected int offsetY;
	protected int offsetZ;

	// Barycentric coordinates at minX/minY corner
	protected final int[] wOrigin = new int[3];
	protected final int[] a = new int[3];
	protected final int[] b = new int[3];
	protected final int[] wRow = new int[3];

	protected final int[] aLow = new int[3];
	protected final int[] bLow = new int[3];
	protected final int[] abLow = new int[3];

	protected int occlusionRange;

	protected int edgeFlags;

	// edge function values at min bounding corner - not pixel centered
	//	protected final int[] cornerOrigin = new int[3];

	// TODO: remove
	//	protected int totalCount;
	//	protected int extTrue;
	//	protected int extFalse;
	//	protected int earlyExit;


	private final boolean testUp() { return testQuad(V110, V010, V011, V111); }
	private final boolean testDown() { return testQuad(V000, V100, V101, V001); }
	private final boolean testEast() { return testQuad(V101, V100, V110, V111); }
	private final boolean testWest() { return testQuad(V000, V001, V011, V010); }
	private final boolean testSouth() { return testQuad(V001, V101, V111, V011); }
	private final boolean testNorth() { return testQuad(V100, V000, V010, V110); }

	public final boolean isChunkVisible()  {
		CanvasWorldRenderer.innerTimer.start();
		final boolean result;

		final boolean early = isPointVisible(8, 8, 8);

		if (early)  { //isPointVisible(8, 8, 8)) {
			//			++earlyExit;
			result = true;
		} else {
			computeProjectedBoxBounds(0, 0, 0, 16, 16, 16);


			// rank tests by how directly they face - use distance from camera coordinates for this
			final int offsetX = this.offsetX;
			final int offsetY = this.offsetY;
			final int offsetZ = this.offsetZ;

			int testBits = 0;
			int nearBits  = 0;
			int xTest =  0;
			int yTest = 0;
			int zTest = 0;

			// if camera below top face can't be seen
			if (offsetY < -CAMERA_PRECISION_CHUNK_MAX) {
				// UP
				if (offsetY > -CAMERA_PRECISION_CHUNK_MAX - CAMERA_PRECISION_UNITY) {
					// if very close to plane then don't test - precision may give inconsistent results
					nearBits |= 2;
				}

				yTest = -offsetY + CAMERA_PRECISION_CHUNK_MAX;
				testBits |= 2;
			} else if (offsetY > 0) {
				// DOWN
				if (offsetY < CAMERA_PRECISION_UNITY) {
					nearBits |= 2;
				}

				yTest = offsetY;
				testBits |= 2;
			}

			if (offsetX < -CAMERA_PRECISION_CHUNK_MAX) {
				// EAST;
				if (offsetX > -CAMERA_PRECISION_CHUNK_MAX - CAMERA_PRECISION_UNITY) {
					nearBits |= 1;
				}

				xTest = -offsetX + CAMERA_PRECISION_CHUNK_MAX;
				testBits |= 1;
			} else if (offsetX > 0) {
				// WEST
				if (offsetX < CAMERA_PRECISION_UNITY) {
					nearBits |= 1;
				}

				xTest = offsetX;
				testBits |= 1;
			}

			if (offsetZ < -CAMERA_PRECISION_CHUNK_MAX) {
				// SOUTH
				if (offsetZ > -CAMERA_PRECISION_CHUNK_MAX - CAMERA_PRECISION_UNITY) {
					nearBits |= 4;
				}

				zTest = -offsetZ + CAMERA_PRECISION_CHUNK_MAX;
				testBits |= 4;
			} else if (offsetZ > 0) {
				// NORTH
				if (offsetZ < CAMERA_PRECISION_UNITY) {
					nearBits |= 4;
				}

				zTest = offsetZ;
				testBits |= 4;
			}

			// if only valid tests are very near, assume visible to avoid false negatives due to precision
			if (nearBits != 0 && (testBits & ~nearBits) == 0) {
				result = true;
			} else {
				switch (testBits)  {
				default:
				case 0b000:
					result = false;
					break;
				case 0b001:
					result = offsetX > 0 ? testWest() : testEast();
					break;
				case 0b010:
					result = offsetY > 0 ? testDown() : testUp();
					break;
				case 0b011:
					if (xTest > yTest) {
						result = (offsetX > 0 ? testWest() : testEast()) || (offsetY > 0 ? testDown() : testUp());
					} else {
						result = (offsetY > 0 ? testDown() : testUp()) || (offsetX > 0 ? testWest() : testEast());
					}
					break;
				case 0b100:
					result = offsetZ > 0 ? testNorth() : testSouth();
					break;
				case 0b101:
					if (xTest > zTest) {
						result = (offsetX > 0 ? testWest() : testEast()) || (offsetZ > 0 ? testNorth() : testSouth());
					} else {
						result = (offsetZ > 0 ? testNorth() : testSouth()) || (offsetX > 0 ? testWest() : testEast());
					}
					break;
				case 0b110:
					if (yTest > zTest) {
						result = (offsetY > 0 ? testDown() : testUp()) || (offsetZ > 0 ? testNorth() : testSouth());
					} else {
						result = (offsetZ > 0 ? testNorth() : testSouth()) || (offsetY > 0 ? testDown() : testUp());
					}
					break;
				case 0b111:
					if (xTest > yTest) {
						if  (zTest > xTest) {
							// z first
							result = (offsetZ > 0 ? testNorth() : testSouth())
									|| (offsetX > 0 ? testWest() : testEast())
									|| (offsetY > 0 ? testDown() : testUp());
						} else {
							// x first
							result = (offsetX > 0 ? testWest() : testEast())
									|| (offsetZ > 0 ? testNorth() : testSouth())
									|| (offsetY > 0 ? testDown() : testUp());
						}
					} else if (zTest > yTest) {
						// z first
						result = (offsetZ > 0 ? testNorth() : testSouth())
								|| (offsetY > 0 ? testDown() : testUp())
								|| (offsetX > 0 ? testWest() : testEast());
					} else {
						// y first
						result = (offsetY > 0 ? testDown() : testUp())
								|| (offsetZ > 0 ? testNorth() : testSouth())
								|| (offsetX > 0 ? testWest() : testEast());
					}
					break;
				}
			}
		}

		CanvasWorldRenderer.innerTimer.stop();

		//		// TODO: remove
		//		if (occlusionRange == PackedBox.RANGE_EXTREME) {
		//			if (result) {
		//				++extTrue;
		//			} else {
		//				++extFalse;
		//			}
		//		}
		//
		//		if (++totalCount == 1000000) {
		//			System.out.println(String.format("extreme true: %f  extreme false: %f", extTrue / 10000f, extFalse / 10000f));
		//			System.out.println(String.format("Early exit: %f", earlyExit / 10000f));
		//			System.out.println();
		//			totalCount = 0;
		//			extTrue = 0;
		//			extFalse = 0;
		//			earlyExit = 0;
		//		}

		return result;
	}

	public final boolean isBoxVisible(int packedBox) {
		final int x0  = PackedBox.x0(packedBox);
		final int y0  = PackedBox.y0(packedBox);
		final int z0  = PackedBox.z0(packedBox);
		final int x1  = PackedBox.x1(packedBox);
		final int y1  = PackedBox.y1(packedBox);
		final int z1  = PackedBox.z1(packedBox);


		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		// rank tests by how directly they face - use distance from camera coordinates for this
		final int offsetX = this.offsetX;
		final int offsetY = this.offsetY;
		final int offsetZ = this.offsetZ;

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
				return offsetX > bx0 ? testWest() : testEast();
			case 0b010:
				return offsetY > by0 ? testDown() : testUp();
			case 0b011:
				if (xTest > yTest) {
					return (offsetX > bx0 ? testWest() : testEast()) || (offsetY > by0 ? testDown() : testUp());
				} else {
					return (offsetY > by0 ? testDown() : testUp()) || (offsetX > bx0 ? testWest() : testEast());
				}
			case 0b100:
				return offsetZ > bz0 ? testNorth() : testSouth();
			case 0b101:
				if (xTest > zTest) {
					return (offsetX > bx0 ? testWest() : testEast()) || (offsetZ > bz0 ? testNorth() : testSouth());
				} else {
					return (offsetZ > bz0 ? testNorth() : testSouth()) || (offsetX > bx0 ? testWest() : testEast());
				}
			case 0b110:
				if (yTest > zTest) {
					return (offsetY > by0 ? testDown() : testUp()) || (offsetZ > bz0 ? testNorth() : testSouth());
				} else {
					return (offsetZ > bz0 ? testNorth() : testSouth()) || (offsetY > by0 ? testDown() : testUp());
				}
			case 0b111:
				if (xTest > yTest) {
					if  (zTest > xTest) {
						// z first
						return (offsetZ > bz0 ? testNorth() : testSouth())
								|| (offsetX > bx0 ? testWest() : testEast())
								|| (offsetY > by0 ? testDown() : testUp());
					} else {
						// x first
						return (offsetX > bx0 ? testWest() : testEast())
								|| (offsetZ > bz0 ? testNorth() : testSouth())
								|| (offsetY > by0 ? testDown() : testUp());
					}
				} else if (zTest > yTest) {
					// z first
					return (offsetZ > bz0 ? testNorth() : testSouth())
							|| (offsetY > by0 ? testDown() : testUp())
							|| (offsetX > bx0 ? testWest() : testEast());
				} else {
					// y first
					return (offsetY > by0 ? testDown() : testUp())
							|| (offsetZ > bz0 ? testNorth() : testSouth())
							|| (offsetX > bx0 ? testWest() : testEast());
				}
			}
		}
	}

	public final void occludeChunk()  {
		computeProjectedBoxBounds(0, 0, 0, 16, 16, 16);

		// PERF use same techniques as view test?
		if (offsetY < -CAMERA_PRECISION_CHUNK_MAX) drawQuad(V110, V010, V011, V111); // up
		if (offsetY > 0) drawQuad(V000, V100, V101, V001); // down
		if (offsetX < -CAMERA_PRECISION_CHUNK_MAX) drawQuad(V101, V100, V110, V111); // east
		if (offsetX > 0) drawQuad(V000, V001, V011, V010); // west
		if (offsetZ < -CAMERA_PRECISION_CHUNK_MAX) drawQuad(V001, V101, V111, V011); // south
		if (offsetZ > 0) drawQuad(V100, V000, V010, V110); // north
	}

	protected final void occlude(int x0, int y0, int z0, int x1, int y1, int z1) {
		computeProjectedBoxBounds(x0, y0, z0, x1, y1, z1);

		// PERF use same techniques as view test?
		if (offsetY < -(y1 << CAMERA_PRECISION_BITS)) drawQuad(V110, V010, V011, V111); // up
		if (offsetY > -(y0 << CAMERA_PRECISION_BITS)) drawQuad(V000, V100, V101, V001); // down
		if (offsetX < -(x1 << CAMERA_PRECISION_BITS)) drawQuad(V101, V100, V110, V111); // east
		if (offsetX > -(x0 << CAMERA_PRECISION_BITS)) drawQuad(V000, V001, V011, V010); // west
		if (offsetZ < -(z1 << CAMERA_PRECISION_BITS)) drawQuad(V001, V101, V111, V011); // south
		if (offsetZ > -(z0 << CAMERA_PRECISION_BITS)) drawQuad(V100, V000, V010, V110); // north
	}

	public final void occlude(int[] visData) {
		final int occlusionRange = this.occlusionRange;
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

	/**
	 * For early exit testing
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	protected boolean isPointVisible(int x, int y, int z) {
		final Matrix4fExt mvpMatrixExt = this.mvpMatrixExt;

		final float w = mvpMatrixExt.a30() * x + mvpMatrixExt.a31() * y + mvpMatrixExt.a32() * z + mvpMatrixExt.a33();
		final float tz = mvpMatrixExt.a20() * x + mvpMatrixExt.a21() * y + mvpMatrixExt.a22() * z + mvpMatrixExt.a23();

		if (w <= 0 || tz < 0 || tz > w) {
			return false;
		}

		final float iw = 1f / w;
		final float tx = HALF_PIXEL_WIDTH  * iw * (mvpMatrixExt.a00() * x + mvpMatrixExt.a01() * y + mvpMatrixExt.a02() * z + mvpMatrixExt.a03());
		final float ty = HALF_PIXEL_HEIGHT * iw * (mvpMatrixExt.a10() * x + mvpMatrixExt.a11() * y + mvpMatrixExt.a12() * z + mvpMatrixExt.a13());
		final int px = (int) tx + HALF_PIXEL_WIDTH;
		final int py = (int) ty + HALF_PIXEL_HEIGHT;

		if (px >= 0 && py >= 0 && px < PIXEL_WIDTH && py < PIXEL_HEIGHT && testPixel(px, py)) {
			return true;
		}

		return false;
	}

	protected final void computeProjectedBoxBounds(int x0, int y0, int z0, int x1, int y1, int z1) {
		final int[] vertexData = this.vertexData;
		setupVertex(vertexData, V000, x0, y0, z0, mvpMatrixExt);
		setupVertex(vertexData, V001, x0, y0, z1, mvpMatrixExt);
		setupVertex(vertexData, V010, x0, y1, z0, mvpMatrixExt);
		setupVertex(vertexData, V011, x0, y1, z1, mvpMatrixExt);
		setupVertex(vertexData, V100, x1, y0, z0, mvpMatrixExt);
		setupVertex(vertexData, V101, x1, y0, z1, mvpMatrixExt);
		setupVertex(vertexData, V110, x1, y1, z0, mvpMatrixExt);
		setupVertex(vertexData, V111, x1, y1, z1, mvpMatrixExt);
	}

	public final void prepareScene(Matrix4f projectionMatrix, Matrix4f modelMatrix, Camera camera) {
		this.projectionMatrix = projectionMatrix.copy();
		this.modelMatrix = modelMatrix.copy();
		final Vec3d vec3d = camera.getPos();
		cameraX = vec3d.getX();
		cameraY = vec3d.getY();
		cameraZ = vec3d.getZ();
	}

	public final void clearScene() {
		System.arraycopy(EMPTY_BITS, 0, lowBins, 0, LOW_BIN_COUNT);
		System.arraycopy(EMPTY_BITS, 0, midBins, 0, MID_BIN_WORDS);
		System.arraycopy(EMPTY_BITS, 0, topBins, 0, TOP_BIN_WORDS);
	}

	public final void prepareChunk(BlockPos origin, int occlusionRange) {
		this.occlusionRange = occlusionRange;
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

	protected abstract void drawQuad(int v0, int v1, int v2, int v3);

	protected abstract void drawTri(int v0, int v1, int v2);

	protected abstract boolean testQuad(int v0, int v1, int v2, int v3);

	protected abstract boolean testTri(int v0, int v1, int v2);

	/**
	 *
	 * @param v0
	 * @param v1
	 * @param v2
	 * @return constant value from BoundsResult
	 */
	protected final int prepareTriBounds(int v0, int v1, int v2) {
		final int[] vertexData = this.vertexData;
		final int x0 = vertexData[v0 + PV_PX];
		final int y0 = vertexData[v0 + PV_PY];
		final int x1 = vertexData[v1 + PV_PX];
		final int y1 = vertexData[v1 + PV_PY];
		final int x2 = vertexData[v2 + PV_PX];
		final int y2 = vertexData[v2 + PV_PY];

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

		if (maxY < 0 || minY >= PRECISE_HEIGHT) {
			return BoundsResult.OUT_OF_BOUNDS;
		}

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

		if (maxX < 0 || minX >= PRECISE_WIDTH) {
			return BoundsResult.OUT_OF_BOUNDS;
		}

		if (minX < -GUARD_SIZE || minY < -GUARD_SIZE || maxX > GUARD_WIDTH || maxY > GUARD_HEIGHT) {
			return BoundsResult.NEEDS_CLIP;
		}

		int minPixelX = (minX + PRECISE_PIXEL_CENTER - 1) >> PRECISION_BITS;
		int minPixelY = (minY + PRECISE_PIXEL_CENTER - 1) >> PRECISION_BITS;
		int maxPixelX = (maxX - PRECISE_PIXEL_CENTER) >> PRECISION_BITS;
		int maxPixelY = (maxY - PRECISE_PIXEL_CENTER) >> PRECISION_BITS;

		if (minPixelX < 0) {
			minPixelX = 0;
		}

		if (maxPixelX > PIXEL_WIDTH - 1)  {
			maxPixelX = PIXEL_WIDTH  - 1;
		}

		if (minPixelY < 0) {
			minPixelY = 0;
		}

		if (maxPixelY > PIXEL_HEIGHT - 1)  {
			maxPixelY = PIXEL_HEIGHT - 1;
		}

		this.minPixelX = minPixelX;
		this.minPixelY = minPixelY;
		this.maxPixelX = maxPixelX;
		this.maxPixelY = maxPixelY;

		this.minX = minX >> PRECISION_BITS;
		this.minY = minY >> PRECISION_BITS;
		this.maxX = maxX >> PRECISION_BITS;
		this.maxY = maxY >> PRECISION_BITS;

		return BoundsResult.IN_BOUNDS;
	}

	private void prepareTriLowA() {
		for (int i = 0; i < 3; ++i) {
			aLow[i] = a[i] * LOW_BIN_PIXEL_DIAMETER_VECTOR[i];
		}
	}

	private void prepareTriLowB() {
		for (int i = 0; i < 3; ++i) {
			bLow[i] = b[i] * LOW_BIN_PIXEL_DIAMETER_VECTOR[i];
		}
	}

	private void prepareTriLowAB() {
		for (int i = 0; i < 3; ++i) {
			abLow[i] = aLow[i] + bLow[i];
		}
	}

	// TODO: remove
	protected int v0 = 0, v1 = 0, v2 = 0;

	protected void prepareTriScan(int v0, int v1, int v2) {

		this.v0 = v0;
		this.v1 = v1;
		this.v2 = v2;

		final int[] vertexData = this.vertexData;
		final int x0 = vertexData[v0 + PV_PX];
		final int y0 = vertexData[v0 + PV_PY];
		final int x1 = vertexData[v1 + PV_PX];
		final int y1 = vertexData[v1 + PV_PY];
		final int x2 = vertexData[v2 + PV_PX];
		final int y2 = vertexData[v2 + PV_PY];

		final int a0 = (y0 - y1);
		final int b0 = (x1 - x0);
		final int a1 = (y1 - y2);
		final int b1 = (x2 - x1);
		final int a2 = (y2 - y0);
		final int b2 = (x0 - x2);


		final boolean isTopLeft0 = a0 > 0 || (a0 == 0 && b0 < 0);
		final boolean isTopLeft1 = a1 > 0 || (a1 == 0 && b1 < 0);
		final boolean isTopLeft2 = a2 > 0 || (a2 == 0 && b2 < 0);

		final long cx = (minPixelX << PRECISION_BITS) + PRECISE_PIXEL_CENTER;
		final long cy = (minPixelY << PRECISION_BITS) + PRECISE_PIXEL_CENTER;

		// Barycentric coordinates at minX/minY corner
		// Can reduce precision (with accurate rounding) because increments will always be multiple of full pixel width
		wOrigin[0] = (int) ((orient2d(x0, y0, x1, y1, cx, cy) + (isTopLeft0 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
		wOrigin[1] = (int) ((orient2d(x1, y1, x2, y2, cx, cy) + (isTopLeft1 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);
		wOrigin[2] = (int) ((orient2d(x2, y2, x0, y0, cx, cy) + (isTopLeft2 ? PRECISE_PIXEL_CENTER : (PRECISE_PIXEL_CENTER - 1))) >> PRECISION_BITS);

		//		final long ecx = minX << PRECISION_BITS;
		//		final long ecy = minY << PRECISION_BITS;

		//		cornerOrigin[0] = (int) (orient2d(x0, y0, x1, y1, ecx, ecy) >> PRECISION_BITS);
		//		cornerOrigin[1] = (int) (orient2d(x1, y1, x2, y2, ecx, ecy) >> PRECISION_BITS);
		//		cornerOrigin[2] = (int) (orient2d(x2, y2, x0, y0, ecx, ecy) >> PRECISION_BITS);

		a[0] = a0;
		a[1] = a1;
		a[2] = a2;

		b[0] = b0;
		b[1] = b1;
		b[2] = b2;

		edgeFlags = edgeFlag(a0, b0) | (edgeFlag(a1, b1) << EDGE_SHIFT_1) | (edgeFlag(a2, b2) << EDGE_SHIFT_2);

		prepareTriLowA();
		prepareTriLowB();
		prepareTriLowAB();
	}

	protected void computeRow(final int dx, final int dy) {
		for (int i = 0; i < 3; ++i)  {
			wRow[i] = wOrigin[i] + a[i] * dx + b[i] * dy;
		}
	}

	protected final long orient2d(long x0, long y0, long x1, long y1, long cx, long cy) {
		return ((x1 - x0) * (cy - y0) - (y1 - y0) * (cx - x0));
	}

	protected  final boolean testPixel(int x, int y) {
		return (lowBins[lowIndexFromPixelXY(x, y)] & (1L << (pixelIndex(x, y)))) == 0;
	}

	protected void drawPixel(int x, int y) {
		lowBins[lowIndexFromPixelXY(x, y)] |= (1L << (pixelIndex(x, y)));
	}

	private long nextTime;

	public final void outputRaster() {
		if (!ENABLE_RASTER_OUTPUT) {
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

	private static int mortonNumber(int x, int y) {
		int z = (x & 0b001) | ((y & 0b001) << 1);
		z |= ((x & 0b010) << 1) | ((y & 0b010) << 2);
		return z | ((x & 0b100) << 2) | ((y & 0b100) << 3);
	}

	protected static int midIndex(int midX, int midY) {
		final int topX = (midX >> LOW_AXIS_SHIFT);
		final int topY = (midY >> LOW_AXIS_SHIFT);
		return (topIndex(topX, topY) << MID_AXIS_SHIFT) | (mortonNumber(midX, midY));
	}

	protected static int topIndex(int topX, int topY) {
		return (topY << TOP_Y_SHIFT) | topX;
	}

	protected static int lowIndex(int lowX, int lowY) {
		final int midX = (lowX >> LOW_AXIS_SHIFT) & BIN_PIXEL_INDEX_MASK;
		final int midY = (lowY >> LOW_AXIS_SHIFT) & BIN_PIXEL_INDEX_MASK;

		final int topX = (lowX >> MID_AXIS_SHIFT);
		final int topY = (lowY >> MID_AXIS_SHIFT);

		return (topIndex(topX, topY) << TOP_INDEX_SHIFT) | (mortonNumber(midX, midY) << MID_INDEX_SHIFT) | mortonNumber(lowX & BIN_PIXEL_INDEX_MASK, lowY & BIN_PIXEL_INDEX_MASK);
	}

	protected static int lowIndexFromPixelXY(int x, int y)  {
		return lowIndex(x >>> LOW_AXIS_SHIFT, y >>> LOW_AXIS_SHIFT);
	}

	protected static int pixelIndex(int x, int y)  {
		return  ((y & BIN_PIXEL_INDEX_MASK) << BIN_AXIS_SHIFT) | (x & BIN_PIXEL_INDEX_MASK);
	}

	protected static boolean isPixelClear(long word, int x, int y)  {
		return (word & (1L << pixelIndex(x, y))) == 0;
	}

	protected static long pixelMask(int x, int y) {
		return 1L << pixelIndex(x, y);
	}

	/** REQUIRES 0-7 inputs! */
	protected static boolean testPixelInWordPreMasked(long word, int x, int y) {
		return (word & (1L << ((y << BIN_AXIS_SHIFT) | x))) == 0;
	}

	protected static long setPixelInWordPreMasked(long word, int x, int y) {
		return word | (1L << ((y << BIN_AXIS_SHIFT) | x));
	}

	/**
	 *
	 * @param x0 zero to seven
	 * @param y0 zero to seven
	 * @param x1 zero to seven
	 * @param y1 zero to seven
	 * @return
	 */
	protected static long coverageMask(int x0, int y0, int x1, int y1) {
		return COVERAGE_MASKS[x0 | (x1 << 3)] & COVERAGE_MASKS[64 | y0 | (y1 << 3)];
	}

	protected static void printCoverageMask(long mask) {
		final String s = Strings.padStart(Long.toBinaryString(mask), 64, '0');
		OcclusionBitPrinter.printSpaced(s.substring(0, 8));
		OcclusionBitPrinter.printSpaced(s.substring(8, 16));
		OcclusionBitPrinter.printSpaced(s.substring(16, 24));
		OcclusionBitPrinter.printSpaced(s.substring(24, 32));
		OcclusionBitPrinter.printSpaced(s.substring(32, 40));
		OcclusionBitPrinter.printSpaced(s.substring(40, 48));
		OcclusionBitPrinter.printSpaced(s.substring(48, 56));
		OcclusionBitPrinter.printSpaced(s.substring(56, 64));
		System.out.println();
	}


	//Y starts  at  64
	private static long COVERAGE_MASKS[] = new long[128];

	static {
		for (int min = 0; min <= 7; min++) {
			for (int max = 0; max <= 7; max++) {
				if (min <= max) {
					long xBits = 0;
					long yBits = 0;

					for (int i = min; i <= max; i++) {
						for (int j = 0; j <= 7; j++) {
							xBits |= pixelMask(i, j);
							yBits |= pixelMask(j, i);
						}
					}

					final int index = min | (max << 3);
					COVERAGE_MASKS[index] = xBits;
					COVERAGE_MASKS[index + 64] = yBits;
				}
			}
		}
	}

	protected static final boolean ENABLE_RASTER_OUTPUT = Configurator.debugOcclusionRaster;

	protected static final int BIN_AXIS_SHIFT = 3;
	protected static final int BIN_PIXEL_DIAMETER = 1 << BIN_AXIS_SHIFT;
	protected static final int BIN_PIXEL_INDEX_MASK = BIN_PIXEL_DIAMETER - 1;
	protected static final int BIN_PIXEL_INVERSE_MASK = ~BIN_PIXEL_INDEX_MASK;

	protected static final int LOW_AXIS_SHIFT = BIN_AXIS_SHIFT;
	protected static final int MID_AXIS_SHIFT = BIN_AXIS_SHIFT * 2;
	protected static final int TOP_AXIS_SHIFT = BIN_AXIS_SHIFT * 3;

	protected static final int MID_INDEX_SHIFT = LOW_AXIS_SHIFT * 2;
	protected static final int TOP_INDEX_SHIFT = MID_INDEX_SHIFT * 2;

	protected static final int TOP_WIDTH = 2;
	protected static final int TOP_Y_SHIFT = Integer.bitCount(TOP_WIDTH - 1);
	protected static final int TOP_HEIGHT = 1;

	protected static final int MID_WIDTH = TOP_WIDTH  * 8;
	protected static final int MID_Y_SHIFT = Integer.bitCount(MID_WIDTH - 1);
	protected static final int MIDDLE_HEIGHT = TOP_HEIGHT  * 8;

	protected static final int PRECISION_BITS = 4;
	protected static final int PRECISE_FRACTION_MASK = (1 << PRECISION_BITS) - 1;
	protected static final int PRECISE_INTEGER_MASK = ~PRECISE_FRACTION_MASK;
	protected static final int PRECISE_PIXEL_CENTER = 1 << (PRECISION_BITS - 1);

	protected static final int LOW_WIDTH = MID_WIDTH * 8;
	//protected static final int LOW_Y_SHIFT = Integer.bitCount(LOW_WIDTH - 1);
	protected static final int PIXEL_WIDTH = LOW_WIDTH * BIN_PIXEL_DIAMETER;
	protected static final int HALF_PIXEL_WIDTH = PIXEL_WIDTH / 2;
	protected static final int PRECISE_WIDTH = PIXEL_WIDTH << PRECISION_BITS;
	protected static final int HALF_PRECISE_WIDTH = PRECISE_WIDTH / 2;

	protected static final int LOW_HEIGHT = MIDDLE_HEIGHT * 8;
	protected static final int PIXEL_HEIGHT = LOW_HEIGHT * BIN_PIXEL_DIAMETER;
	protected static final int HALF_PIXEL_HEIGHT = PIXEL_HEIGHT / 2;
	//	protected static final int HEIGHT_WORD_RELATIVE_SHIFT = LOW_Y_SHIFT - BIN_AXIS_SHIFT;
	protected static final int PRECISE_HEIGHT = PIXEL_HEIGHT << PRECISION_BITS;
	protected static final int HALF_PRECISE_HEIGHT = PRECISE_HEIGHT / 2;

	protected static final int GUARD_SIZE = 512 << PRECISION_BITS;
	protected static final int GUARD_WIDTH = PRECISE_WIDTH + GUARD_SIZE;
	protected static final int GUARD_HEIGHT = PRECISE_HEIGHT + GUARD_SIZE;

	protected static final int LOW_BIN_COUNT = LOW_WIDTH * LOW_HEIGHT;
	protected static final int MID_BIN_COUNT = MID_WIDTH * LOW_HEIGHT;
	// two words per bin -  first indicates full coverage, second indicates partial
	protected static final int MID_BIN_WORDS = MID_BIN_COUNT * 2;
	protected static final int TOP_BIN_COUNT = TOP_WIDTH * TOP_HEIGHT;
	// two words per bin -  first indicates full coverage, second indicates partial
	protected static final int TOP_BIN_WORDS = TOP_BIN_COUNT * 2;

	protected static final int TOP_BIN_PIXEL_DIAMETER = PIXEL_WIDTH / TOP_WIDTH;
	protected static final int TOP_BIN_PIXEL_INDEX_MASK = TOP_BIN_PIXEL_DIAMETER - 1;

	protected static final int MID_BIN_PIXEL_DIAMETER = PIXEL_WIDTH / MID_WIDTH;
	protected static final int MID_BIN_PIXEL_INDEX_MASK = MID_BIN_PIXEL_DIAMETER - 1;

	protected static final int LOW_BIN_PIXEL_DIAMETER = PIXEL_WIDTH / LOW_WIDTH;
	protected static final int LOW_BIN_PIXEL_INDEX_MASK = LOW_BIN_PIXEL_DIAMETER - 1;

	protected static final long[] EMPTY_BITS = new long[LOW_BIN_COUNT];

	protected static final int CAMERA_PRECISION_BITS = 12;
	protected static final int CAMERA_PRECISION_UNITY = 1 << CAMERA_PRECISION_BITS;
	protected static final int CAMERA_PRECISION_CHUNK_MAX = 16 * CAMERA_PRECISION_UNITY;

	protected static final int[] LOW_BIN_PIXEL_DIAMETER_VECTOR = {LOW_BIN_PIXEL_DIAMETER - 1, LOW_BIN_PIXEL_DIAMETER - 1, LOW_BIN_PIXEL_DIAMETER - 1};
	protected static final int[] MID_BIN_PIXEL_DIAMETER_VECTOR = {MID_BIN_PIXEL_DIAMETER - 1, MID_BIN_PIXEL_DIAMETER - 1, MID_BIN_PIXEL_DIAMETER - 1};

	// Edge classifications - refers to position in the triangle.
	// Things above top edge, for example, are outside the edge.
	protected static final int EDGE_TOP = 0;
	protected static final int EDGE_BOTTOM = 1;
	protected static final int EDGE_LEFT = 2;
	protected static final int EDGE_RIGHT = 3;
	protected static final int EDGE_TOP_LEFT = 4;
	protected static final int EDGE_TOP_RIGHT = 5;
	protected static final int EDGE_BOTTOM_LEFT = 6;
	protected static final int EDGE_BOTTOM_RIGHT = 7;

	protected static final int EDGE_SHIFT_1 = 3;
	protected static final int EDGE_SHIFT_2 = 6;
	protected static final int EDGE_MASK = 7;

	protected static int edgeFlag(int a, int b) {
		if (a == 0) {
			return b > 0 ? EDGE_BOTTOM : EDGE_TOP;
		} else if (b == 0) {
			return a > 0 ? EDGE_LEFT : EDGE_RIGHT;
		}

		if (a > 0) {
			return b > 0 ? EDGE_BOTTOM_LEFT : EDGE_TOP_LEFT;
		}  else { // a < 0
			return b > 0 ? EDGE_BOTTOM_RIGHT : EDGE_TOP_RIGHT;
		}
	}
}

