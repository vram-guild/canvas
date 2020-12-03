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

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.BuiltRenderRegion;
import grondag.canvas.terrain.RegionData;
import grondag.canvas.terrain.RenderRegionStorage;
import grondag.canvas.terrain.occlusion.region.OcclusionRegion;
import grondag.canvas.terrain.occlusion.region.PackedBox;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.Useful;

public class TerrainIterator implements Consumer<TerrainRenderContext> {
	public static final int IDLE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int COMPLETE = 3;
	public final SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions = new SimpleUnorderedArrayList<>();
	public final BuiltRenderRegion[] visibleRegions = new BuiltRenderRegion[CanvasWorldRenderer.MAX_REGION_COUNT];
	private final CanvasFrustum frustum = new CanvasFrustum();
	private final RenderRegionStorage renderRegionStorage;
	private final TerrainOccluder terrainOccluder;
	private final AtomicInteger state = new AtomicInteger(IDLE);
	private final TerrainDistanceSorter distanceSorter = new TerrainDistanceSorter();
	public volatile int visibleRegionCount;
	private BuiltRenderRegion cameraRegion;
	private BlockPos cameraBlockPos;
	private int renderDistance;
	private boolean chunkCullingEnabled = true;
	private volatile boolean cancelled = false;

	public TerrainIterator(CanvasWorldRenderer cwr) {
		renderRegionStorage = cwr.regionStorage();
		terrainOccluder = cwr.terrainOccluder;
	}

	public void prepare(@Nullable BuiltRenderRegion cameraRegion, BlockPos cameraBlockPos, CanvasFrustum frustum, int renderDistance, boolean chunkCullingEnabled) {
		assert state.get() == IDLE;
		this.cameraRegion = cameraRegion;
		this.cameraBlockPos = cameraBlockPos;
		this.frustum.copy(frustum);
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

	// WIP: remove
	int magicVersion = -1;

	@Override
	public void accept(TerrainRenderContext ignored) {
		assert state.get() == READY;
		state.set(RUNNING);

		final boolean chunkCullingEnabled = this.chunkCullingEnabled;
		final int renderDistance = this.renderDistance;
		final CanvasFrustum frustum = this.frustum;
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final boolean redrawOccluder = terrainOccluder.needsRedraw();
		final int occluderVersion = terrainOccluder.version();
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		final TerrainDistanceSorter distanceSorter = this.distanceSorter;
		int visibleRegionCount = 0;
		updateRegions.clear();
		distanceSorter.clear();
		BuiltRenderRegion.advanceFrameIndex();

		if (cameraRegion == null) {
			// prime visible when above or below world and camera region is null
			final int y = cameraBlockPos.getY() > 0 ? 248 : 8;
			final int x = cameraBlockPos.getX();
			final int z = cameraBlockPos.getZ();

			final int limit = Useful.getLastDistanceSortedOffsetIndex(renderDistance);

			for (int i = 0; i < limit; ++i) {
				final Vec3i offset = Useful.getDistanceSortedCircularOffset(i);

				final BuiltRenderRegion region = regionStorage.getOrCreateRegion((offset.getX() << 4) + x, y, (offset.getZ() << 4) + z);

				if (region != null && region.isInFrustum(frustum)) {
					distanceSorter.add(region);
				}
			}
		} else {
			final RegionData regionData = cameraRegion.getBuildData();
			final int[] visData = regionData.getOcclusionData();

			if (visData != OcclusionRegion.EMPTY_CULL_DATA && visData != null) {
				visibleRegions[visibleRegionCount++] = cameraRegion;

				if (redrawOccluder || cameraRegion.occluderVersion != occluderVersion) {
					terrainOccluder.prepareRegion(cameraRegion.getOrigin(), cameraRegion.occlusionRange);
					terrainOccluder.occlude(visData);
				}
			}

			cameraRegion.occluderVersion = occluderVersion;
			cameraRegion.enqueueUnvistedNeighbors(distanceSorter);
			cameraRegion.occluderResult = true;
		}

		if (magicVersion != frustum.viewVersion()) {
			System.out.println("NEW VIEW VERSION: " + magicVersion);
			magicVersion = frustum.viewVersion();
			CanvasWorldRenderer.doMagicA = true;
			CanvasWorldRenderer.doMagicB = true;
			CanvasWorldRenderer.doMagicC = true;
		}

		// WIP: removve
		int drawSequence = 0;
		int iterationNo = -1;
		int maxDist = 0;
		int maxSimpDist = 0;

		if (CanvasWorldRenderer.doFirstSnapshot) {
			terrainOccluder.outputRaster("snapshot_" + magicVersion + ".png", true);
			CanvasWorldRenderer.doFirstSnapshot = false;
		}

		// PERF: look for ways to improve branch prediction
		while (!cancelled) {
			++iterationNo;
			final BuiltRenderRegion builtRegion = distanceSorter.next();

			if (builtRegion == null) {
				break;
			}

			// WIP: remove
			if (builtRegion.sqCameraDist() < maxDist) {
				final double d0 = Math.sqrt(builtRegion.sqCameraDist());
				final double d1 = Math.sqrt(maxDist);

				if (d0 - d1 > 8.0) {
					System.out.println("cmaera dx: " + (d0 - d1));
				}
			} else {
				maxDist = builtRegion.sqCameraDist();
			}

			// WIP: remove
			if (builtRegion.simpleCameraDistance() < maxSimpDist) {
				System.out.println("oops");
			} else {
				maxSimpDist = builtRegion.simpleCameraDistance();
			}

			// don't visit if not in frustum
			if (!builtRegion.isInFrustum(frustum)) {
				if (builtRegion.isMagic) {
					System.out.println("BADNESS: FRUSTUM");
				}

				continue;
			}

			// don't visit if region is outside near distance and doesn't have all 4 neighbors loaded
			// also checks for outside of render distance
			if (!builtRegion.shouldBuild()) {
				if (builtRegion.isMagic) {
					System.out.println("BADNESS: SHOULD BUILD");
				}

				continue;
			}

			final RegionData regionData = builtRegion.getBuildData();
			final int[] visData = regionData.getOcclusionData();

			if (visData == null) {
				updateRegions.add(builtRegion);
				continue;
			}

			if (builtRegion.needsRebuild()) {
				updateRegions.add(builtRegion);
			}

			// for empty regions, check neighbors if visible but don't add to visible set
			if (visData == OcclusionRegion.EMPTY_CULL_DATA) {
				if (builtRegion.isMagic) {
					System.out.println("BADNESS: EMPTY");
				}

				if (Configurator.cullEntityRender) {
					if (builtRegion.occluderVersion == occluderVersion) {
						// reuse prior test results
						if (builtRegion.occluderResult) {
							builtRegion.enqueueUnvistedNeighbors(distanceSorter);
						}
					} else {
						builtRegion.occluderVersion = occluderVersion;

						if (!chunkCullingEnabled || builtRegion.isNear() || terrainOccluder.isEmptyRegionVisible(builtRegion.getOrigin())) {
							builtRegion.enqueueUnvistedNeighbors(distanceSorter);
							builtRegion.occluderResult = true;
						} else {
							builtRegion.occluderResult = false;
						}
					}
				} else {
					builtRegion.enqueueUnvistedNeighbors(distanceSorter);
					builtRegion.occluderVersion = occluderVersion;
					builtRegion.occluderResult = false;
				}

				continue;
			} else if (builtRegion.isMagic && CanvasWorldRenderer.doMagicA) {
				CanvasWorldRenderer.doMagicA = false;
				System.out.println("Magic Box: " + PackedBox.toString(visData[OcclusionRegion.CULL_DATA_REGION_BOUNDS]));
				System.out.println("Magic Origin: " + builtRegion.getOrigin().toShortString() + "      Range: " + builtRegion.occlusionRange);
			}

			if (!chunkCullingEnabled || builtRegion.isNear()) {
				builtRegion.enqueueUnvistedNeighbors(distanceSorter);
				visibleRegions[visibleRegionCount++] = builtRegion;

				if (builtRegion.isMagic && CanvasWorldRenderer.doMagicC) {
					System.out.println("GOODNESS: NEAR");
					CanvasWorldRenderer.doMagicC = false;
				}

				if (redrawOccluder || builtRegion.occluderVersion != occluderVersion) {
					++drawSequence;
					terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);
					terrainOccluder.occlude(visData);
				}

				builtRegion.occluderVersion = occluderVersion;
				builtRegion.occluderResult = true;
			} else if (builtRegion.occluderVersion == occluderVersion) {
				// reuse prior test results
				if (builtRegion.occluderResult) {
					builtRegion.enqueueUnvistedNeighbors(distanceSorter);
					visibleRegions[visibleRegionCount++] = builtRegion;

					// will already have been drawn if occluder view version hasn't changed
					if (redrawOccluder) {
						++drawSequence;
						terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);
						terrainOccluder.occlude(visData);
					}

					if (builtRegion.isMagic && CanvasWorldRenderer.doMagicC) {
						System.out.println("GOODNESS: REUSE");
						CanvasWorldRenderer.doMagicC = false;
					}
				} else if (builtRegion.isMagic) {
					System.out.println("BADNESS: OLD TEST");
				}
			} else {
				terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);

				if (builtRegion.isMagic && CanvasWorldRenderer.doMagicB) {
					CanvasWorldRenderer.doMagicB = false;
					System.out.println("OccluderState - itNo:" + iterationNo + "  drawSequence:" + drawSequence + "  " + terrainOccluder.toString());
					terrainOccluder.outputRaster("magic_raster_" + magicVersion + ".png", true);
				}

				if (terrainOccluder.isBoxVisible(visData[OcclusionRegion.CULL_DATA_REGION_BOUNDS])) {
					builtRegion.enqueueUnvistedNeighbors(distanceSorter);
					visibleRegions[visibleRegionCount++] = builtRegion;
					builtRegion.occluderVersion = occluderVersion;
					builtRegion.occluderResult = true;

					++drawSequence;
					// these must always be drawn - will be additive if view hasn't changed
					terrainOccluder.occlude(visData);

					if (builtRegion.isMagic && CanvasWorldRenderer.doMagicC) {
						CanvasWorldRenderer.doMagicC = false;
						System.out.println("GOODNESS: TEST SUCCESS");
					}
				} else {
					// note that we don't update occluder version in this case
					// casues some chunks not to render if set - reason doesn't seem clear but
					// didn't actually contribute any information to occluder and should not be tied to it
					builtRegion.occluderResult = false;

					if (builtRegion.isMagic && CanvasWorldRenderer.doMagicC) {
						CanvasWorldRenderer.doMagicC = false;
						System.out.println("BADNESS: TEST FAILED");
					}
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
