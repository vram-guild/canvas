/*
 * Copyright © Contributing Authors
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

import java.util.function.Consumer;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import io.vram.frex.api.model.ModelHelper;

import grondag.canvas.apiimpl.util.FaceConstants;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.terrain.occlusion.geometry.OcclusionResult;
import grondag.canvas.varia.BlockPosHelper;

/** Caches directly adjacent regions for fast access and provides visitor operations for terrain iteration. */
public class NeighborRegions {
	private final RenderRegion owner;
	private final boolean isBottom;
	private final boolean isTop;
	private final RenderRegion[] neighbors = new RenderRegion[6];

	NeighborRegions(RenderRegion owner) {
		this.owner = owner;
		final BlockPos origin = owner.origin;
		final ClientLevel world = owner.worldRenderState.getWorld();
		isBottom = origin.getY() == world.getMinBuildHeight();
		isTop = origin.getY() == world.getMaxBuildHeight() - 16;
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

		if (region == null || region.isClosed()) {
			// this check is now done in all callers
			//if ((faceIndex == FaceConstants.UP_INDEX && isTop) || (faceIndex == FaceConstants.DOWN_INDEX && isBottom)) {
			//	return null;
			//}

			final Direction face = ModelHelper.faceFromIndex(faceIndex);
			final BlockPos origin = owner.origin;
			region = owner.storage.getOrCreateRegion(origin.getX() + face.getStepX() * 16, origin.getY() + face.getStepY() * 16, origin.getZ() + face.getStepZ() * 16);
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

	/** Used in simple occlusion config. */
	public void enqueueUnvistedCameraNeighbors(final long mutalOcclusionFaceFlags) {
		assert !Pipeline.advancedTerrainCulling();

		final int mySquaredDist = owner.origin.squaredCameraChunkDistance();
		final int openFlags = OcclusionResult.openFacesFlag(mutalOcclusionFaceFlags, owner.cameraVisibility.entryFaceFlags());

		if ((openFlags & FaceConstants.EAST_FLAG) != 0) {
			getNeighbor(FaceConstants.EAST_INDEX).enqueueAsUnvistedCameraNeighbor(FaceConstants.WEST_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceConstants.WEST_FLAG) != 0) {
			getNeighbor(FaceConstants.WEST_INDEX).enqueueAsUnvistedCameraNeighbor(FaceConstants.EAST_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceConstants.NORTH_FLAG) != 0) {
			getNeighbor(FaceConstants.NORTH_INDEX).enqueueAsUnvistedCameraNeighbor(FaceConstants.SOUTH_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceConstants.SOUTH_FLAG) != 0) {
			getNeighbor(FaceConstants.SOUTH_INDEX).enqueueAsUnvistedCameraNeighbor(FaceConstants.NORTH_FLAG, mySquaredDist);
		}

		if (!isTop && (openFlags & FaceConstants.UP_FLAG) != 0) {
			getNeighbor(FaceConstants.UP_INDEX).enqueueAsUnvistedCameraNeighbor(FaceConstants.DOWN_FLAG, mySquaredDist);
		}

		if (!isBottom && (openFlags & FaceConstants.DOWN_FLAG) != 0) {
			getNeighbor(FaceConstants.DOWN_INDEX).enqueueAsUnvistedCameraNeighbor(FaceConstants.UP_FLAG, mySquaredDist);
		}
	}

	/** Used in advanced occlusion config. */
	public void enqueueUnvistedCameraNeighbors() {
		assert Pipeline.advancedTerrainCulling();

		final int mySquaredDist = owner.origin.squaredCameraChunkDistance();

		var region = getNeighbor(FaceConstants.EAST_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		region = getNeighbor(FaceConstants.WEST_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		region = getNeighbor(FaceConstants.NORTH_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		region = getNeighbor(FaceConstants.SOUTH_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		if (!isTop) {
			region = getNeighbor(FaceConstants.UP_INDEX);
			if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		}

		if (!isBottom) {
			region = getNeighbor(FaceConstants.DOWN_INDEX);
			if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		}
	}

	public void enqueueUnvistedShadowNeighbors() {
		getNeighbor(FaceConstants.EAST_INDEX).shadowVisibility.addIfValid();
		getNeighbor(FaceConstants.WEST_INDEX).shadowVisibility.addIfValid();
		getNeighbor(FaceConstants.NORTH_INDEX).shadowVisibility.addIfValid();
		getNeighbor(FaceConstants.SOUTH_INDEX).shadowVisibility.addIfValid();

		if (!isTop) {
			getNeighbor(FaceConstants.UP_INDEX).shadowVisibility.addIfValid();
		}

		if (!isBottom) {
			getNeighbor(FaceConstants.DOWN_INDEX).shadowVisibility.addIfValid();
		}
	}
}
