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

package grondag.canvas.render.world;

import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;

import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.RegionDrawList;
import grondag.canvas.render.region.base.RegionDrawListBuilder;
import grondag.canvas.render.region.vf.VfDrawList;
import grondag.canvas.render.region.vs.RenderSectorMap;
import grondag.canvas.shader.data.MatrixState;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.terrain.occlusion.SortableVisibleRegionList;
import grondag.canvas.terrain.occlusion.TerrainIterator;
import grondag.canvas.terrain.occlusion.VisibleRegionList;
import grondag.canvas.terrain.region.RegionRebuildManager;
import grondag.canvas.terrain.region.RenderRegionBuilder;
import grondag.canvas.terrain.region.RenderRegionStorage;

/**
 * Holds most of the state needed by the world renderer, allowing that
 * class to be simpler. The relationship between member components
 * is a network, not a hierarchy - each component manages a particular set of
 * concerns and talks with other components directly as needed.
 */
public class WorldRenderState {
	/** Provides access to world renderer state/functions that can't live here. */
	public final CanvasWorldRenderer cwr;

	/** Tracks which regions had rebuilds requested, both camera and shadow view, and causes some to get built each frame. */
	public final RegionRebuildManager regionRebuildManager = new RegionRebuildManager();

	public final TerrainIterator terrainIterator = new TerrainIterator(this);
	public final RenderRegionStorage renderRegionStorage = new RenderRegionStorage(this);
	public final RenderSectorMap sectorManager = new RenderSectorMap();

	/**
	 * Updated every frame and used by external callers looking for the vanilla world renderer frustum.
	 * Differs from vanilla in that it may not include FOV distortion in the frustum and can include
	 * some padding to minimize edge tearing.
	 *
	 * <p>A snapshot of this is used for terrain culling - usually off thread. The snapshot lives inside TerrainOccluder.
	 */
	public final TerrainFrustum terrainFrustum = new TerrainFrustum();

	public final SortableVisibleRegionList cameraVisibleRegions = new SortableVisibleRegionList();
	public final VisibleRegionList[] shadowVisibleRegions = new VisibleRegionList[ShadowMatrixData.CASCADE_COUNT];

	private RegionDrawList solidDrawList = RegionDrawList.EMPTY;
	private RegionDrawList translucentDrawList = RegionDrawList.EMPTY;
	private final RegionDrawList[] shadowDrawLists = new RegionDrawList[ShadowMatrixData.CASCADE_COUNT];

	private RenderRegionBuilder regionBuilder;
	private ClientWorld world;
	private boolean hasSkylight;

	// these are measured in chunks, not blocks
	private int chunkRenderDistance;
	private int squaredChunkRenderDistance;
	private int squaredChunkRetentionDistance;

	public WorldRenderState(CanvasWorldRenderer cwr) {
		this.cwr = cwr;

		for (int i = 0; i < ShadowMatrixData.CASCADE_COUNT; ++i) {
			shadowVisibleRegions[i] = new VisibleRegionList();
			shadowDrawLists[i] = VfDrawList.EMPTY;
		}
	}

	void computeDistances() {
		@SuppressWarnings("resource")
		int renderDistance = MinecraftClient.getInstance().options.viewDistance;
		chunkRenderDistance = renderDistance;
		squaredChunkRenderDistance = renderDistance * renderDistance;
		renderDistance += 2;
		squaredChunkRetentionDistance = renderDistance * renderDistance;
	}

	void setWorld(@Nullable ClientWorld clientWorld) {
		// happens here to avoid creating before renderer is initialized
		if (regionBuilder == null) {
			regionBuilder = new RenderRegionBuilder();
		}

		// DitherTexture.instance().initializeIfNeeded();
		world = clientWorld;
		cameraVisibleRegions.clear();
		clearDrawSpecs();
		terrainIterator.reset();
		renderRegionStorage.clear();
		hasSkylight = world != null && world.getDimension().hasSkyLight();
	}

	public ClientWorld getWorld() {
		return world;
	}

	public boolean shadowsEnabled() {
		return Pipeline.shadowsEnabled() && hasSkylight;
	}

	public int chunkRenderDistance() {
		return chunkRenderDistance;
	}

	public int maxSquaredChunkRenderDistance() {
		return squaredChunkRenderDistance;
	}

	public int maxSquaredChunkRetentionDistance() {
		return squaredChunkRetentionDistance;
	}

	public RenderRegionBuilder regionBuilder() {
		return regionBuilder;
	}

	private boolean areDrawListsValid = false;

	public void invalidateDrawLists() {
		areDrawListsValid = false;
	}

	void copyVisibleRegionsFromIterator() {
		final TerrainIterator terrainIterator = this.terrainIterator;

		cameraVisibleRegions.copyFrom(terrainIterator.visibleRegions);

		if (shadowsEnabled()) {
			shadowVisibleRegions[0].copyFrom(terrainIterator.shadowVisibleRegions[0]);
			shadowVisibleRegions[1].copyFrom(terrainIterator.shadowVisibleRegions[1]);
			shadowVisibleRegions[2].copyFrom(terrainIterator.shadowVisibleRegions[2]);
			shadowVisibleRegions[3].copyFrom(terrainIterator.shadowVisibleRegions[3]);
		}

		invalidateDrawLists();
	}

	void rebuidDrawListsIfNeeded() {
		if (areDrawListsValid) {
			return;
		}

		areDrawListsValid = true;

		final Function<ObjectArrayList<DrawableRegion>, RegionDrawList> drawListFunc = Configurator.terrainRenderConfig.drawListFunc;

		Configurator.terrainRenderConfig.beforeDrawListBuild();

		solidDrawList.close();
		solidDrawList = RegionDrawListBuilder.build(cameraVisibleRegions, drawListFunc, false);
		translucentDrawList.close();
		translucentDrawList = RegionDrawListBuilder.build(cameraVisibleRegions, drawListFunc, true);

		if (shadowsEnabled()) {
			for (int i = 0; i < 4; ++i) {
				shadowDrawLists[i].close();
				shadowDrawLists[i] = RegionDrawListBuilder.build(shadowVisibleRegions[i], drawListFunc, false);
			}
		}

		Configurator.terrainRenderConfig.afterDrawListBuild();
	}

	void clear() {
		computeDistances();
		terrainIterator.reset();
		regionRebuildManager.clear();

		if (regionBuilder != null) {
			regionBuilder.reset();
		}

		renderRegionStorage.clear();
		cameraVisibleRegions.clear();
		terrainFrustum.reload();
		clearDrawSpecs();
		sectorManager.clear();
		Configurator.terrainRenderConfig.reload(this);
	}

	void clearDrawSpecs() {
		solidDrawList.close();
		solidDrawList = VfDrawList.EMPTY;

		translucentDrawList.close();
		translucentDrawList = VfDrawList.EMPTY;

		for (int i = 0; i < 4; ++i) {
			shadowDrawLists[i].close();
			shadowDrawLists[i] = VfDrawList.EMPTY;
		}
	}

	void renderSolidTerrain() {
		final Profiler prof = MinecraftClient.getInstance().getProfiler();
		prof.push("render_solid");
		MatrixState.set(MatrixState.REGION);
		solidDrawList.draw();
		MatrixState.set(MatrixState.CAMERA);
		prof.pop();
	}

	void renderTranslucentTerrain() {
		final Profiler prof = MinecraftClient.getInstance().getProfiler();
		prof.push("render_translucent");
		MatrixState.set(MatrixState.REGION);
		translucentDrawList.draw();
		MatrixState.set(MatrixState.CAMERA);
		prof.pop();
	}

	void renderShadowLayer(int cascadeIndex) {
		final Profiler prof = MinecraftClient.getInstance().getProfiler();
		prof.push("render_shadow");
		MatrixState.set(MatrixState.REGION);
		shadowDrawLists[cascadeIndex].draw();
		MatrixState.set(MatrixState.CAMERA);
		prof.pop();
	}
}
