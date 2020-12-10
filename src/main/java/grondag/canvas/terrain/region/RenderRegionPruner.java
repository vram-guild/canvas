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

import net.minecraft.util.math.BlockPos;

import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.terrain.occlusion.PotentiallyVisibleRegionSorter;
import grondag.canvas.terrain.occlusion.TerrainOccluder;

public class RenderRegionPruner {
	private boolean invalidateOccluder = false;
	private int occluderVersion = 0;
	private long cameraChunkPos;
	private long lastCameraChunkPos = -1;

	private int maxSquaredChunkDistance;
	public final CanvasFrustum frustum;
	public final TerrainOccluder occluder;
	public final PotentiallyVisibleRegionSorter potentiallyVisibleRegions;

	public RenderRegionPruner(TerrainOccluder occluder, PotentiallyVisibleRegionSorter distanceSorter) {
		this.occluder = occluder;
		potentiallyVisibleRegions = distanceSorter;
		frustum = occluder.frustum;
	}

	public void prepare(final long cameraChunkOrigin) {
		invalidateOccluder = false;
		cameraChunkPos = BlockPos.asLong(BlockPos.unpackLongX(cameraChunkOrigin) >> 4, BlockPos.unpackLongY(cameraChunkOrigin) >> 4, BlockPos.unpackLongZ(cameraChunkOrigin) >> 4);

		if (lastCameraChunkPos != cameraChunkPos) {
			lastCameraChunkPos = cameraChunkPos;
			potentiallyVisibleRegions.clear();
		} else {
			potentiallyVisibleRegions.returnToStart();
		}

		occluderVersion = occluder.version();
		maxSquaredChunkDistance = occluder.maxSquaredChunkDistance();
	}

	public int occluderVersion() {
		return occluderVersion;
	}

	public long cameraChunkPos() {
		return cameraChunkPos;
	}

	public boolean didInvalidateOccluder() {
		return invalidateOccluder;
	}

	public void invalidateOccluder() {
		invalidateOccluder = true;
	}

	public int maxSquaredChunkDistance() {
		return maxSquaredChunkDistance;
	}
}
