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
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.occlusion.geometry.OcclusionRegion;
import grondag.canvas.terrain.region.BuiltRenderRegion;
import grondag.canvas.terrain.region.RegionData;
import grondag.canvas.terrain.region.RenderRegionStorage;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.Useful;

public class TerrainIterator implements Consumer<TerrainRenderContext> {
	public static final boolean TRACE_OCCLUSION_OUTCOMES = Configurator.traceOcclusionOutcomes;

	public static final int IDLE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int COMPLETE = 3;
	public final SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions = new SimpleUnorderedArrayList<>();
	public final BuiltRenderRegion[] visibleRegions = new BuiltRenderRegion[CanvasWorldRenderer.MAX_REGION_COUNT];
	private final RenderRegionStorage renderRegionStorage;
	public final TerrainOccluder terrainOccluder;
	private final AtomicInteger state = new AtomicInteger(IDLE);
	private final RegionDistanceSorter distanceSorter = new RegionDistanceSorter();
	public volatile int visibleRegionCount;
	private BuiltRenderRegion cameraRegion;
	private Vec3d cameraPos;
	private long cameraChunkOrigin;
	private int renderDistance;
	private boolean chunkCullingEnabled = true;
	private volatile boolean cancelled = false;

	public TerrainIterator(RenderRegionStorage renderRegionStorage, TerrainOccluder terrainOccluder) {
		this.renderRegionStorage = renderRegionStorage;
		this.terrainOccluder = terrainOccluder;
	}

	public void prepare(@Nullable BuiltRenderRegion cameraRegion, Camera camera, CanvasFrustum frustum, int renderDistance, boolean chunkCullingEnabled) {
		assert state.get() == IDLE;
		this.cameraRegion = cameraRegion;
		cameraPos = camera.getPos();
		final BlockPos cameraBlockPos = camera.getBlockPos();
		cameraChunkOrigin = BlockPos.asLong(cameraBlockPos.getX() & 0xFFFFFFF0, cameraBlockPos.getY() & 0xFFFFFFF0, cameraBlockPos.getZ() & 0xFFFFFFF0);
		terrainOccluder.frustum.copy(frustum);
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
	}

	@Override
	public void accept(TerrainRenderContext ignored) {
		assert state.get() == READY;
		state.set(RUNNING);

		final boolean chunkCullingEnabled = this.chunkCullingEnabled;
		final int renderDistance = this.renderDistance;
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		final RegionDistanceSorter distanceSorter = this.distanceSorter;
		int visibleRegionCount = 0;
		updateRegions.clear();
		distanceSorter.clear();
		BuiltRenderRegion.advanceFrameIndex();

		renderRegionStorage.updateCameraDistanceAndVisibilityInfo(cameraChunkOrigin);
		final boolean redrawOccluder = terrainOccluder.prepareScene(cameraPos);
		final int occluderVersion = terrainOccluder.version();

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

				if (region != null && region.isInFrustum()) {
					distanceSorter.add(region);
				}
			}
		} else {
			final RegionData regionBuildData = cameraRegion.getBuildData();
			final int[] visData = regionBuildData.getOcclusionData();

			if (visData != OcclusionRegion.EMPTY_CULL_DATA && visData != null) {
				visibleRegions[visibleRegionCount++] = cameraRegion;

				if (redrawOccluder || cameraRegion.occluderVersion() != occluderVersion) {
					terrainOccluder.prepareRegion(cameraRegion.getOrigin(), cameraRegion.occlusionRange, cameraRegion.squaredChunkDistance());
					terrainOccluder.occlude(visData);
				}
			}

			cameraRegion.enqueueUnvistedNeighbors(distanceSorter);
			cameraRegion.setOccluderResult(true, occluderVersion);
		}

		// PERF: look for ways to improve branch prediction
		while (!cancelled) {
			final BuiltRenderRegion builtRegion = distanceSorter.next();

			if (builtRegion == null) {
				break;
			}

			// WIP: remove
			//			if (builtRegion.getOrigin().getX() >> 4 == -19 >> 4 && builtRegion.getOrigin().getY() >> 4 == 83 >> 4 && builtRegion.getOrigin().getZ() >> 4 == -27 >> 4) {
			//				System.out.println("boop");
			//			}

			// don't visit if not in frustum and within render distance
			if (!builtRegion.isInFrustum()) {
				continue;
			}

			// don't visit if region is outside near distance and doesn't have all 4 neighbors loaded
			// also checks for outside of render distance
			if (!builtRegion.shouldBuild()) {
				continue;
			}

			// Use build data for visibility - render data lags in availability and should only be used for rendering
			final RegionData regionData = builtRegion.getBuildData();
			final int[] visData = regionData.getOcclusionData();

			// If never built then don't do anything with it
			if (regionData == RegionData.UNBUILT) {
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
					if (builtRegion.occluderVersion() == occluderVersion) {
						// reuse prior test results
						if (builtRegion.occluderResult()) {
							builtRegion.enqueueUnvistedNeighbors(distanceSorter);
						}
					} else {
						if (!chunkCullingEnabled || builtRegion.isNear() || terrainOccluder.isEmptyRegionVisible(builtRegion.getOrigin())) {
							builtRegion.enqueueUnvistedNeighbors(distanceSorter);
							builtRegion.setOccluderResult(true, occluderVersion);
						} else {
							builtRegion.setOccluderResult(false, occluderVersion);
						}
					}
				} else {
					builtRegion.enqueueUnvistedNeighbors(distanceSorter);
					builtRegion.setOccluderResult(false, occluderVersion);
				}

				continue;
			}

			if (!chunkCullingEnabled || builtRegion.isNear()) {
				builtRegion.enqueueUnvistedNeighbors(distanceSorter);
				visibleRegions[visibleRegionCount++] = builtRegion;

				if (redrawOccluder || builtRegion.occluderVersion() != occluderVersion) {
					terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange, builtRegion.squaredChunkDistance());
					terrainOccluder.occlude(visData);
				}

				builtRegion.setOccluderResult(true, occluderVersion);
			} else if (builtRegion.occluderVersion() == occluderVersion) {
				// reuse prior test results
				if (builtRegion.occluderResult()) {
					builtRegion.enqueueUnvistedNeighbors(distanceSorter);
					visibleRegions[visibleRegionCount++] = builtRegion;

					// will already have been drawn if occluder view version hasn't changed
					if (redrawOccluder) {
						terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange, builtRegion.squaredChunkDistance());
						terrainOccluder.occlude(visData);
					}
				}
			} else {
				terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange, builtRegion.squaredChunkDistance());

				if (terrainOccluder.isBoxVisible(visData[OcclusionRegion.CULL_DATA_REGION_BOUNDS])) {
					builtRegion.enqueueUnvistedNeighbors(distanceSorter);
					visibleRegions[visibleRegionCount++] = builtRegion;
					builtRegion.setOccluderResult(true, occluderVersion);

					// these must always be drawn - will be additive if view hasn't changed
					terrainOccluder.occlude(visData);
				} else {
					builtRegion.setOccluderResult(false, occluderVersion);
				}
			}
		}

		if (cancelled) {
			state.set(IDLE);
			this.visibleRegionCount = 0;
		} else {
			assert state.get() == RUNNING;
			state.set(COMPLETE);
			this.visibleRegionCount = visibleRegionCount;

			if (Configurator.debugOcclusionRaster) {
				terrainOccluder.outputRaster();
			}
		}
	}
}
