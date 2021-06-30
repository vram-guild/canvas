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

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import grondag.bitraster.PackedBox;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.data.ShaderDataManager;
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
	public final ShadowOccluder shadowOccluder = new ShadowOccluder("canvas_shadow_occlusion_raster.png");
	public final ShadowOccluder targetOccluder = new ShadowOccluder("canvas_shadow_target_occlusion_raster.png");
	private final RegionBoundingSphere regionBoundingSphere = new RegionBoundingSphere();
	public final SimpleUnorderedArrayList<RenderRegion> updateRegions = new SimpleUnorderedArrayList<>();
	public final VisibleRegionList visibleRegions = new VisibleRegionList();
	public final VisibleRegionList[] shadowVisibleRegions = new VisibleRegionList[ShadowMatrixData.CASCADE_COUNT];
	private final AtomicInteger state = new AtomicInteger(IDLE);
	private final WorldRenderState worldRenderState;

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

	public TerrainIterator(WorldRenderState worldRenderState) {
		this.worldRenderState = worldRenderState;

		for (int i = 0; i < ShadowMatrixData.CASCADE_COUNT; ++i) {
			shadowVisibleRegions[i] = new VisibleRegionList();
		}
	}

	public long cameraRegionOrigin() {
		return cameraChunkOrigin;
	}

	public void updateViewDependencies(Camera camera, TerrainFrustum frustum, int renderDistance) {
		final BlockPos cameraBlockPos = camera.getBlockPos();
		cameraChunkOrigin = RenderRegionIndexer.blockPosToRegionOrigin(cameraBlockPos);
		cameraOccluder.copyFrustum(frustum);
		regionBoundingSphere.update(renderDistance);
		worldRenderState.potentiallyVisibleSetManager.update(cameraChunkOrigin);

		if (includeShadow) {
			shadowOccluder.copyState(frustum);
			shadowOccluder.setLightVector(ShaderDataManager.skyLightVector);
			targetOccluder.copyState(frustum);
			targetOccluder.setLightVector(ShaderDataManager.skyLightVector);
		}

		this.renderDistance = renderDistance;

		cameraRegion = worldRenderState.getWorld() == null || worldRenderState.getWorld().isOutOfHeightLimit(cameraBlockPos) ? null : worldRenderState.renderRegionStorage.getOrCreateRegion(cameraBlockPos);
		assert cameraRegion == null || cameraChunkOrigin == cameraRegion.origin.asLong();
	}

	public void buildNearIfNeeded() {
		MinecraftClient.getInstance().getProfiler().swap("buildnear");

		if (cameraRegion != null) {
			worldRenderState.regionRebuildManager.buildNearRegionIfNeeded(cameraRegion);
			cameraRegion.neighbors.forEachAvailable(worldRenderState.regionRebuildManager::buildNearRegionIfNeeded);
		}
	}

	public void prepare(Camera camera, TerrainFrustum frustum, int renderDistance, boolean chunkCullingEnabled, int occlusionInputFlags) {
		assert state.get() == IDLE;

		includeCamera = (occlusionInputFlags & OcclusionInputManager.CAMERA_INVALID) == OcclusionInputManager.CAMERA_INVALID;

		// We always reiterate shadows if enabled because if terrain iteration ran
		// then shadows are impacted.  And if terrain iteration didn't run, the only
		// reason we are here is that shadows were not current.
		includeShadow = Pipeline.shadowsEnabled();

		updateViewDependencies(camera, frustum, renderDistance);

		buildNearIfNeeded();

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
		cameraOccluder.invalidate();
		targetOccluder.invalidate();
		shadowOccluder.invalidate();
		visibleRegions.clear();
		clearShadowRegions();
	}

	@Override
	public void run(TerrainRenderContext ignored) {
		assert state.get() == READY;
		state.set(RUNNING);
		worldRenderState.renderRegionStorage.updateRegionPositionAndVisibility();
		worldRenderState.occlusionStateManager.afterRegionUpdate();
		final boolean redrawOccluder = cameraOccluder.prepareScene();

		if (redrawOccluder) {
			includeCamera = true;

			if (includeShadow) {
				targetOccluder.invalidate();
			}
		}

		if (includeShadow) {
			targetOccluder.prepareScene();
		}

		updateRegions.clear();

		if (includeCamera) {
			if (redrawOccluder) {
				worldRenderState.potentiallyVisibleSetManager.cameraPVS.clear();
				primeCameraRegions();
			}

			iterateTerrain(redrawOccluder);
		}

		if (includeShadow) {
			final boolean redrawShadowOccluder = shadowOccluder.prepareScene();

			if (redrawShadowOccluder) {
				worldRenderState.potentiallyVisibleSetManager.shadowPVS.clear();
			}

			iterateShadows(redrawShadowOccluder);
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
					targetOccluder.outputRaster();
				}
			}
		}
	}

	private void primeCameraRegions() {
		if (cameraRegion == null) {
			// prime visible when above or below world and camera region is null
			final RenderRegionStorage regionStorage = worldRenderState.renderRegionStorage;
			final int y = BlockPos.unpackLongY(cameraChunkOrigin) > 0 ? (worldRenderState.getWorld().getTopY() - 1) & 0xFFFFFFF0 : worldRenderState.getWorld().getBottomY() & 0xFFFFFFF0;
			final int x = BlockPos.unpackLongX(cameraChunkOrigin);
			final int z = BlockPos.unpackLongZ(cameraChunkOrigin);
			final int limit = Useful.getLastDistanceSortedOffsetIndex(renderDistance);

			for (int i = 0; i < limit; ++i) {
				final Vec3i offset = Useful.getDistanceSortedCircularOffset(i);
				final RenderRegion region = regionStorage.getOrCreateRegion((offset.getX() << 4) + x, y, (offset.getZ() << 4) + z);

				if (region != null) {
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
		final CameraPotentiallyVisibleRegionSet cameraDistanceSorter = worldRenderState.potentiallyVisibleSetManager.cameraPVS;

		boolean isOccluderValid = true;

		while (!cancelled) {
			final RenderRegion region = cameraDistanceSorter.next();

			if (region == null) {
				break;
			}

			assert region.origin.isPotentiallyVisibleFromCamera();
			assert region.isNearOrHasLoadedNeighbors();
			assert !region.isClosed();

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			final RegionBuildState buildState = region.getBuildState();

			// If never built then don't do anything with it
			if (buildState == RegionBuildState.UNBUILT) {
				updateRegions.add(region);
				continue;
			}

			// If get to here has been built - if needs rebuilt we can use existing data this frame
			if (region.needsRebuild()) {
				updateRegions.add(region);
			}

			// for empty regions, check neighbors if visible but don't add to visible set
			if (!buildState.canOcclude()) {
				if (Configurator.cullEntityRender) {
					// reuse prior test results
					if (!region.occlusionState.isCameraOcclusionResultCurrent(occlusionResultVersion)) {
						if (!chunkCullingEnabled || region.origin.isNear() || cameraOccluder.isEmptyRegionVisible(region.origin)) {
							region.neighbors.enqueueUnvistedCameraNeighbors();
							region.occlusionState.setCameraOccluderResult(true, occlusionResultVersion);
						} else {
							region.occlusionState.setCameraOccluderResult(false, occlusionResultVersion);
						}
					}
				} else {
					region.neighbors.enqueueUnvistedCameraNeighbors();
					region.occlusionState.setCameraOccluderResult(false, occlusionResultVersion);
				}

				continue;
			}

			if (!chunkCullingEnabled || region.origin.isNear()) {
				region.neighbors.enqueueUnvistedCameraNeighbors();
				visibleRegions.add(region);

				if (redrawOccluder || !region.occlusionState.isCameraOcclusionResultCurrent(occlusionResultVersion)) {
					cameraOccluder.prepareRegion(region.origin);
					occludeTerrainRegion(region, buildState.getOcclusionData());
				}

				region.occlusionState.setCameraOccluderResult(true, occlusionResultVersion);
			} else if (!redrawOccluder && region.occlusionState.isCameraOcclusionResultCurrent(occlusionResultVersion)) {
				// reuse prior test results
				if (region.occlusionState.cameraOccluderResult()) {
					region.neighbors.enqueueUnvistedCameraNeighbors();
					visibleRegions.add(region);
				}
			} else {
				if (isOccluderValid && region.origin.squaredCameraChunkDistance() < cameraOccluder.maxSquaredChunkDistance()) {
					//System.out.println("invalidateCameraOcclusionResult due to backtrack from " + cameraOccluder.maxSquaredChunkDistance() + " to " + region.origin.squaredCameraChunkDistance() + " with origin " + region.origin.toShortString());
					worldRenderState.occlusionStateManager.invalidateCameraOcclusionResult();
					isOccluderValid = false;
				}

				cameraOccluder.prepareRegion(region.origin);
				final int[] occlusionData = buildState.getOcclusionData();

				if (cameraOccluder.isBoxVisible(occlusionData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX])) {
					region.neighbors.enqueueUnvistedCameraNeighbors();
					visibleRegions.add(region);
					region.occlusionState.setCameraOccluderResult(true, occlusionResultVersion);

					// these must always be drawn - will be additive if view hasn't changed
					occludeTerrainRegion(region, buildState.getOcclusionData());
					cameraOccluder.occlude(occlusionData);
				} else {
					// need to progress through the region if part of it is visible
					if (cameraOccluder.isBoxVisible(PackedBox.FULL_BOX)) {
						region.neighbors.enqueueUnvistedCameraNeighbors();
					}

					// FIX: this really won't work with entity culling
					region.occlusionState.setCameraOccluderResult(false, occlusionResultVersion);
				}
			}
		}
	}

	private void occludeTerrainRegion(RenderRegion region, int[] occlusionData) {
		cameraOccluder.occlude(occlusionData);

		if (Pipeline.shadowsEnabled()) {
			targetOccluder.prepareRegion(region.origin);
			targetOccluder.occludeBox(occlusionData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX]);
		}
	}

	private void iterateShadows(boolean redrawOccluder) {
		final int occlusionResultVersion = shadowOccluder.occlusionVersion();
		final RenderRegionStorage regionStorage = worldRenderState.renderRegionStorage;
		final ShadowPotentiallyVisibleRegionSet<RenderRegion> shadowPvs = worldRenderState.potentiallyVisibleSetManager.shadowPVS;
		// prime visible when above or below world and camera region is null
		final int y = BlockPos.unpackLongY(cameraChunkOrigin);
		final int x = BlockPos.unpackLongX(cameraChunkOrigin);
		final int z = BlockPos.unpackLongZ(cameraChunkOrigin);
		final int limit = Useful.getLastDistanceSortedOffsetIndex(renderDistance);
		final int yMin = worldRenderState.getWorld().getBottomY() & 0xFFFFFFF0;
		final int yMax = (worldRenderState.getWorld().getTopY() - 1) & 0xFFFFFFF0;

		for (int i = 0; i < limit; ++i) {
			final int ySphere = regionBoundingSphere.getY(i);

			// values < 0 indicate regions not within render distance
			if (ySphere < 0) {
				continue;
			}

			final Vec3i offset = Useful.getDistanceSortedCircularOffset(i);

			final RenderRegion region = regionStorage.getOrCreateRegion(
					(offset.getX() << 4) + x,
					MathHelper.clamp((ySphere << 4) + y, yMin, yMax),
					(offset.getZ() << 4) + z);

			if (region != null) {
				region.occlusionState.addToShadowPvsIfValid();
			}
		}

		while (!cancelled) {
			final RenderRegion region = shadowPvs.next();

			if (region == null) {
				break;
			}

			assert region.origin.isPotentiallyVisibleFromSkylight();
			assert region.renderChunk.areCornersLoaded();
			assert !region.isClosed();

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			final RegionBuildState buildState = region.getBuildState();

			// If never built then don't do anything with it
			if (buildState == RegionBuildState.UNBUILT) {
				updateRegions.add(region);
				continue;
			}

			// If get to here has been built - if needs rebuilt we can use existing data this frame
			if (region.needsRebuild()) {
				updateRegions.add(region);
			}

			targetOccluder.prepareRegion(region.origin);

			// for empty regions, check neighbors if visible but don't add to visible set
			if (!buildState.canOcclude()) {
				region.occlusionState.setShadowOccluderResult(false, occlusionResultVersion);

				// only check neighbors if the region is potentially visible
				if (targetOccluder.isBoxOccluded(PackedBox.FULL_BOX)) {
					region.neighbors.enqueueUnvistedShadowNeighbors();
				}

				continue;
			}

			final int[] occlusionData = buildState.getOcclusionData();

			// If can't shadow any terrain then no need to render
			if (!targetOccluder.isBoxOccluded(occlusionData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX])) {
				region.occlusionState.setShadowOccluderResult(false, occlusionResultVersion);

				// but if any part of region is potentially occluding then should check neighbors
				if (targetOccluder.isBoxOccluded(PackedBox.FULL_BOX)) {
					region.neighbors.enqueueUnvistedShadowNeighbors();
				}

				continue;
			}

			if (region.occlusionState.isShadowOcclusionResultCurrent(occlusionResultVersion)) {
				// reuse prior test results
				if (region.occlusionState.shadowOccluderResult()) {
					region.neighbors.enqueueUnvistedShadowNeighbors();
					addShadowRegion(region);

					// will already have beens drawn if occluder view version hasn't changed
					if (redrawOccluder) {
						shadowOccluder.prepareRegion(region.origin);
						shadowOccluder.occlude(occlusionData);
					}
				}
			} else {
				shadowOccluder.prepareRegion(region.origin);

				if (shadowOccluder.isBoxVisible(occlusionData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX])) {
					region.neighbors.enqueueUnvistedShadowNeighbors();
					addShadowRegion(region);
					region.occlusionState.setShadowOccluderResult(true, occlusionResultVersion);

					// these must always be drawn - will be additive if view hasn't changed
					shadowOccluder.occlude(occlusionData);
				} else {
					region.occlusionState.setShadowOccluderResult(false, occlusionResultVersion);
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

	private void addShadowRegion(RenderRegion r) {
		final VisibleRegionList[] shadowVisibleRegions = this.shadowVisibleRegions;

		switch (r.origin.shadowCascade()) {
			case 0:
				shadowVisibleRegions[0].add(r);
				break;
			case 1:
				shadowVisibleRegions[0].add(r);
				shadowVisibleRegions[1].add(r);
				break;
			case 2:
				shadowVisibleRegions[0].add(r);
				shadowVisibleRegions[1].add(r);
				shadowVisibleRegions[2].add(r);
				break;
			case 3:
				shadowVisibleRegions[0].add(r);
				shadowVisibleRegions[1].add(r);
				shadowVisibleRegions[2].add(r);
				shadowVisibleRegions[3].add(r);
				break;
			case -1:
			default:
				// NOOP
		}
	}

	@Override
	public int priority() {
		return -1;
	}
}
