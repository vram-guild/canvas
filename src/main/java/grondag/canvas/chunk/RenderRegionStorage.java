package grondag.canvas.chunk;

import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RenderRegionStorage {
	private int sizeY;
	private int sizeX;
	private int sizeZ;
	private final BuiltRenderRegion[] regions;
	private final BuiltRenderRegion[] sortedRegions;
	private int lastCameraX;
	private int lastCameraY;
	private int lastCameraZ;
	private boolean isSortDirty = true;

	private final IntComparator comparator;

	private final Swapper swapper;

	public RenderRegionStorage(RenderRegionBuilder regionBuilder, int viewDistance) {
		setViewDistance(viewDistance);
		final int i = sizeX * sizeY * sizeZ;
		regions = new BuiltRenderRegion[i];
		sortedRegions = new BuiltRenderRegion[i];

		comparator = (a, b) -> Integer.compare(sortedRegions[a].squaredCameraDistance(), sortedRegions[b].squaredCameraDistance());

		swapper = (a, b) -> {
			final BuiltRenderRegion swap = sortedRegions[a];
			sortedRegions[a] = sortedRegions[b];
			sortedRegions[b] = swap;
		};

		for(int j = 0; j < sizeX; ++j) {
			for(int k = 0; k < sizeY; ++k) {
				for(int l = 0; l < sizeZ; ++l) {
					final int m = getRegionIndex(j, k, l);
					final BuiltRenderRegion r = new BuiltRenderRegion(regionBuilder);
					r.setOrigin(j * 16, k * 16, l * 16, this);
					regions[m] = r;
					sortedRegions[m] = r;
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

	public int getRegionIndex(int i, int j, int k) {
		return (k * sizeY + j) * sizeX + i;
	}

	private void setViewDistance(int i) {
		final int j = i * 2 + 1;
		sizeX = j;
		sizeY = 16;
		sizeZ = j;
	}

	public void updateRegionOrigins(double playerX, double playerZ) {
		final int i = MathHelper.floor(playerX);
		final int j = MathHelper.floor(playerZ);

		for(int k = 0; k < sizeX; ++k) {
			final int l = sizeX * 16;
			final int m = i - 8 - l / 2;
			final int x = m + Math.floorMod(k * 16 - m, l);

			for(int o = 0; o < sizeZ; ++o) {
				final int p = sizeZ * 16;
				final int q = j - 8 - p / 2;
				final int z = q + Math.floorMod(o * 16 - q, p);

				for(int s = 0; s < sizeY; ++s) {
					final int y = s * 16;
					final BuiltRenderRegion builtChunk = regions[getRegionIndex(k, s, o)];
					builtChunk.setOrigin(x, y, z, this);
				}
			}
		}

		isSortDirty = true;
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		final int l = Math.floorMod(x, sizeX);
		final int m = Math.floorMod(y, sizeY);
		final int n = Math.floorMod(z, sizeZ);
		final BuiltRenderRegion builtChunk = regions[getRegionIndex(l, m, n)];
		builtChunk.markForBuild(urgent);
	}

	/**
	 * Used when coordinates may be out of view.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @return -1 if out of bounds
	 */
	public int getRegionIndexSafely(int x, int y, int z) {
		int i = MathHelper.floorDiv(x, 16);
		final int j = MathHelper.floorDiv(y, 16);
		int k = MathHelper.floorDiv(z, 16);

		if (j >= 0 && j < sizeY) {
			i = MathHelper.floorMod(i, sizeX);
			k = MathHelper.floorMod(k, sizeZ);
			return getRegionIndex(i, j, k);
		} else {
			return -1;
		}
	}

	public int getRegionIndexSafely(BlockPos pos) {
		return getRegionIndexSafely(pos.getX(), pos.getY(), pos.getZ());
	}

	/** Called each frame. Avoids recalc on each lookup, which also happens every frame */
	public void updateCameraDistance(Vec3d cameraPos) {
		final double xExact = cameraPos.x;
		final double yExact = cameraPos.y;
		final double zExact = cameraPos.z;

		final int x = (int) Math.round(xExact);
		final int y = (int) Math.round(yExact);
		final int z = (int) Math.round(zExact);

		if (x == lastCameraX && y == lastCameraY && z == lastCameraZ) {
			return;
		}

		lastCameraX = x;
		lastCameraY = y;
		lastCameraZ = z;

		for (final BuiltRenderRegion chunk : regions) {
			chunk.updateCameraDistance(xExact, yExact, zExact);
		}

		isSortDirty = true;
	}

	public BuiltRenderRegion[] regions() {
		return regions;
	}

	public BuiltRenderRegion[] sortedRegions() {
		if (isSortDirty) {
			isSortDirty = false;

			it.unimi.dsi.fastutil.Arrays.quickSort(0, sortedRegions.length, comparator, swapper);
		}

		return sortedRegions;
	}
}
