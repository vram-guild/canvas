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

import java.util.function.Consumer;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.util.FaceConstants;
import grondag.canvas.varia.BlockPosHelper;

/** Caches directly adjacent regions for fast access and provides visitor operations for terrain iteration. */
public class NeighborRegions {
	private final RenderRegion owner;
	private final boolean isBottom;
	private final boolean isTop;
	private final RenderRegion[] neighbors = new RenderRegion[6];

	NeighborRegions(RenderRegion owner) {
		this.owner = owner;
		BlockPos origin = owner.origin();
		ClientWorld world = owner.cwr.getWorld();
		isBottom = origin.getY() == world.getBottomY();
		isTop = origin.getY() == world.getTopY() - 16;
	}

	void close() {
		for (int i = 0; i < 6; ++i) {
			final RenderRegion nr = neighbors[i];

			if (nr != null) {
				nr.neighbors.notifyNeighborClosed(BlockPosHelper.oppositeFaceIndex(i), owner);
			}
		}
	}

	public void forEachAvailable(Consumer<RenderRegion> operation) {
		operation.accept(getNeighbor(FaceConstants.EAST_INDEX));
		operation.accept(getNeighbor(FaceConstants.WEST_INDEX));
		operation.accept(getNeighbor(FaceConstants.NORTH_INDEX));
		operation.accept(getNeighbor(FaceConstants.SOUTH_INDEX));

		if (!isTop) {
			operation.accept(getNeighbor(FaceConstants.UP_INDEX));
		}

		if (!isBottom) {
			operation.accept(getNeighbor(FaceConstants.DOWN_INDEX));
		}
	}

	private RenderRegion getNeighbor(int faceIndex) {
		RenderRegion region = neighbors[faceIndex];

		if (region == null || region.isClosed) {
			// this check is now done in all callers
			//if ((faceIndex == FaceConstants.UP_INDEX && isTop) || (faceIndex == FaceConstants.DOWN_INDEX && isBottom)) {
			//	return null;
			//}

			final Direction face = ModelHelper.faceFromIndex(faceIndex);
			BlockPos origin = owner.origin;
			region = owner.storage.getOrCreateRegion(origin.getX() + face.getOffsetX() * 16, origin.getY() + face.getOffsetY() * 16, origin.getZ() + face.getOffsetZ() * 16);
			neighbors[faceIndex] = region;
			region.neighbors.attachOrConfirmVisitingNeighbor(BlockPosHelper.oppositeFaceIndex(faceIndex), owner);
		}

		return region;
	}

	private void attachOrConfirmVisitingNeighbor(int visitingFaceIndex, RenderRegion visitingNeighbor) {
		assert neighbors[visitingFaceIndex] == null || neighbors[visitingFaceIndex] == visitingNeighbor
			: "Visting render region is attaching to a position that already has a non-null region";

		neighbors[visitingFaceIndex] = visitingNeighbor;
	}

	private void notifyNeighborClosed(int closedFaceIndex, RenderRegion closingNeighbor) {
		assert neighbors[closedFaceIndex] == closingNeighbor
			: "Closing neighbor render region does not match current attachment";

		neighbors[closedFaceIndex] = null;
	}

	public void enqueueUnvistedCameraNeighbors() {
		getNeighbor(FaceConstants.EAST_INDEX).occlusionState.addToCameraPvsIfValid();
		getNeighbor(FaceConstants.WEST_INDEX).occlusionState.addToCameraPvsIfValid();
		getNeighbor(FaceConstants.NORTH_INDEX).occlusionState.addToCameraPvsIfValid();
		getNeighbor(FaceConstants.SOUTH_INDEX).occlusionState.addToCameraPvsIfValid();

		if (!isTop) {
			getNeighbor(FaceConstants.UP_INDEX).occlusionState.addToCameraPvsIfValid();
		}

		if (!isBottom) {
			getNeighbor(FaceConstants.DOWN_INDEX).occlusionState.addToCameraPvsIfValid();
		}
	}

	public void enqueueUnvistedShadowNeighbors() {
		getNeighbor(FaceConstants.EAST_INDEX).occlusionState.addToShadowPvsIfValid();
		getNeighbor(FaceConstants.WEST_INDEX).occlusionState.addToShadowPvsIfValid();
		getNeighbor(FaceConstants.NORTH_INDEX).occlusionState.addToShadowPvsIfValid();
		getNeighbor(FaceConstants.SOUTH_INDEX).occlusionState.addToShadowPvsIfValid();

		if (!isTop) {
			getNeighbor(FaceConstants.UP_INDEX).occlusionState.addToShadowPvsIfValid();
		}

		if (!isBottom) {
			getNeighbor(FaceConstants.DOWN_INDEX).occlusionState.addToShadowPvsIfValid();
		}
	}
}
