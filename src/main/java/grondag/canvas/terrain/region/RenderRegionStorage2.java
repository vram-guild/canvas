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

import java.util.Arrays;

import it.unimi.dsi.fastutil.Hash;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.util.HackedLong2ObjectMap;

public class RenderRegionStorage2 {
	public final RenderRegionPruner regionPruner;
	private final CanvasWorldRenderer cwr;
	private int regionCount;

	private final BuiltRenderRegion[] regions = new BuiltRenderRegion[128 * 128 * 16];

	private final HackedLong2ObjectMap<RegionChunkReference> chunkRefMap = new HackedLong2ObjectMap<RegionChunkReference>(2048, Hash.VERY_FAST_LOAD_FACTOR, r -> { }) {
		@Override
		protected boolean shouldPrune(RegionChunkReference item) {
			return item.isEmpty();
		}
	};

	private RegionChunkReference chunkRef(long packedOriginPos) {
		final long key = ChunkPos.toLong(BlockPos.unpackLongX(packedOriginPos) >> 4, BlockPos.unpackLongZ(packedOriginPos) >> 4);
		return chunkRefMap.computeIfAbsent(key, k -> new RegionChunkReference(cwr.getWorld(), key));
	}

	public RenderRegionStorage2(CanvasWorldRenderer canvasWorldRenderer, RenderRegionPruner pruner) {
		cwr = canvasWorldRenderer;
		regionPruner = pruner;
	}

	public synchronized void clear() {
		Arrays.fill(regions, null);
		chunkRefMap.clear();
	}

	private static int index(int x, int y, int z) {
		x = ((x + 30000000) >> 4) & 127;
		y = (y >> 4);
		z = ((z + 30000000) >> 4) & 127;

		return y | (x << 4) | (z << 11);
	}

	public void scheduleRebuild(int x, int y, int z, boolean urgent) {
		if ((y & 0xFFFFFF00) == 0) {
			final BuiltRenderRegion region = getRegionIfExists(x, y, z);

			if (region != null) {
				region.markForBuild(urgent);
			}
		}
	}

	public void updateCameraDistanceAndVisibilityInfo(long cameraChunkOrigin) {
		regionPruner.prepare(cameraChunkOrigin);
		pruneRegions();
		chunkRefMap.prune();

		if (regionPruner.didInvalidateOccluder()) {
			regionPruner.occluder.invalidate();
		}

		regionPruner.post();
	}

	private synchronized void pruneRegions() {
		final int limit = regions.length;

		for (int i = 0; i < limit; ++i) {
			final BuiltRenderRegion val = regions[i];

			if (val != null && val.shouldPrune()) {
				--regionCount;
				regions[i] = null;
			}
		}
	}

	public int regionCount() {
		return regionCount;
	}

	public synchronized BuiltRenderRegion getOrCreateRegion(int x, int y, int z) {
		if (y < 0 || y > 255) {
			return null;
		}

		final int i = index(x, y, z);
		BuiltRenderRegion r = regions[i];

		if (r == null) {
			final long k = BlockPos.asLong(x & 0xFFFFFFF0, y & 0xFFFFFFF0, z & 0xFFFFFFF0);
			r = new BuiltRenderRegion(cwr, this, chunkRef(k), k);
			regions[i] = r;
			++regionCount;
		}

		return r;
	}

	public BuiltRenderRegion getOrCreateRegion(BlockPos pos) {
		return getOrCreateRegion(pos.getX(), pos.getY(), pos.getZ());
	}

	public BuiltRenderRegion getRegionIfExists(BlockPos pos) {
		return getRegionIfExists(pos.getX(), pos.getY(), pos.getZ());
	}

	public synchronized BuiltRenderRegion getRegionIfExists(int x, int y, int z) {
		if (y < 0 || y > 255) {
			return null;
		}

		// WIP: handle world border
		return regions[index(x, y, z)];
	}

	public boolean wasSeen(int x, int y, int z) {
		final BuiltRenderRegion r = getRegionIfExists(x, y, z);
		return r != null && r.wasRecentlySeen();
	}
}
