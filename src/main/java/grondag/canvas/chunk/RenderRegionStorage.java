package grondag.canvas.chunk;

import java.util.function.IntUnaryOperator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RenderRegionStorage {
	private static final int SIZE_Y = 16;
	private final int xySize;
	private final IntUnaryOperator modFunc;
	private final BuiltRenderRegion[] regions;

	private final int regionCount;

	private int positionVersion;

	private int regionVersion = -1;

	// position of the player for last origin update
	//	private double playerX;
	//	private double playerY;
	//	private double playerZ;

	// chunk coords of the player for last origin update
	private int playerChunkX;
	private int playerChunkY;
	private int playerChunkZ;

	public RenderRegionStorage(RenderRegionBuilder regionBuilder, int viewDistance) {
		xySize = viewDistance * 2 + 1;
		modFunc = FastFloorMod.get(viewDistance);
		regionCount = xySize * SIZE_Y * xySize;
		regions = new BuiltRenderRegion[regionCount];

		for(int x = 0; x < xySize; ++x) {
			for(int z = 0; z < xySize; ++z) {
				final RegionChunkReference chunkReference = new RegionChunkReference();

				for(int y = 0; y < SIZE_Y; ++y) {
					final int i = getRegionIndex(x, y, z);
					final BuiltRenderRegion r = new BuiltRenderRegion(regionBuilder, this, chunkReference, y == 0);
					r.setOrigin(x << 4, y << 4, z << 4, i);
					regions[i] = r;
				}
			}
		}
	}

	public void clear() {
		final BuiltRenderRegion[] regions = this.regions;
		final int limit = regions.length;

		for(int i = 0; i < limit; ++i) {
			final BuiltRenderRegion region = regions[i];
			region.delete();
		}
	}

	private int getRegionIndex(int x, int y, int z) {
		return (((z * xySize) + x) << 4) + y;
	}


	public void updateRegionOriginsIfNeeded(MinecraftClient mc) {
		final int cx = mc.player.chunkX;
		final int cy = mc.player.chunkY;
		final int cz = mc.player.chunkZ;

		if (playerChunkX != cx || playerChunkY != cy || playerChunkZ != cz) {
			playerChunkX = cx;
			playerChunkY = cy;
			playerChunkZ = cz;
			++regionVersion;
			updateRegionOrigins(mc.player.getX(), mc.player.getZ());
		}

		// TODO: remove if not keeping (also field declarations)
		//		final double x = mc.player.getX();
		//		final double y = mc.player.getY();
		//		final double z = mc.player.getZ();
		//		final double dx = playerX - x;
		//		final double dy = playerY - y;
		//		final double dz = playerZ - z;


		//		if (playerChunkX != cx || playerChunkY != cy || playerChunkZ != cz || dx * dx + dy * dy + dz * dz > 16.0D) {
		//			playerX = x;
		//			playerY = y;
		//			playerZ = z;
		//			playerChunkX = cx;
		//			playerChunkY = cy;
		//			playerChunkZ = cz;
		//			updateRegionOrigins(mc.player.getX(), mc.player.getZ());
		//		}

	}

	public void updateRegionOrigins(double playerX, double playerZ) {
		final int cx = MathHelper.floor(playerX);
		final int cz = MathHelper.floor(playerZ);
		final int xDist = xySize << 4;
		final int dcx = cx - 8 - xDist / 2;
		final int zDist = xySize << 4;
		final int dcz = cz - 8 - zDist / 2;

		for(int dx = 0; dx < xySize; ++dx) {
			final int x = dcx + Math.floorMod((dx << 4) - dcx, xDist);

			for(int dz = 0; dz < xySize; ++dz) {
				final int z = dcz + Math.floorMod((dz << 4) - dcz, zDist);

				for(int dy = 0; dy < SIZE_Y; ++dy) {
					final int y = dy << 4;
					final int index = getRegionIndex(dx, dy, dz);
					final BuiltRenderRegion builtChunk = regions[index];
					builtChunk.setOrigin(x, y, z, index);
				}
			}
		}


	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		if ((y & 0xFFFFFFF0) == 0) {
			regions[getRegionIndex(modFunc.applyAsInt(x), y, modFunc.applyAsInt(z))].markForBuild(urgent);
		}
	}

	/**
	 * Used when coordinates may be out of view.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @return -1 if out of bounds
	 */
	public int getRegionIndexFromBlockPos(int x, int y, int z) {
		if ((y & 0xFFFFFF00) != 0) {
			return -1;
		}

		return getRegionIndex(modFunc.applyAsInt(x >> 4), y >> 4, modFunc.applyAsInt(z >> 4));
	}

	public int getRegionIndexFromBlockPos(BlockPos pos) {
		return getRegionIndexFromBlockPos(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Called each frame, but only updates when player has moved more than 1 block.
	 * Uses position version to detect the movement.
	 */
	public void updateCameraDistance(Vec3d cameraPos, int positionVersion, int renderDistance) {
		if (this.positionVersion == positionVersion) {
			return;
		}

		final int maxRenderDistance = renderDistance * renderDistance * 256;
		this.positionVersion = positionVersion;
		final double x = cameraPos.x;
		final double y = cameraPos.y;
		final double z = cameraPos.z;

		for (final BuiltRenderRegion chunk : regions) {
			chunk.updateCameraDistance(x, y, z, maxRenderDistance);
		}
	}

	public BuiltRenderRegion[] regions() {
		return regions;
	}

	public int regionCount() {
		return regionCount;
	}

	public int regionVersion() {
		return regionVersion;
	}

	public BuiltRenderRegion getRegion(BlockPos pos) {
		final int index = getRegionIndexFromBlockPos(pos);
		return index == -1 ? null : regions[index];
	}

	static int fastFloorDiv(int x, int y) {
		int r = x / y;
		// if the signs are different and modulo not zero, round down
		if ((x ^ y) < 0 && (r * y != x)) {
			r--;
		}
		return r;
	}
}
