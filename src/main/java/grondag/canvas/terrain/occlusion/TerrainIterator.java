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

package grondag.canvas.terrain.occlusion;

import static grondag.canvas.shader.data.ShadowMatrixData.CASCADE_FLAG_0;
import static grondag.canvas.shader.data.ShadowMatrixData.CASCADE_FLAG_1;
import static grondag.canvas.shader.data.ShadowMatrixData.CASCADE_FLAG_2;
import static grondag.canvas.shader.data.ShadowMatrixData.CASCADE_FLAG_3;

import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.region.RegionBuildState;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.terrain.region.RenderRegionIndexer;
import grondag.canvas.terrain.region.RenderRegionStorage;
import grondag.canvas.terrain.util.TerrainExecutor.TerrainExecutorTask;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.Useful;

public class TerrainIterator implements TerrainExecutorTask {
	public static final int IDLE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int COMPLETE = 3;

	public final TerrainOccluder cameraOccluder = new TerrainOccluder();
	public final ShadowOccluder shadowOccluder = new ShadowOccluder();
	private final RegionBoundingSphere regionBoundingSphere = new RegionBoundingSphere();
	public final SimpleUnorderedArrayList<RenderRegion> updateRegions = new SimpleUnorderedArrayList<>();
	public final VisibleRegionList visibleRegions = new VisibleRegionList();
	public final VisibleRegionList[] shadowVisibleRegions = new VisibleRegionList[ShadowMatrixData.CASCADE_COUNT];
	private final AtomicInteger state = new AtomicInteger(IDLE);
	private final CanvasWorldRenderer cwr;

	private RenderRegion cameraRegion;
	private boolean includeCamera = false;
	private boolean includeShadow = false;

	/**
	 * Will be a valid region origin even if the actual camera region is null because
	 * camera is outside world range.  Otherwise will match the origin of the camera region.
	 */
	private long cameraChunkOrigin;
	private int renderDistance;
	private boolean chunkCullingEnabled = true;
	private volatile boolean cancelled = false;

	public TerrainIterator(CanvasWorldRenderer cwr) {
		this.cwr = cwr;

		for (int i = 0; i < ShadowMatrixData.CASCADE_COUNT; ++i) {
			shadowVisibleRegions[i] = new VisibleRegionList();
		}
	}

	public long cameraRegionOrigin() {
		return cameraChunkOrigin;
	}

	public void prepare(@Nullable RenderRegion cameraRegion, Camera camera, TerrainFrustum frustum, int renderDistance, boolean chunkCullingEnabled, int occlusionInputFlags) {
		assert state.get() == IDLE;
		this.cameraRegion = cameraRegion;
		includeCamera = (occlusionInputFlags & OcclusionInputManager.CAMERA_INVALID) == OcclusionInputManager.CAMERA_INVALID;

		// We always reiterate shadows if enabled because if terrain iteration ran
		// then shadows are impacted.  And if terrain iteration didn't run, the only
		// reason we are here is that shadows were not current.
		includeShadow = Pipeline.shadowsEnabled();

		final BlockPos cameraBlockPos = camera.getBlockPos();
		cameraChunkOrigin = RenderRegionIndexer.blockPosToRegionOrigin(cameraBlockPos);
		assert cameraRegion == null || cameraChunkOrigin == cameraRegion.origin.asLong();
		cameraOccluder.copyFrustum(frustum);
		regionBoundingSphere.update(renderDistance);

		if (Pipeline.shadowsEnabled()) {
			shadowOccluder.copyState(frustum);
		}

		this.renderDistance = renderDistance;
		this.chunkCullingEnabled = chunkCullingEnabled;

		state.set(READY);
		cancelled = false;
	}

	public int state() {
		return state.get();
	}

	public boolean includeCamera() {
		return includeCamera;
	}

	public boolean includeShadow() {
		return includeShadow;
	}

	public void reset() {
		cancelled = true;
		includeCamera = false;
		includeShadow = false;
		state.compareAndSet(COMPLETE, IDLE);
		visibleRegions.clear();
		clearShadowRegions();
		cameraOccluder.invalidate();
	}

	@Override
	public void run(TerrainRenderContext ignored) {
		assert state.get() == READY;
		state.set(RUNNING);
		cwr.potentiallyVisibleSetManager.update(cameraChunkOrigin);
		cwr.occlusionStateManager.beforeRegionUpdate();
		cwr.renderRegionStorage.updateRegionPositionAndVisibility();
		cwr.occlusionStateManager.afterRegionUpdate();
		final boolean redrawOccluder = cameraOccluder.prepareScene();
		final boolean redrawShadowOccluder = Pipeline.shadowsEnabled() ? shadowOccluder.prepareScene() : false;

		updateRegions.clear();

		if (includeCamera) {
			primeCameraRegions();
			iterateTerrain(redrawOccluder);
		}

		if (includeShadow) {
			iterateShadows(redrawShadowOccluder);
			//classifyVisibleShadowRegions();
		}

		if (cancelled) {
			state.set(IDLE);
			visibleRegions.clear();
			clearShadowRegions();
		} else {
			assert state.get() == RUNNING;
			state.set(COMPLETE);

			if (Configurator.debugOcclusionRaster) {
				cameraOccluder.outputRaster();

				if (Pipeline.shadowsEnabled()) {
					shadowOccluder.outputRaster();
				}
			}
		}
	}

	private void primeCameraRegions() {
		if (cameraRegion == null) {
			// prime visible when above or below world and camera region is null
			final RenderRegionStorage regionStorage = cwr.renderRegionStorage;
			// WIP: deal with variable world height
			final int y = BlockPos.unpackLongY(cameraChunkOrigin) > 0 ? 240 : 0;
			final int x = BlockPos.unpackLongX(cameraChunkOrigin);
			final int z = BlockPos.unpackLongZ(cameraChunkOrigin);
			final int limit = Useful.getLastDistanceSortedOffsetIndex(renderDistance);

			for (int i = 0; i < limit; ++i) {
				final Vec3i offset = Useful.getDistanceSortedCircularOffset(i);
				final RenderRegion region = regionStorage.getOrCreateRegion((offset.getX() << 4) + x, y, (offset.getZ() << 4) + z);

				if (region != null && region.origin.isPotentiallyVisibleFromCamera()) {
					region.occlusionState.addToCameraPvsIfValid();
				}
			}
		} else {
			cameraRegion.origin.forceCameraPotentialVisibility();
			cameraRegion.occlusionState.addToCameraPvsIfValid();
		}
	}

	private void iterateTerrain(boolean redrawOccluder) {
		final int occlusionResultVersion = cameraOccluder.occlusionVersion();
		final boolean chunkCullingEnabled = this.chunkCullingEnabled;
		final CameraPotentiallyVisibleRegionSet cameraDistanceSorter = cwr.potentiallyVisibleSetManager.cameraPVS;

		// PERF: look for ways to improve branch prediction
		while (!cancelled) {
			final RenderRegion builtRegion = cameraDistanceSorter.next();

			if (builtRegion == null) {
				break;
			}

			// don't visit if not in frustum and within render distance
			if (!builtRegion.origin.isPotentiallyVisibleFromCamera()) {
				continue;
			}

			// don't visit if region is outside near distance and doesn't have all 4 neighbors loaded
			if (!builtRegion.isNearOrHasLoadedNeighbors()) {
				continue;
			}

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			final RegionBuildState regionData = builtRegion.getBuildState();

			// If never built then don't do anything with it
			if (regionData == RegionBuildState.UNBUILT) {
				updateRegions.add(builtRegion);
				continue;
			}

			// If get to here has been built - if needs rebuilt we can use existing data this frame
			if (builtRegion.needsRebuild()) {
				updateRegions.add(builtRegion);
			}

			// for empty regions, check neighbors if visible but don't add to visible set
			if (!regionData.canOcclude()) {
				if (Configurator.cullEntityRender) {
					// reuse prior test results
					if (!builtRegion.occlusionState.isCameraOcclusionResultCurrent(occlusionResultVersion)) {
						if (!chunkCullingEnabled || builtRegion.origin.isNear() || cameraOccluder.isEmptyRegionVisible(builtRegion.origin)) {
							builtRegion.neighbors.enqueueUnvistedCameraNeighbors();
							builtRegion.occlusionState.setCameraOccluderResult(true, occlusionResultVersion);
						} else {
							builtRegion.occlusionState.setCameraOccluderResult(false, occlusionResultVersion);
						}
					}
				} else {
					builtRegion.neighbors.enqueueUnvistedCameraNeighbors();
					builtRegion.occlusionState.setCameraOccluderResult(false, occlusionResultVersion);
				}

				continue;
			}

			if (!chunkCullingEnabled || builtRegion.origin.isNear()) {
				builtRegion.neighbors.enqueueUnvistedCameraNeighbors();
				visibleRegions.add(builtRegion);

				if (redrawOccluder || !builtRegion.occlusionState.isCameraOcclusionResultCurrent(occlusionResultVersion)) {
					cameraOccluder.prepareRegion(builtRegion.origin);
					cameraOccluder.occlude(regionData.getOcclusionData());
				}

				builtRegion.occlusionState.setCameraOccluderResult(true, occlusionResultVersion);
			} else if (builtRegion.occlusionState.isCameraOcclusionResultCurrent(occlusionResultVersion)) {
				// reuse prior test results
				if (builtRegion.occlusionState.cameraOccluderResult()) {
					builtRegion.neighbors.enqueueUnvistedCameraNeighbors();
					visibleRegions.add(builtRegion);

					// will already have been drawn if occluder view version hasn't changed
					if (redrawOccluder) {
						cameraOccluder.prepareRegion(builtRegion.origin);
						cameraOccluder.occlude(regionData.getOcclusionData());
					}
				}
			} else {
				cameraOccluder.prepareRegion(builtRegion.origin);
				final int[] visData = regionData.getOcclusionData();

				if (cameraOccluder.isBoxVisible(visData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX])) {
					builtRegion.neighbors.enqueueUnvistedCameraNeighbors();
					visibleRegions.add(builtRegion);
					builtRegion.occlusionState.setCameraOccluderResult(true, occlusionResultVersion);

					// these must always be drawn - will be additive if view hasn't changed
					cameraOccluder.occlude(visData);
				} else {
					builtRegion.occlusionState.setCameraOccluderResult(false, occlusionResultVersion);
				}
			}
		}
	}

	private void iterateShadows(boolean redrawOccluder) {
		final int occlusionResultVersion = shadowOccluder.occlusionVersion();
		final RenderRegionStorage regionStorage = cwr.renderRegionStorage;
		final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPvs = cwr.potentiallyVisibleSetManager.shadowPVS;
		// prime visible when above or below world and camera region is null
		final int y = BlockPos.unpackLongY(cameraChunkOrigin);
		final int x = BlockPos.unpackLongX(cameraChunkOrigin);
		final int z = BlockPos.unpackLongZ(cameraChunkOrigin);
		final int limit = Useful.getLastDistanceSortedOffsetIndex(renderDistance);

		for (int i = 0; i < limit; ++i) {
			final Vec3i offset = Useful.getDistanceSortedCircularOffset(i);
			final RenderRegion region = regionStorage.getOrCreateRegion(
					(offset.getX() << 4) + x,
					// WIP: deal with variable world height
					MathHelper.clamp((regionBoundingSphere.getY(i) << 4) + y, 0, 240),
					(offset.getZ() << 4) + z);

			if (region != null && region.origin.isPotentiallyVisibleFromSkylight()) {
				region.occlusionState.addToShadowPvsIfValid();
			}
		}

		while (!cancelled) {
			final RenderRegion builtRegion = shadowPvs.next();

			if (builtRegion == null) {
				break;
			}

			// WIP: can remove this check?  Seems redundant of above
			// don't visit if not in shadow frustum and within render distance
			if (!builtRegion.origin.isPotentiallyVisibleFromSkylight()) {
				continue;
			}

			// don't visit if region is outside near distance and doesn't have all 4 neighbors loaded
			if (!builtRegion.isNearOrHasLoadedNeighbors()) {
				continue;
			}

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			final RegionBuildState regionData = builtRegion.getBuildState();

			// If never built then don't do anything with it
			if (regionData == RegionBuildState.UNBUILT) {
				updateRegions.add(builtRegion);
				continue;
			}

			// If get to here has been built - if needs rebuilt we can use existing data this frame
			if (builtRegion.needsRebuild()) {
				updateRegions.add(builtRegion);
			}

			// for empty regions, check neighbors if visible but don't add to visible set
			if (!regionData.canOcclude()) {
				//System.out.println("Empty region @ "  + builtRegion.origin().toShortString());
				// WIP: try to avoid re-running this for regions already in PVS - neighbors should be there already
				builtRegion.neighbors.enqueueUnvistedShadowNeighbors();
				builtRegion.occlusionState.setShadowOccluderResult(false, occlusionResultVersion);

				continue;
			}

			if (builtRegion.occlusionState.isShadowOcclusionResultCurrent(occlusionResultVersion)) {
				// reuse prior test results
				if (builtRegion.occlusionState.shadowOccluderResult()) {
					builtRegion.neighbors.enqueueUnvistedShadowNeighbors();
					addShadowRegion(builtRegion);

					// will already have beens drawn if occluder view version hasn't changed
					if (redrawOccluder) {
						shadowOccluder.prepareRegion(builtRegion.origin);
						shadowOccluder.occlude(regionData.getOcclusionData());
					}
				}
			} else {
				shadowOccluder.prepareRegion(builtRegion.origin);
				final int[] visData = regionData.getOcclusionData();

				if (shadowOccluder.isBoxVisible(visData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX])) {
					builtRegion.neighbors.enqueueUnvistedShadowNeighbors();
					addShadowRegion(builtRegion);
					builtRegion.occlusionState.setShadowOccluderResult(true, occlusionResultVersion);

					// these must always be drawn - will be additive if view hasn't changed
					shadowOccluder.occlude(visData);
				} else {
					builtRegion.occlusionState.setShadowOccluderResult(false, occlusionResultVersion);
				}
			}
		}
	}

	private void clearShadowRegions() {
		shadowVisibleRegions[0].clear();
		shadowVisibleRegions[1].clear();
		shadowVisibleRegions[2].clear();
		shadowVisibleRegions[3].clear();
	}

	private void classifyVisibleShadowRegions() {
		final VisibleRegionList visibleRegions = this.visibleRegions;
		final int limit = visibleRegions.size();

		for (int i = 0; i < limit; ++i) {
			addShadowRegion(visibleRegions.get(i));
		}
	}

	private void addShadowRegion(RenderRegion r) {
		final VisibleRegionList[] shadowVisibleRegions = this.shadowVisibleRegions;

		switch (r.origin.shadowCascadeFlags()) {
			case CASCADE_FLAG_0:
				shadowVisibleRegions[0].add(r);
				break;
			case CASCADE_FLAG_1:
				shadowVisibleRegions[1].add(r);
				break;
			case CASCADE_FLAG_1 | CASCADE_FLAG_0:
				shadowVisibleRegions[0].add(r);
				shadowVisibleRegions[1].add(r);
				break;
			case CASCADE_FLAG_2:
				shadowVisibleRegions[2].add(r);
				break;
			case CASCADE_FLAG_2 | CASCADE_FLAG_1:
				shadowVisibleRegions[1].add(r);
				shadowVisibleRegions[2].add(r);
				break;
			case CASCADE_FLAG_2 | CASCADE_FLAG_1 | CASCADE_FLAG_0:
				shadowVisibleRegions[0].add(r);
				shadowVisibleRegions[1].add(r);
				shadowVisibleRegions[2].add(r);
				break;
			case CASCADE_FLAG_3:
				shadowVisibleRegions[3].add(r);
				break;
			case CASCADE_FLAG_3 | CASCADE_FLAG_2:
				shadowVisibleRegions[2].add(r);
				shadowVisibleRegions[3].add(r);
				break;
			case CASCADE_FLAG_3 | CASCADE_FLAG_2 | CASCADE_FLAG_1:
				shadowVisibleRegions[1].add(r);
				shadowVisibleRegions[2].add(r);
				shadowVisibleRegions[3].add(r);
				break;
			case CASCADE_FLAG_3 | CASCADE_FLAG_2 | CASCADE_FLAG_1 | CASCADE_FLAG_0:
				shadowVisibleRegions[0].add(r);
				shadowVisibleRegions[1].add(r);
				shadowVisibleRegions[2].add(r);
				shadowVisibleRegions[3].add(r);
				break;
			case 0:
			default:
				// NOOP
		}
	}

	@Override
	public int priority() {
		return -1;
	}
}
