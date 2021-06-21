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

import grondag.bitraster.PackedBox;

public class RegionPosition extends BlockPos {
	private final RenderRegion owner;
	private final int chunkY;

	private int occlusionRange;
	private int squaredCameraChunkDistance;
	private boolean isNear;
	private boolean isInsideRenderDistance;

	public RegionPosition(long packedPos, RenderRegion owner) {
		super(unpackLongX(packedPos), unpackLongY(packedPos), unpackLongZ(packedPos));
		this.owner = owner;
		chunkY = getY() >> 4;
	}

	void computeDistanceChecks() {
		final int cy = owner.storage.cameraChunkY() - chunkY;
		squaredCameraChunkDistance = owner.renderRegionChunk.horizontalSquaredDistance + cy * cy;
		isInsideRenderDistance = squaredCameraChunkDistance <= owner.cwr.maxSquaredChunkRenderDistance();
		isNear = squaredCameraChunkDistance <= 3;
		occlusionRange = PackedBox.rangeFromSquareChunkDist(squaredCameraChunkDistance);
	}

	public void close() {
		isInsideRenderDistance = false;
		isNear = false;
	}

	public int squaredCameraChunkDistance() {
		return squaredCameraChunkDistance;
	}

	/**
	 * Our logic for this is a little different than vanilla, which checks for squared distance
	 * to chunk center from camera < 768.0.  Ours will always return true for all 26 chunks adjacent
	 * (including diagonal) to the achunk containing the camera.
	 *
	 * <p>This logic is in {@link #updateCameraDistanceAndVisibilityInfo(TerrainVisibilityState)}.
	 */
	public boolean isNear() {
		return isNear;
	}

	public boolean isInsideRenderDistance() {
		return isInsideRenderDistance;
	}

	public int occlusionRange() {
		return occlusionRange;
	}
}
