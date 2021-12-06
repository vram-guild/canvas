/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.terrain.region;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;

import grondag.canvas.render.world.WorldRenderState;

public class RenderChunk {
	final WorldRenderState worldRenderState;

	private int chunkX;
	private int chunkZ;
	private RenderRegion[] regions = null;
	private boolean areCornersLoadedCache = false;

	private long cameraRegionOrigin = -1;

	int horizontalSquaredDistance;

	public RenderChunk(WorldRenderState worldRenderState) {
		this.worldRenderState = worldRenderState;
	}

	private void open(int chunkX, int chunkZ) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		regions = new RenderRegion[RenderRegionIndexer.MAX_Y_REGIONS];
		areCornersLoadedCache = false;
		cameraRegionOrigin = -1;
		computeChunkDistanceMetrics();
	}

	public synchronized void close() {
		if (regions != null) {
			for (final RenderRegion region : regions) {
				if (region != null) {
					region.close();
				}
			}

			regions = null;
		}
	}

	public boolean areCornersLoaded() {
		return areCornersLoadedCache || areCornerChunksLoaded();
	}

	private boolean areCornerChunksLoaded() {
		final ClientLevel world = worldRenderState.getWorld();

		final boolean result = world.getChunk(chunkX - 1, chunkZ - 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX - 1, chunkZ + 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX + 1, chunkZ - 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX + 1, chunkZ + 1, ChunkStatus.FULL, false) != null;

		areCornersLoadedCache = result;

		return result;
	}

	synchronized void updatePositionAndVisibility() {
		computeChunkDistanceMetrics();

		final RenderRegion[] regions = this.regions;

		if (regions != null) {
			for (int i = 0; i < RenderRegionIndexer.MAX_Y_REGIONS; ++i) {
				final RenderRegion r = regions[i];

				if (r != null) {
					r.origin.update();
				}
			}

			if (horizontalSquaredDistance > worldRenderState.maxSquaredChunkRetentionDistance()) {
				worldRenderState.renderRegionStorage.scheduleClose(this);
			}
		}
	}

	private void computeChunkDistanceMetrics() {
		final long cameraRegionOrigin = worldRenderState.terrainIterator.cameraRegionOrigin();

		if (this.cameraRegionOrigin != cameraRegionOrigin) {
			this.cameraRegionOrigin = cameraRegionOrigin;
			final int cx = (BlockPos.getX(cameraRegionOrigin) >> 4) - chunkX;
			final int cz = (BlockPos.getZ(cameraRegionOrigin) >> 4) - chunkZ;
			horizontalSquaredDistance = cx * cx + cz * cz;
		}
	}

	synchronized @Nullable RenderRegion getOrCreateRegion(int x, int y, int z) {
		final int i = (y + RenderRegionIndexer.Y_BLOCKPOS_OFFSET) >> 4;

		if (i < 0 || i >= RenderRegionIndexer.MAX_Y_REGIONS) {
			return null;
		}

		RenderRegion[] regions = this.regions;

		if (regions == null) {
			open(x >> 4, z >> 4);
			regions = this.regions;
		}

		RenderRegion r = regions[i];

		if (r == null) {
			r = new RenderRegion(this, RenderRegionIndexer.blockPosToRegionOrigin(x, y, z));
			regions[i] = r;
		}

		return r;
	}

	synchronized RenderRegion getRegionIfExists(int x, int y, int z) {
		final int i = (y + RenderRegionIndexer.Y_BLOCKPOS_OFFSET) >> 4;

		if (i < 0 || i >= RenderRegionIndexer.MAX_Y_REGIONS) {
			return null;
		}

		final RenderRegion[] regions = this.regions;
		return regions == null ? null : regions[i];
	}

	public long cameraRegionOrigin() {
		return cameraRegionOrigin;
	}
}
