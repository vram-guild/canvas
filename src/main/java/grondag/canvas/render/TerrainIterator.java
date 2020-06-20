package grondag.canvas.render;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.chunk.BuiltRenderRegion;
import grondag.canvas.chunk.RegionData;
import grondag.canvas.chunk.RenderRegionStorage;
import grondag.canvas.chunk.occlusion.TerrainOccluder;
import grondag.canvas.chunk.occlusion.region.OcclusionRegion;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.Useful;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class TerrainIterator implements  Consumer<TerrainRenderContext> {
	private final SimpleUnorderedArrayList<BuiltRenderRegion> regionListA = new SimpleUnorderedArrayList<>();
	private final SimpleUnorderedArrayList<BuiltRenderRegion> regionListB = new SimpleUnorderedArrayList<>();
	private final CanvasFrustum frustum = new CanvasFrustum();
	private RenderRegionStorage renderRegionStorage;
	public final SimpleUnorderedArrayList<BuiltRenderRegion> updateRegions = new SimpleUnorderedArrayList<>();

	private BuiltRenderRegion cameraRegion;
	private BlockPos cameraBlockPos;
	private int renderDistance;

	public static final int IDLE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int COMPLETE = 3;

	private final AtomicInteger state = new AtomicInteger(IDLE);
	public BuiltRenderRegion[] visibleRegions = new BuiltRenderRegion[4096];
	public volatile int visibleRegionCount;
	private volatile boolean cancelled = false;

	public void setRegionStorage(RenderRegionStorage renderRegionStorage) {
		this.renderRegionStorage = renderRegionStorage;
		visibleRegions = new BuiltRenderRegion[renderRegionStorage.regionCount()];
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

	int state() {
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

		final CanvasFrustum frustum = this.frustum;
		final int renderDistance = this.renderDistance;
		final RenderRegionStorage regionStorage = renderRegionStorage;
		final BuiltRenderRegion[] regions = regionStorage.regions();
		final MinecraftClient mc = MinecraftClient.getInstance();
		final boolean redrawOccluder = TerrainOccluder.needsRedraw();
		final int occluderVersion = TerrainOccluder.version();
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
				final int regionIndex = regionStorage.getRegionIndexFromBlockPos((offset.getX() << 4) + x, y, (offset.getZ() << 4) + z);

				if (regionIndex != -1) {
					final BuiltRenderRegion region = regions[regionIndex];

					if (region.isInFrustum(frustum)) {
						currentLevel.add(region);
					}
				}
			}
		}  else {
			currentLevel.add(cameraRegion);
		}

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

			// for empty regions, check neighbors but don't add to visible set
			if (visData == OcclusionRegion.EMPTY_CULL_DATA) {
				builtRegion.enqueueUnvistedNeighbors(nextLevel);
				builtRegion.occluderVersion = occluderVersion;
				builtRegion.occluderResult = false;
				continue;
			}

			if (!chunkCullingEnabled || builtRegion == cameraRegion || builtRegion.isNear()) {
				builtRegion.enqueueUnvistedNeighbors(nextLevel);
				visibleRegions[visibleRegionCount++] = builtRegion;

				if (redrawOccluder || builtRegion.occluderVersion != occluderVersion) {
					TerrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);
					TerrainOccluder.occlude(visData);
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
						TerrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);
						TerrainOccluder.occlude(visData);
					}
				}
			} else {
				TerrainOccluder.prepareRegion(builtRegion.getOrigin(), builtRegion.occlusionRange);

				if (TerrainOccluder.isBoxVisible(visData[OcclusionRegion.CULL_DATA_REGION_BOUNDS])) {
					builtRegion.enqueueUnvistedNeighbors(nextLevel);
					visibleRegions[visibleRegionCount++] = builtRegion;
					builtRegion.occluderVersion = occluderVersion;
					builtRegion.occluderResult = true;

					// these must always be drawn - will be additive if view hasn't changed
					TerrainOccluder.occlude(visData);
				} else {
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
				TerrainOccluder.outputRaster();
			}
		}
	}
}
