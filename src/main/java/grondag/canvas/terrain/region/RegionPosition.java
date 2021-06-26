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
import net.minecraft.util.math.Vec3d;

import grondag.bitraster.PackedBox;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.terrain.occlusion.TerrainOccluder;

public class RegionPosition extends BlockPos {
	/** Region that holds this position as its origin. Provides access to world render state. */
	private final RenderRegion owner;

	/**
	 * The y coordinate of this position in chunks (16 blocks each), relative to world Y = 0. (Can be negative.)
	 * Sole purpose is to make camera chunk distance computation slightly faster.
	 */
	private final int chunkY;

	private long cameraRegionOrigin = -1;

	/** Tracks the version of the camera occluder view transform to know when we must recompute dependent values. */
	private int cameraOccluderViewVersion = -1;

	/**
	 * Tracks the version of the camera occluder position to know when we must recompute dependent values.
	 * The occlusion position is much more sensitive and changes more frequently than {@link #chunkDistVersion}.
	 *
	 * <p>Position cannot change without view also changing, so there is no need to check this unless
	 * {@link #cameraOccluderViewVersion} has changed.
	 */
	private int cameraOccluderPositionVersion = -1;

	/** See {@link #occlusionRange()}. */
	private int occlusionRange;

	/** See {@link #squaredCameraChunkDistance()}. */
	private int squaredCameraChunkDistance;

	/** See {@link #isNear()}. */
	private boolean isNear;

	/** See {@link #isInsideRenderDistance()}. */
	private boolean isInsideRenderDistance;

	/** Used by frustum tests. Will be current only if region is within render distance. */
	private float cameraRelativeCenterX;
	private float cameraRelativeCenterY;
	private float cameraRelativeCenterZ;

	private boolean isPotentiallyVisibleFromCamera;

	/** See {@link #checkAndUpdateSortNeeded(int)}. */
	private int sortPositionVersion = -1;

	/** Concatenated bit flags marking the shadow cascades that include this region. */
	private int shadowCascadeFlags;

	public RegionPosition(long packedPos, RenderRegion owner) {
		super(unpackLongX(packedPos), unpackLongY(packedPos), unpackLongZ(packedPos));
		this.owner = owner;
		chunkY = getY() >> 4;
	}

	public void update() {
		computeRegionDependentValues();
		computeViewDependentValues();
		shadowCascadeFlags = Pipeline.shadowsEnabled() ? owner.cwr.terrainIterator.shadowOccluder.cascadeFlags(this) : 0;
	}

	private void computeRegionDependentValues() {
		final long cameraRegionOrigin = owner.cwr.terrainIterator.cameraRegionOrigin();

		if (this.cameraRegionOrigin != cameraRegionOrigin) {
			this.cameraRegionOrigin = cameraRegionOrigin;
			final int cy = (BlockPos.unpackLongY(cameraRegionOrigin) >> 4) - chunkY;
			squaredCameraChunkDistance = owner.renderChunk.horizontalSquaredDistance + cy * cy;
			isInsideRenderDistance = squaredCameraChunkDistance <= owner.cwr.maxSquaredChunkRenderDistance();
			isNear = squaredCameraChunkDistance <= 3;
			occlusionRange = PackedBox.rangeFromSquareChunkDist(squaredCameraChunkDistance);
		}
	}

	private void computeViewDependentValues() {
		final TerrainOccluder cameraOccluder = owner.cameraOccluder;
		final int viewVersion = cameraOccluder.frustumViewVersion();

		if (viewVersion != cameraOccluderViewVersion) {
			cameraOccluderViewVersion = viewVersion;

			final int positionVersion = cameraOccluder.frustumPositionVersion();

			// These checks depend on the camera occluder position version,
			// which may not necessarily change when view version change.
			if (positionVersion != cameraOccluderPositionVersion) {
				cameraOccluderPositionVersion = positionVersion;

				// These are needed by frustum tests, which happen below, after this update.
				// not needed at all if outside of render distance
				if (isInsideRenderDistance()) {
					final Vec3d cameraPos = cameraOccluder.frustumCameraPos();
					final float dx = (float) (getX() + 8 - cameraPos.x);
					final float dy = (float) (getY() + 8 - cameraPos.y);
					final float dz = (float) (getZ() + 8 - cameraPos.z);
					cameraRelativeCenterX = dx;
					cameraRelativeCenterY = dy;
					cameraRelativeCenterZ = dz;
				}
			}

			//  PERF: implement hierarchical tests with propagation of per-plane inside test results
			isPotentiallyVisibleFromCamera = isInsideRenderDistance() && cameraOccluder.isRegionVisible(this);
		}
	}

	public void close() {
		isInsideRenderDistance = false;
		isNear = false;
		cameraOccluderPositionVersion = -1;
		cameraOccluderViewVersion = -1;
		cameraRegionOrigin = -1;
		isPotentiallyVisibleFromCamera = false;
	}

	/**
	 * Square of distance of this region from the camera region measured in chunks. (16, blocks each.)
	 */
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

	/**
	 * Means what the name suggests.  Note that retention distance is longer.
	 * Does not mean region is visible or within the view frustum.
	 */
	public boolean isInsideRenderDistance() {
		return isInsideRenderDistance;
	}

	/**
	 * True when region is within render distance and also within the camera frustum.
	 *
	 * <p>NB: tried a crude hierarchical scheme of checking chunk columns first
	 * but didn't pay off.  Would probably  need to propagate per-plane results
	 * over a more efficient region but that might not even help. Is already
	 * quite fast and typically only one or a few regions per chunk must be tested.
	 */
	public boolean isPotentiallyVisibleFromCamera() {
		return isPotentiallyVisibleFromCamera;
	}

	/**
	 * Called for camera region because frustum checks on near plane appear to be a little wobbly.
	 */
	public void forceCameraPotentialVisibility() {
		isPotentiallyVisibleFromCamera = true;
	}

	/**
	 * Classifies this region with one of the {@link PackedBox} constants for region ranges,
	 * based on distance from the camera. Used by the occluder to select level of detail used.
	 */
	public int occlusionRange() {
		return occlusionRange;
	}

	public float cameraRelativeCenterX() {
		return cameraRelativeCenterX;
	}

	public float cameraRelativeCenterY() {
		return cameraRelativeCenterY;
	}

	public float cameraRelativeCenterZ() {
		return cameraRelativeCenterZ;
	}

	/**
	 * Tracks the given sort counter and returns true when the input value was different.
	 * Used to identify regions that require a translucency resort.
	 * The sort version is incremented elsewhere based on camera movement.
	 *
	 * <p>Here because it is nominally related to position even if not related
	 * to other feature of this class. (It has to live somewhere.) Future optimizations
	 * might make more use of region-specific position information.
	 */
	public boolean checkAndUpdateSortNeeded(int sortPositionVersion) {
		if (this.sortPositionVersion == sortPositionVersion) {
			return false;
		} else {
			this.sortPositionVersion = sortPositionVersion;
			return true;
		}
	}

	/** For debugging. */
	public boolean sharesOriginWith(int blockX, int blockY, int blockZ) {
		return getX() >> 4 == blockX >> 4 && getY() >> 4 == blockY >> 4 && getZ() >> 4 == blockZ >> 4;
	}

	public int shadowCascadeFlags() {
		return shadowCascadeFlags;
	}

	public boolean isPotentiallyVisibleFromSkylight() {
		return owner.origin.isInsideRenderDistance() & shadowCascadeFlags != 0;
	}
}
