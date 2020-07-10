package grondag.canvas.terrain.render;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.render.CanvasFrustum;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.terrain.BuiltRenderRegion;
import grondag.canvas.terrain.RegionData;
import grondag.canvas.terrain.RenderRegionStorage;
import grondag.canvas.terrain.occlusion.TerrainOccluder;
import grondag.canvas.terrain.occlusion.region.OcclusionRegion;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.Useful;

public class TerrainIterator implements  Consumer<TerrainRenderContext> {
	private final SimpleUnorderedArrayList<BuiltRenderRegion> regionListA = new SimpleUnorderedArrayList<>();
	private final SimpleUnorderedArrayList<BuiltRenderRegion> regionListB = new SimpleUnorderedArrayList<>();
	private final CanvasFrustum frustum = new CanvasFrustum();
	private final RenderRegionStorage renderRegionStorage;
	public final SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions = new SimpleUnorderedArrayList<>();
	private final TerrainOccluder terrainOccluder;

	private BuiltRenderRegion cameraRegion;
	private BlockPos cameraBlockPos;
	private int renderDistance;

	public static final int IDLE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int COMPLETE = 3;

	private final AtomicInteger state = new AtomicInteger(IDLE);
	public final BuiltRenderRegion[] visibleRegions = new BuiltRenderRegion[CanvasWorldRenderer.MAX_REGION_COUNT];
	public volatile int visibleRegionCount;
	private volatile boolean cancelled = false;

	public TerrainIterator(CanvasWorldRenderer cwr) {
		renderRegionStorage = cwr.regionStorage();
		terrainOccluder = cwr.terrainOccluder;
	}

	public void prepare(@Nullable BuiltRenderRegion cameraRegion,  BlockPos cameraBlockPos, CanvasFrustum frustum, int renderDistance)  {
		assert state.get() == IDLE;
		this.cameraRegion = cameraRegion;
		this.cameraBlockPos = cameraBlockPos;
		this.frustum.copy(frustum);
		this.renderDistance = renderDistance;
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

		final int renderDistance = this.renderDistance;
		final CanvasFrustum frustum = this.frustum;
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final MinecraftClient mc = MinecraftClient.getInstance();
		final boolean redrawOccluder = terrainOccluder.needsRedraw();
		final int occluderVersion = terrainOccluder.version();
		final BuiltRenderRegion[] visibleRegions = this.visibleRegions;
		int visibleRegionCount = 0;
		updateRegions.clear();

		SimpleUnorderedArrayList<BuiltRenderRegion> currentLevel  =  regionListA;
		currentLevel.clear();
		SimpleUnorderedArrayList<BuiltRenderRegion> nextLevel  =  regionListB;
		nextLevel.clear();

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
					currentLevel.add(region);
				}
			}
		}  else {
			final RegionData regionData = cameraRegion.getBuildData();
			final int[] visData =  regionData.getOcclusionData();

			if (visData != OcclusionRegion.EMPTY_CULL_DATA && visData != null) {
				visibleRegions[visibleRegionCount++] = cameraRegion;
				cameraRegion.occluderResult = true;
			} else {
				cameraRegion.occluderResult = false;
			}

			cameraRegion.enqueueUnvistedNeighbors(currentLevel);

			if (redrawOccluder || cameraRegion.occluderVersion != occluderVersion) {
				terrainOccluder.prepareRegion(cameraRegion.getOrigin(), cameraRegion.occlusionRange);
				terrainOccluder.occlude(visData);
			}

			cameraRegion.occluderVersion = occluderVersion;
		}

		assert !currentLevel.isEmpty();

		final boolean chunkCullingEnabled = mc.chunkCullingEnabled;

		// PERF: look for ways to improve branch prediction
		while (!cancelled) {
			if (currentLevel.isEmpty()) {
				if(nextLevel.isEmpty()) {
					break;
				} else {
					final SimpleUnorderedArrayList<BuiltRenderRegion> swapLevel = currentLevel;
					currentLevel  = nextLevel;
					nextLevel = swapLevel;
					nextLevel.clear();
				}
			}

			final BuiltRenderRegion builtRegion = currentLevel.removeLast();

			// don't visit if not in frustum
			if(!builtRegion.isInFrustum(frustum)) {
				continue;
			}

			// don't visit if region is outside near distance and doesn't have all 4 neighbors loaded
			// also checks for outside of render distance
			if (!builtRegion.shouldBuild()) {
				continue;
			}

			final RegionData regionData = builtRegion.getBuildData();
			final int[] visData =  regionData.getOcclusionData();

			if (visData == null) {
				updateRegions.add(builtRegion);
				continue;
			}

			if (builtRegion.needsRebuild()) {
				updateRegions.add(builtRegion);
			}

			// for empty regions, check neighbors if visible but don't add to visible set
			if (visData == OcclusionRegion.EMPTY_CULL_DATA) {
				if (Configurator.cullEntityRender) {
					if (builtRegion.occluderVersion == occluderVersion) {
						// reuse prior test results
						if (builtRegion.occluderResult) {
							builtRegion.enqueueUnvistedNeighbors(nextLevel);
						}
					} else {
						builtRegion.occluderVersion = occluderVersion;
						if (!chunkCullingEnabled || builtRegion.isNear() || terrainOccluder.isEmptyRegionVisible(builtRegion.getOrigin())) {
							builtRegion.enqueueUnvistedNeighbors(nextLevel);
							builtRegion.occluderResult = true;
						} else {
							builtRegion.occluderResult = false;
						}
					}
				} else {
					builtRegion.enqueueUnvistedNeighbors(nextLevel);
					builtRegion.occluderVersion = occluderVersion;
					builtRegion.occluderResult = false;
				}

				continue;
			}

			if (!chunkCullingEnabled || builtRegion.isNear()) {
				builtRegion.enqueueUnvistedNeighbors(nextLevel);
				visibleRegions[visibleRegionCount++] = builtRegion;

				if (redrawOccluder || builtRegion.occluderVersion != occluderVersion) {
					terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);
					terrainOccluder.occlude(visData);
				}

				builtRegion.occluderVersion = occluderVersion;
				builtRegion.occluderResult = true;
			} else if (builtRegion.occluderVersion == occluderVersion) {
				// reuse prior test results
				if (builtRegion.occluderResult) {
					builtRegion.enqueueUnvistedNeighbors(nextLevel);
					visibleRegions[visibleRegionCount++] = builtRegion;

					// will already have been drawn if occluder view version hasn't changed
					if (redrawOccluder) {
						terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);
						terrainOccluder.occlude(visData);
					}
				}
			} else {
				terrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);

				if (terrainOccluder.isBoxVisible(visData[OcclusionRegion.CULL_DATA_REGION_BOUNDS])) {
					builtRegion.enqueueUnvistedNeighbors(nextLevel);
					visibleRegions[visibleRegionCount++] = builtRegion;
					builtRegion.occluderVersion = occluderVersion;
					builtRegion.occluderResult = true;

					// these must always be drawn - will be additive if view hasn't changed
					terrainOccluder.occlude(visData);
				} else {
					// note that we don't update occluder version in this case
					// casues some chunks not to render if set - reason doesn't seem clear but
					// didn't actually contribute any information to occluder and should not be tied to it
					builtRegion.occluderResult = false;
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
