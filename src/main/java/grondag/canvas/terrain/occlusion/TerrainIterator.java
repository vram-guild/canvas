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

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;
import grondag.canvas.terrain.region.BuiltRenderRegion;
import grondag.canvas.terrain.region.RegionData;
import grondag.canvas.terrain.region.RenderRegionIndexer;
import grondag.canvas.terrain.region.RenderRegionStorage;
import grondag.canvas.terrain.util.TerrainExecutor.TerrainExecutorTask;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.Useful;

public class TerrainIterator implements TerrainExecutorTask {
	public static final boolean TRACE_OCCLUSION_OUTCOMES = Configurator.traceOcclusionOutcomes;

	public static final int IDLE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int COMPLETE = 3;

	public final TerrainOccluder cameraOccluder = new TerrainOccluder();
	public final SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions = new SimpleUnorderedArrayList<>();
	public final VisibleRegionList visibleRegions = new VisibleRegionList();
	private final AtomicInteger state = new AtomicInteger(IDLE);
	private final CanvasWorldRenderer cwr;

	private BuiltRenderRegion cameraRegion;
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
	}

	public void prepare(@Nullable BuiltRenderRegion cameraRegion, Camera camera, TerrainFrustum frustum, int renderDistance, boolean chunkCullingEnabled) {
		assert state.get() == IDLE;
		this.cameraRegion = cameraRegion;
		final BlockPos cameraBlockPos = camera.getBlockPos();
		cameraChunkOrigin = RenderRegionIndexer.blockPosToRegionOrigin(cameraBlockPos);
		assert cameraRegion == null || cameraChunkOrigin == cameraRegion.getOrigin().asLong();
		cameraOccluder.copyFrustum(frustum);
		this.renderDistance = renderDistance;
		this.chunkCullingEnabled = chunkCullingEnabled;

		state.set(READY);
		cancelled = false;
	}

	public int state() {
		return state.get();
	}

	public void reset() {
		cancelled = true;
		state.compareAndSet(COMPLETE, IDLE);
		visibleRegions.clear();
		cameraOccluder.invalidate();
	}

	@Override
	public void run(TerrainRenderContext ignored) {
		assert state.get() == READY;
		state.set(RUNNING);

		final int renderDistance = this.renderDistance;
		final RenderRegionStorage regionStorage = cwr.renderRegionStorage;
		regionStorage.updateCameraDistanceAndVisibility(cameraChunkOrigin);
		final boolean redrawOccluder = cameraOccluder.prepareScene();
		final int occluderVersion = cameraOccluder.version();

		if (TRACE_OCCLUSION_OUTCOMES) {
			CanvasMod.LOG.info("TerrainIterator Redraw Status: " + redrawOccluder);
		}

		if (cameraRegion == null) {
			// prime visible when above or below world and camera region is null
			final int y = BlockPos.unpackLongY(cameraChunkOrigin) > 0 ? 248 : 8;
			final int x = BlockPos.unpackLongX(cameraChunkOrigin);
			final int z = BlockPos.unpackLongZ(cameraChunkOrigin);
			final int limit = Useful.getLastDistanceSortedOffsetIndex(renderDistance);

			for (int i = 0; i < limit; ++i) {
				final Vec3i offset = Useful.getDistanceSortedCircularOffset(i);
				final BuiltRenderRegion region = regionStorage.getOrCreateRegion((offset.getX() << 4) + x, y, (offset.getZ() << 4) + z);

				if (region != null && region.isPotentiallyVisibleFromCamera()) {
					region.addToCameraPvsIfValid();
				}
			}
		} else {
			cameraRegion.forceCameraPotentialVisibility();
			cameraRegion.addToCameraPvsIfValid();
		}

		iterateTerrain(occluderVersion, redrawOccluder);

		if (cancelled) {
			state.set(IDLE);
			visibleRegions.clear();
		} else {
			assert state.get() == RUNNING;
			state.set(COMPLETE);

			if (Configurator.debugOcclusionRaster) {
				cameraOccluder.outputRaster();
			}
		}
	}

	private void iterateTerrain(int occluderVersion, boolean redrawOccluder) {
		final boolean chunkCullingEnabled = this.chunkCullingEnabled;
		final RenderRegionStorage regionStorage = cwr.renderRegionStorage;
		final CameraPotentiallyVisibleRegionSet cameraDistanceSorter = regionStorage.cameraPVS;

		updateRegions.clear();

		// PERF: look for ways to improve branch prediction
		while (!cancelled) {
			final BuiltRenderRegion builtRegion = cameraDistanceSorter.next();

			if (builtRegion == null) {
				break;
			}

			// don't visit if not in frustum and within render distance
			if (!builtRegion.isPotentiallyVisibleFromCamera()) {
				continue;
			}

			// don't visit if region is outside near distance and doesn't have all 4 neighbors loaded
			if (!builtRegion.isNearOrHasLoadedNeighbors()) {
				continue;
			}

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			final RegionData regionData = builtRegion.getBuildData();

			// If never built then don't do anything with it
			if (regionData == RegionData.UNBUILT) {
				builtRegion.markNeededForCameraVisibilityProgression();
				updateRegions.add(builtRegion);
				continue;
			}

			// If get to here has been built - if needs rebuilt we can use existing data this frame
			if (builtRegion.needsRebuild()) {
				builtRegion.markNeededForCameraVisibilityProgression();
				updateRegions.add(builtRegion);
			}

			// for empty regions, check neighbors if visible but don't add to visible set
			if (!regionData.canOcclude()) {
				if (Configurator.cullEntityRender) {
					// reuse prior test results
					if (builtRegion.cameraOccluderVersion() != occluderVersion) {
						if (!chunkCullingEnabled || builtRegion.isNear() || cameraOccluder.isEmptyRegionVisible(builtRegion.getOrigin())) {
							builtRegion.enqueueUnvistedCameraNeighbors();
							builtRegion.setCameraOccluderResult(true, occluderVersion);
						} else {
							builtRegion.setCameraOccluderResult(false, occluderVersion);
						}
					}
				} else {
					builtRegion.enqueueUnvistedCameraNeighbors();
					builtRegion.setCameraOccluderResult(false, occluderVersion);
				}

				continue;
			}

			if (!chunkCullingEnabled || builtRegion.isNear()) {
				builtRegion.enqueueUnvistedCameraNeighbors();
				visibleRegions.add(builtRegion);

				if (redrawOccluder || builtRegion.cameraOccluderVersion() != occluderVersion) {
					cameraOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange, builtRegion.squaredCameraChunkDistance());
					cameraOccluder.occlude(regionData.getOcclusionData());
				}

				builtRegion.setCameraOccluderResult(true, occluderVersion);
			} else if (builtRegion.cameraOccluderVersion() == occluderVersion) {
				// reuse prior test results
				if (builtRegion.cameraOccluderResult()) {
					builtRegion.enqueueUnvistedCameraNeighbors();
					visibleRegions.add(builtRegion);

					// will already have been drawn if occluder view version hasn't changed
					if (redrawOccluder) {
						cameraOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange, builtRegion.squaredCameraChunkDistance());
						cameraOccluder.occlude(regionData.getOcclusionData());
					}
				}
			} else {
				cameraOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange, builtRegion.squaredCameraChunkDistance());
				final int[] visData = regionData.getOcclusionData();

				if (cameraOccluder.isBoxVisible(visData[RegionOcclusionCalculator.OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX])) {
					builtRegion.enqueueUnvistedCameraNeighbors();
					visibleRegions.add(builtRegion);
					builtRegion.setCameraOccluderResult(true, occluderVersion);

					// these must always be drawn - will be additive if view hasn't changed
					cameraOccluder.occlude(visData);
				} else {
					builtRegion.setCameraOccluderResult(false, occluderVersion);
				}
			}
		}
	}

	@Override
	public int priority() {
		return -1;
	}
}
