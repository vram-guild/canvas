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

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import io.vram.frex.api.model.util.FaceUtil;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.terrain.occlusion.geometry.OcclusionResult;

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
				nr.neighbors.notifyNeighborClosed(FaceUtil.oppositeFaceIndex(i), owner);
			}
		}
	}

	public void forEachAvailable(Consumer<RenderRegion> operation) {
		var region = getNeighbor(FaceUtil.EAST_INDEX);
		if (region != null) operation.accept(region);

		region = getNeighbor(FaceUtil.WEST_INDEX);
		if (region != null) operation.accept(region);

		region = getNeighbor(FaceUtil.NORTH_INDEX);
		if (region != null) operation.accept(region);

		region = getNeighbor(FaceUtil.SOUTH_INDEX);
		if (region != null) operation.accept(region);

		if (!isTop) {
			region = getNeighbor(FaceUtil.UP_INDEX);
			if (region != null) operation.accept(region);
		}

		if (!isBottom) {
			region = getNeighbor(FaceUtil.DOWN_INDEX);
			if (region != null) operation.accept(region);
		}
	}

	private @Nullable RenderRegion getNeighbor(int faceIndex) {
		RenderRegion region = neighbors[faceIndex];

		if (region == null || region.isClosed()) {
			// this check is now done in all callers
			//if ((faceIndex == FaceConstants.UP_INDEX && isTop) || (faceIndex == FaceConstants.DOWN_INDEX && isBottom)) {
			//	return null;
			//}

			final Direction face = FaceUtil.faceFromIndex(faceIndex);
			final BlockPos origin = owner.origin;
			region = owner.storage.getOrCreateRegion(origin.getX() + face.getStepX() * 16, origin.getY() + face.getStepY() * 16, origin.getZ() + face.getStepZ() * 16);
			neighbors[faceIndex] = region;

			if (region != null) {
				region.neighbors.attachOrConfirmVisitingNeighbor(FaceUtil.oppositeFaceIndex(faceIndex), owner);
			}
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

		if ((openFlags & FaceUtil.EAST_FLAG) != 0) {
			final var region = getNeighbor(FaceUtil.EAST_INDEX);
			if (region != null) region.enqueueAsUnvistedCameraNeighbor(FaceUtil.WEST_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceUtil.WEST_FLAG) != 0) {
			final var region = getNeighbor(FaceUtil.WEST_INDEX);
			if (region != null) region.enqueueAsUnvistedCameraNeighbor(FaceUtil.EAST_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceUtil.NORTH_FLAG) != 0) {
			final var region = getNeighbor(FaceUtil.NORTH_INDEX);
			if (region != null) region.enqueueAsUnvistedCameraNeighbor(FaceUtil.SOUTH_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceUtil.SOUTH_FLAG) != 0) {
			final var region = getNeighbor(FaceUtil.SOUTH_INDEX);
			if (region != null) region.enqueueAsUnvistedCameraNeighbor(FaceUtil.NORTH_FLAG, mySquaredDist);
		}

		if (!isTop && (openFlags & FaceUtil.UP_FLAG) != 0) {
			final var region = getNeighbor(FaceUtil.UP_INDEX);
			if (region != null) region.enqueueAsUnvistedCameraNeighbor(FaceUtil.DOWN_FLAG, mySquaredDist);
		}

		if (!isBottom && (openFlags & FaceUtil.DOWN_FLAG) != 0) {
			final var region = getNeighbor(FaceUtil.DOWN_INDEX);
			if (region != null) region.enqueueAsUnvistedCameraNeighbor(FaceUtil.UP_FLAG, mySquaredDist);
		}
	}

	/** Used in advanced occlusion config. */
	public void enqueueUnvistedCameraNeighbors() {
		assert Pipeline.advancedTerrainCulling();

		final int mySquaredDist = owner.origin.squaredCameraChunkDistance();

		var region = getNeighbor(FaceUtil.EAST_INDEX);
		if (region != null && region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		region = getNeighbor(FaceUtil.WEST_INDEX);
		if (region != null && region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		region = getNeighbor(FaceUtil.NORTH_INDEX);
		if (region != null && region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		region = getNeighbor(FaceUtil.SOUTH_INDEX);
		if (region != null && region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		if (!isTop) {
			region = getNeighbor(FaceUtil.UP_INDEX);
			if (region != null && region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		}

		if (!isBottom) {
			region = getNeighbor(FaceUtil.DOWN_INDEX);
			if (region != null && region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		}
	}

	public void enqueueUnvistedShadowNeighbors() {
		var region = getNeighbor(FaceUtil.EAST_INDEX);
		if (region != null) region.shadowVisibility.addIfValid();

		region = getNeighbor(FaceUtil.WEST_INDEX);
		if (region != null) region.shadowVisibility.addIfValid();

		region = getNeighbor(FaceUtil.NORTH_INDEX);
		if (region != null) region.shadowVisibility.addIfValid();

		region = getNeighbor(FaceUtil.SOUTH_INDEX);
		if (region != null) region.shadowVisibility.addIfValid();

		if (!isTop) {
			region = getNeighbor(FaceUtil.UP_INDEX);
			if (region != null) region.shadowVisibility.addIfValid();
		}

		if (!isBottom) {
			region = getNeighbor(FaceUtil.DOWN_INDEX);
			if (region != null) region.shadowVisibility.addIfValid();
		}
	}
}
