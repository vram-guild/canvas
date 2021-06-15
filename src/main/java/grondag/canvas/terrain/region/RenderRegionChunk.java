/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.terrain.region;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.ChunkStatus;

public class RenderRegionChunk {
	final RenderRegionStorage storage;

	private int chunkX;
	private int chunkZ;
	private BuiltRenderRegion[] regions = null;
	private boolean areCornersLoadedCache = false;
	int chunkDistVersion = -1;
	int horizontalSquaredDistance;

	public RenderRegionChunk(RenderRegionStorage storage) {
		this.storage = storage;
	}

	private void open(int chunkX, int chunkZ) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		regions = new BuiltRenderRegion[RenderRegionIndexer.MAX_Y_REGIONS];
		areCornersLoadedCache = false;
		chunkDistVersion = -1;
	}

	public synchronized void close() {
		if (regions != null) {
			for (final BuiltRenderRegion region : regions) {
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
		final ClientWorld world = storage.cwr.getWorld();

		final boolean result = world.getChunk(chunkX - 1, chunkZ - 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX - 1, chunkZ + 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX + 1, chunkZ - 1, ChunkStatus.FULL, false) != null
				&& world.getChunk(chunkX + 1, chunkZ + 1, ChunkStatus.FULL, false) != null;

		areCornersLoadedCache = result;

		return result;
	}

	synchronized void updateCameraDistanceAndVisibilityInfo() {
		if (storage.cameraChunkDistVersion != chunkDistVersion) {
			chunkDistVersion = storage.cameraChunkDistVersion;
			final int cx = storage.cameraChunkX() - chunkX;
			final int cz = storage.cameraChunkZ() - chunkZ;
			horizontalSquaredDistance = cx * cx + cz * cz;
		}

		final BuiltRenderRegion[] regions = this.regions;

		if (regions != null) {
			for (int i = 0; i < RenderRegionIndexer.MAX_Y_REGIONS; ++i) {
				final BuiltRenderRegion r = regions[i];

				if (r != null) {
					r.updateCameraDistanceAndVisibilityInfo();
				}
			}

			if (horizontalSquaredDistance > storage.cwr.maxSquaredChunkRetentionDistance()) {
				storage.scheduleClose(this);
			}
		}
	}

	synchronized BuiltRenderRegion getOrCreateRegion(int x, int y, int z) {
		final int i = (y + RenderRegionIndexer.Y_BLOCKPOS_OFFSET) >> 4;

		if (i < 0 || i >= RenderRegionIndexer.MAX_Y_REGIONS) {
			return null;
		}

		BuiltRenderRegion[] regions = this.regions;

		if (regions == null) {
			open(x >> 4, z >> 4);
			regions = this.regions;
		}

		BuiltRenderRegion r = regions[i];

		if (r == null) {
			r = new BuiltRenderRegion(this, RenderRegionIndexer.blockPosToRegionOrigin(x, y, z));
			regions[i] = r;
		}

		return r;
	}

	synchronized BuiltRenderRegion getRegionIfExists(int x, int y, int z) {
		final int i = (y + RenderRegionIndexer.Y_BLOCKPOS_OFFSET) >> 4;

		if (i < 0 || i >= RenderRegionIndexer.MAX_Y_REGIONS) {
			return null;
		}

		final BuiltRenderRegion[] regions = this.regions;
		return regions == null ? null : regions[i];
	}
}
