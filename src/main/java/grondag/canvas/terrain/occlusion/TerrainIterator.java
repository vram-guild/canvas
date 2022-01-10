/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.terrain.occlusion;

import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import io.vram.dtk.CircleUtil;
import io.vram.frex.api.config.FlawlessFrames;
import io.vram.frex.api.model.util.FaceUtil;
import io.vram.sc.unordered.SimpleUnorderedArrayList;

import grondag.bitraster.PackedBox;
import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.CanvasTerrainRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.render.world.WorldRenderState;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.terrain.occlusion.camera.CameraRegionVisibility;
import grondag.canvas.terrain.occlusion.camera.CameraVisibility;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.occlusion.shadow.RegionBoundingSphere;
import grondag.canvas.terrain.occlusion.shadow.ShadowRegionVisibility;
import grondag.canvas.terrain.occlusion.shadow.ShadowVisibility;
import grondag.canvas.terrain.region.RegionBuildState;
import grondag.canvas.terrain.region.RenderRegion;
import grondag.canvas.terrain.region.RenderRegionIndexer;
import grondag.canvas.terrain.region.RenderRegionStorage;
import grondag.canvas.terrain.util.TerrainExecutorTask;

public class TerrainIterator implements TerrainExecutorTask {
	public static final int IDLE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int COMPLETE = 3;

	public final ShadowVisibility shadowVisibility;
	public final CameraVisibility cameraVisibility;
	private final RegionBoundingSphere regionBoundingSphere = new RegionBoundingSphere();
	public final SimpleUnorderedArrayList<RenderRegion> updateRegions = new SimpleUnorderedArrayList<>();
	public final VisibleRegionList visibleRegions = new VisibleRegionList();
	public final VisibleRegionList[] shadowVisibleRegions = new VisibleRegionList[ShadowMatrixData.CASCADE_COUNT];
	private final AtomicInteger state = new AtomicInteger(IDLE);
	private final WorldRenderState worldRenderState;

	private RenderRegion cameraRegion;

	/**
	 * Will be a valid region origin even if the actual camera region is null because
	 * camera is outside world range.  Otherwise will match the origin of the camera region.
	 */
	private long cameraChunkOrigin;
	private int renderDistance;
	private boolean chunkCullingEnabled = true;
	private volatile boolean cancelled = false;
	private boolean resetCameraOccluder;
	private boolean resetShadowOccluder;

	public TerrainIterator(WorldRenderState worldRenderState) {
		this.worldRenderState = worldRenderState;
		shadowVisibility = new ShadowVisibility(worldRenderState);
		cameraVisibility = new CameraVisibility(worldRenderState, shadowVisibility.targetOccluder);

		for (int i = 0; i < ShadowMatrixData.CASCADE_COUNT; ++i) {
			shadowVisibleRegions[i] = new VisibleRegionList();
		}
	}

	public boolean hasWork() {
		return cameraVisibility.isInvalid() || (worldRenderState.shadowsEnabled() && shadowVisibility.isInvalid());
	}

	public long cameraRegionOrigin() {
		return cameraChunkOrigin;
	}

	public void updateViewDependencies(Camera camera, TerrainFrustum frustum, int renderDistance) {
		final BlockPos cameraBlockPos = camera.getBlockPosition();
		worldRenderState.sectorManager.setCamera(camera.getPosition(), cameraBlockPos);
		cameraChunkOrigin = RenderRegionIndexer.blockPosToRegionOrigin(cameraBlockPos);
		regionBoundingSphere.update(renderDistance);
		this.renderDistance = renderDistance;
		cameraVisibility.updateView(frustum, cameraChunkOrigin);

		if (worldRenderState.shadowsEnabled()) {
			shadowVisibility.updateView(frustum, cameraChunkOrigin);
		}

		cameraRegion = worldRenderState.getWorld() == null || worldRenderState.getWorld().isOutsideBuildHeight(cameraBlockPos) ? null : worldRenderState.renderRegionStorage.getOrCreateRegion(cameraBlockPos);
		assert cameraRegion == null || cameraChunkOrigin == cameraRegion.origin.asLong();
	}

	public void buildNearIfNeeded() {
		Minecraft.getInstance().getProfiler().popPush("buildnear");

		if (cameraRegion != null) {
			worldRenderState.regionRebuildManager.buildNearRegionIfNeeded(cameraRegion);
			cameraRegion.neighbors.forEachAvailable(worldRenderState.regionRebuildManager::buildNearRegionIfNeeded);
		}
	}

	public boolean prepare(Camera camera, TerrainFrustum frustum, int renderDistance, boolean chunkCullingEnabled) {
		assert state.get() == IDLE;

		updateViewDependencies(camera, frustum, renderDistance);
		buildNearIfNeeded();
		this.chunkCullingEnabled = chunkCullingEnabled;
		cancelled = false;
		resetCameraOccluder = cameraVisibility.prepareForIteration();

		if (worldRenderState.shadowsEnabled()) {
			if (resetCameraOccluder) {
				shadowVisibility.invalidate();

				// Target occluder should be reset when camera resets
				// But not necessrily if shadow occluder resets.
				// It's content isn't driven by the shadow occluder.
				shadowVisibility.targetOccluder.invalidate();
			}

			if (shadowVisibility.targetOccluder.prepareScene() && !resetCameraOccluder) {
				// If the target occluder reset for some reason other than
				// camera occluder reset, then it will be missing all the visible
				// terrain regions and we need to redraw them.
				final int limit = visibleRegions.size();

				for (int i = 0; i < limit; ++i) {
					final RenderRegion r = visibleRegions.get(i);

					if (r.isClosed()) continue;

					final RegionBuildState buildState = r.getBuildState();

					if (buildState.canOcclude()) {
						shadowVisibility.targetOccluder.prepareRegion(r.origin);
						shadowVisibility.targetOccluder.occludeBox(buildState.getOcclusionResult().occlusionData()[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX]);
					}
				}
			}

			resetShadowOccluder = shadowVisibility.prepareForIteration();

			// shadow occluder should always reset if camera was
			assert !resetCameraOccluder || resetShadowOccluder;
		} else {
			resetShadowOccluder = false;
		}

		final boolean result = resetCameraOccluder || resetShadowOccluder;

		if (result) {
			state.set(READY);
		}

		return result;
	}

	public int state() {
		return state.get();
	}

	public void reset() {
		cancelled = true;
		state.set(IDLE);
		cameraVisibility.invalidate();
		shadowVisibility.invalidate();
		visibleRegions.clear();
		clearShadowRegions();
	}

	public void idle() {
		if (!state.compareAndSet(COMPLETE, IDLE)) {
			assert false : "Iterator in non-complete state on idle";
		}

		cancelled = true;
	}

	@Override
	public void run(CanvasTerrainRenderContext ignored) {
		assert state.get() == READY;
		state.set(RUNNING);

		try {
			worldRenderState.renderRegionStorage.updateRegionPositionAndVisibility();
			worldRenderState.drawListCullingHlper.update();

			if (resetCameraOccluder) {
				visibleRegions.clear();
				primeCameraRegions();
			}

			updateRegions.clear();

			if (Pipeline.advancedTerrainCulling() || FlawlessFrames.isActive()) {
				iterateTerrain();
			} else {
				iterateTerrainSimply();
			}

			if (worldRenderState.shadowsEnabled()) {
				if (resetShadowOccluder) {
					clearShadowRegions();
					primeShadowRegions();
				}

				iterateShadows();
			}

			if (cancelled) {
				state.set(IDLE);
			} else {
				assert state.get() == RUNNING;
				state.set(COMPLETE);

				if (Configurator.debugOcclusionRaster) {
					cameraVisibility.outputRaster();

					if (worldRenderState.shadowsEnabled()) {
						shadowVisibility.outputRaster();
					}
				}
			}
		} catch (final Exception e) {
			// If we have an error, is important that we set status back to IDLE so that we have a chance to restart.
			CanvasMod.LOG.warn("Unhandled exception in terrain iteration. This is probably a bug and will cause incorrect world renderering.", e);
			cancelled = true;
			state.set(IDLE);
		}
	}

	private void primeCameraRegions() {
		if (cameraRegion == null) {
			// prime visible when above or below world and camera region is null
			final RenderRegionStorage regionStorage = worldRenderState.renderRegionStorage;
			final boolean above = BlockPos.getY(cameraChunkOrigin) > 0;
			final int y = above ? (worldRenderState.getWorld().getMaxBuildHeight() - 1) & 0xFFFFFFF0 : worldRenderState.getWorld().getMinBuildHeight() & 0xFFFFFFF0;
			final int x = BlockPos.getX(cameraChunkOrigin);
			final int z = BlockPos.getZ(cameraChunkOrigin);
			final int limit = CircleUtil.getLastDistanceSortedOffsetIndex(renderDistance);
			final int entryFace = above ? FaceUtil.UP_FLAG : FaceUtil.DOWN_FLAG;

			for (int i = 0; i < limit; ++i) {
				final var offset = CircleUtil.getDistanceSortedCircularOffset(i);
				final RenderRegion region = regionStorage.getOrCreateRegion((offset.x() << 4) + x, y, (offset.y() << 4) + z);

				if (region != null) {
					if (Pipeline.advancedTerrainCulling()) {
						region.cameraVisibility.addIfValid();
					} else {
						region.cameraVisibility.addIfValid(entryFace);
					}
				}
			}
		} else {
			cameraRegion.origin.forceCameraPotentialVisibility();

			if (Pipeline.advancedTerrainCulling()) {
				cameraRegion.cameraVisibility.addIfValid();
			} else {
				cameraRegion.cameraVisibility.addIfValid(FaceUtil.ALL_REAL_FACE_FLAGS);
			}
		}
	}

	private void iterateTerrain() {
		final boolean chunkCullingEnabled = this.chunkCullingEnabled;
		final boolean flawless = FlawlessFrames.isActive();

		while (!cancelled) {
			final CameraRegionVisibility state = cameraVisibility.next();

			if (state == null) {
				break;
			}

			final RenderRegion region = state.region;
			assert region.origin.isPotentiallyVisibleFromCamera();
			assert region.isNearOrHasLoadedNeighbors();
			assert !region.isClosed();

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			RegionBuildState buildState = region.getBuildState();

			// If never built then don't do anything with it
			if (buildState == RegionBuildState.UNBUILT) {
				if (flawless) {
					assert RenderSystem.isOnRenderThread();
					region.rebuildOnMainThread();
					buildState = region.getBuildState();
				} else {
					updateRegions.add(region);
					continue;
				}
			}

			// If get to here has been built - if needs rebuilt we can use existing data this frame
			if (region.needsRebuild()) {
				if (flawless) {
					assert RenderSystem.isOnRenderThread();
					region.rebuildOnMainThread();
					buildState = region.getBuildState();
				} else {
					updateRegions.add(region);
				}
			}

			final OcclusionStatus priorResult = state.getOcclusionStatus();

			// Undetermined should not be in iteration because they have been visited.
			assert priorResult != OcclusionStatus.UNDETERMINED;

			if (priorResult != OcclusionStatus.VISITED) {
				// if prior test results still good just enqueue any neighbors we missed last
				// time if the result indicates we should.

				if (priorResult != OcclusionStatus.REGION_NOT_VISIBLE) {
					region.neighbors.enqueueUnvistedCameraNeighbors();
				}

				continue;
			}

			// If we get to here, we need to classify the region and possibly draw it to the rasterizer

			// For empty regions, check neighbors but don't add to visible set
			// We currently don't test these against rasterizer because there are many and it would be too expensive.
			if (!buildState.canOcclude()) {
				region.neighbors.enqueueUnvistedCameraNeighbors();
				state.setOcclusionStatus(OcclusionStatus.ENTITIES_VISIBLE);
				continue;
			}

			// If we get to here, region is not empty

			if (!chunkCullingEnabled || region.origin.isNear()) {
				// We are aren't culling, just add it.
				region.neighbors.enqueueUnvistedCameraNeighbors();
				visibleRegions.add(region);
				state.setOcclusionStatus(OcclusionStatus.REGION_VISIBLE);
				cameraVisibility.prepareRegion(region.origin);
				cameraVisibility.occlude(buildState.getOcclusionResult().occlusionData());
			} else {
				cameraVisibility.prepareRegion(region.origin);
				final int[] occlusionData = buildState.getOcclusionResult().occlusionData();

				if (cameraVisibility.isBoxVisible(occlusionData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX], region.origin.fuzz())) {
					// Renderable portion is visible
					// Continue search, mark visible, add to render list and draw to occluder
					region.neighbors.enqueueUnvistedCameraNeighbors();
					visibleRegions.add(region);
					state.setOcclusionStatus(OcclusionStatus.REGION_VISIBLE);
					cameraVisibility.occlude(occlusionData);
				} else {
					if (cameraVisibility.isBoxVisible(PackedBox.FULL_BOX, region.origin.fuzz())) {
						// need to progress through the region if part of it is visible
						// Like renderable, but we don't need to draw or add to render list
						region.neighbors.enqueueUnvistedCameraNeighbors();
						state.setOcclusionStatus(OcclusionStatus.ENTITIES_VISIBLE);
					} else {
						// no portion is visible
						state.setOcclusionStatus(OcclusionStatus.REGION_NOT_VISIBLE);
					}
				}
			}
		}
	}

	private void iterateTerrainSimply() {
		final boolean chunkCullingEnabled = this.chunkCullingEnabled;

		while (!cancelled) {
			final CameraRegionVisibility state = cameraVisibility.next();

			if (state == null) {
				break;
			}

			final RenderRegion region = state.region;
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

			final OcclusionStatus priorResult = state.getOcclusionStatus();

			// Undetermined should not be in iteration because they have been visited.
			assert priorResult != OcclusionStatus.UNDETERMINED;

			if (priorResult != OcclusionStatus.VISITED) {
				// if prior test results still good just enqueue any neighbors we missed last
				// time if the result indicates we should.

				if (priorResult != OcclusionStatus.REGION_NOT_VISIBLE) {
					region.neighbors.enqueueUnvistedCameraNeighbors(-1L);
				}

				continue;
			}

			// If we get to here, we need to classify the region and possibly draw it to the rasterizer

			// For empty regions, check neighbors but don't add to visible set
			// We currently don't test these against rasterizer because there are many and it would be too expensive.
			if (!buildState.canOcclude()) {
				region.neighbors.enqueueUnvistedCameraNeighbors(-1L);
				state.setOcclusionStatus(OcclusionStatus.ENTITIES_VISIBLE);
				continue;
			}

			// If we get to here, region is not empty
			region.neighbors.enqueueUnvistedCameraNeighbors(chunkCullingEnabled ? buildState.getOcclusionResult().mutalFaceMask() : -1L);
			visibleRegions.add(region);
			state.setOcclusionStatus(OcclusionStatus.REGION_VISIBLE);
		}
	}

	private void primeShadowRegions() {
		final RenderRegionStorage regionStorage = worldRenderState.renderRegionStorage;
		final int y = BlockPos.getY(cameraChunkOrigin);
		final int x = BlockPos.getX(cameraChunkOrigin);
		final int z = BlockPos.getZ(cameraChunkOrigin);
		final int limit = CircleUtil.getLastDistanceSortedOffsetIndex(renderDistance);
		final int yMin = worldRenderState.getWorld().getMinBuildHeight() & 0xFFFFFFF0;
		final int yMax = (worldRenderState.getWorld().getMaxBuildHeight() - 1) & 0xFFFFFFF0;

		for (int i = 0; i < limit; ++i) {
			final int ySphere = regionBoundingSphere.getY(i);

			// values < 0 indicate regions not within render distance
			if (ySphere < 0) {
				continue;
			}

			final var offset = CircleUtil.getDistanceSortedCircularOffset(i);

			final RenderRegion region = regionStorage.getOrCreateRegion(
					(offset.x() << 4) + x,
					Mth.clamp((ySphere << 4) + y, yMin, yMax),
					(offset.y() << 4) + z);

			if (region != null) {
				region.shadowVisibility.addIfValid();
			}
		}
	}

	private void iterateShadows() {
		final boolean flawless = FlawlessFrames.isActive();

		while (!cancelled) {
			final ShadowRegionVisibility state = shadowVisibility.next();

			if (state == null) {
				break;
			}

			final RenderRegion region = state.region;
			assert region.origin.isPotentiallyVisibleFromSkylight();
			assert region.renderChunk.areCornersLoaded();
			assert !region.isClosed();

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			RegionBuildState buildState = region.getBuildState();

			// If never built then don't do anything with it
			if (buildState == RegionBuildState.UNBUILT) {
				if (flawless) {
					assert RenderSystem.isOnRenderThread();
					region.rebuildOnMainThread();
					buildState = region.getBuildState();
				} else {
					updateRegions.add(region);
					continue;
				}
			}

			// If get to here has been built - if needs rebuilt we can use existing data this frame
			if (region.needsRebuild()) {
				if (flawless) {
					assert RenderSystem.isOnRenderThread();
					region.rebuildOnMainThread();
					buildState = region.getBuildState();
				} else {
					updateRegions.add(region);
				}
			}

			final OcclusionStatus priorResult = state.getOcclusionStatus();

			// Undetermined should not be in iteration because they have been visited.
			assert priorResult != OcclusionStatus.UNDETERMINED;

			if (priorResult != OcclusionStatus.VISITED) {
				// if prior test results still good just enqueue any neighbors we missed last
				// time if the result indicates we should.

				// if (region.occlusionState.cameraOccluderResult() != OcclusionResult.REGION_NOT_VISIBLE) { //
				if (priorResult != OcclusionStatus.REGION_NOT_VISIBLE) {
					region.neighbors.enqueueUnvistedShadowNeighbors();
				}

				continue;
			}

			// If we get to here, we need to classify the region and possibly draw it to the rasterizer

			// for empty regions, check neighbors if visible but don't add to visible set
			if (!buildState.canOcclude()) {
				// only check neighbors if the region is potentially visible
				if (shadowVisibility.isEmptyRegionVisible(region.origin, 0)) {
					state.setOcclusionStatus(OcclusionStatus.ENTITIES_VISIBLE);
					region.neighbors.enqueueUnvistedShadowNeighbors();
				} else {
					state.setOcclusionStatus(OcclusionStatus.REGION_NOT_VISIBLE);
				}

				continue;
			}

			// If we get to here, region is not empty

			shadowVisibility.prepareRegion(region.origin);
			final int[] occlusionData = buildState.getOcclusionResult().occlusionData();

			if (shadowVisibility.isBoxVisible(occlusionData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX], 0)) {
				region.neighbors.enqueueUnvistedShadowNeighbors();
				addShadowRegion(region);
				state.setOcclusionStatus(OcclusionStatus.REGION_VISIBLE);
				shadowVisibility.occlude(occlusionData);
			} else {
				if (shadowVisibility.isBoxVisible(PackedBox.FULL_BOX, 0)) {
					region.neighbors.enqueueUnvistedShadowNeighbors();
					state.setOcclusionStatus(OcclusionStatus.ENTITIES_VISIBLE);
				} else {
					state.setOcclusionStatus(OcclusionStatus.REGION_NOT_VISIBLE);
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
