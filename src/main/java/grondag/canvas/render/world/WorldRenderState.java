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

package grondag.canvas.render.world;

import java.util.BitSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.profiling.ProfilerFiller;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.render.frustum.TerrainFrustum;
import grondag.canvas.render.terrain.TerrainSectorMap;
import grondag.canvas.render.terrain.base.DrawableRegionList;
import grondag.canvas.render.terrain.cluster.VertexClusterRealm;
import grondag.canvas.render.terrain.drawlist.DrawListCullingHelper;
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
	public final TerrainSectorMap sectorManager = new TerrainSectorMap();

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

	private DrawableRegionList solidDrawList = DrawableRegionList.EMPTY;
	private DrawableRegionList translucentDrawList = DrawableRegionList.EMPTY;
	private final DrawableRegionList[] shadowDrawLists = new DrawableRegionList[ShadowMatrixData.CASCADE_COUNT];

	private RenderRegionBuilder regionBuilder;
	private ClientLevel world;
	private boolean hasSkylight;
	private String drawlistDebugSummary = "";

	// these are measured in chunks, not blocks
	private int chunkRenderDistance;
	private int squaredChunkRenderDistance;
	private int squaredChunkRetentionDistance;

	public final DrawListCullingHelper drawListCullingHlper = new DrawListCullingHelper(this);
	public final VertexClusterRealm solidClusterRealm = new VertexClusterRealm(false);
	public final VertexClusterRealm translucentClusterRealm = new VertexClusterRealm(true);
	public final BitSet terrainAnimationBits = new BitSet();

	public WorldRenderState(CanvasWorldRenderer cwr) {
		this.cwr = cwr;

		for (int i = 0; i < ShadowMatrixData.CASCADE_COUNT; ++i) {
			shadowVisibleRegions[i] = new VisibleRegionList();
			shadowDrawLists[i] = DrawableRegionList.EMPTY;
		}
	}

	void computeDistances() {
		@SuppressWarnings("resource")
		int renderDistance = Minecraft.getInstance().options.renderDistance().get();
		chunkRenderDistance = renderDistance;
		squaredChunkRenderDistance = renderDistance * renderDistance;
		renderDistance += 2;
		squaredChunkRetentionDistance = renderDistance * renderDistance;
	}

	void setWorld(@Nullable ClientLevel clientWorld) {
		// happens here to avoid creating before renderer is initialized
		if (regionBuilder == null) {
			regionBuilder = new RenderRegionBuilder();
		}

		// DitherTexture.instance().initializeIfNeeded();
		world = clientWorld;
		cameraVisibleRegions.clear();
		clearDrawSpecs();
		terrainIterator.reset(true);
		renderRegionStorage.clear(true);
		hasSkylight = world != null && world.dimensionType().hasSkyLight();
		solidClusterRealm.clear();
		translucentClusterRealm.clear();
	}

	public ClientLevel getWorld() {
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

		solidDrawList.close();
		solidDrawList = DrawableRegionList.build(cameraVisibleRegions, false, false);
		translucentDrawList.close();
		translucentDrawList = DrawableRegionList.build(cameraVisibleRegions, true, false);

		terrainAnimationBits.clear();
		final int cameraLimit = cameraVisibleRegions.size();

		for (int i = 0; i < cameraLimit; ++i) {
			terrainAnimationBits.or(cameraVisibleRegions.get(i).animationBits);
		}

		if (shadowsEnabled()) {
			int shadowQuadCount = 0;

			for (int i = 0; i < 4; ++i) {
				final var shadowList = shadowVisibleRegions[i];
				shadowDrawLists[i].close();
				shadowDrawLists[i] = DrawableRegionList.build(shadowList, false, true);
				shadowQuadCount += shadowDrawLists[i].quadCount();

				final int shadowLimit = shadowList.size();

				for (int j = 0; j < shadowLimit; ++j) {
					terrainAnimationBits.or(shadowList.get(j).animationBits);
				}
			}

			drawlistDebugSummary = String.format("Visible quads: %,ds  %,dt  %,dsh", solidDrawList.quadCount(), translucentDrawList.quadCount(), shadowQuadCount);
		} else {
			drawlistDebugSummary = String.format("Visible quads: %,ds  %,dt", solidDrawList.quadCount(), translucentDrawList.quadCount());
		}
	}

	public String drawlistDebugSummary() {
		return drawlistDebugSummary;
	}

	void clear() {
		computeDistances();
		terrainIterator.reset(false);
		regionRebuildManager.clear();

		if (regionBuilder != null) {
			regionBuilder.reset();
		}

		renderRegionStorage.clear(false);
		cameraVisibleRegions.clear();
		terrainFrustum.reload();
		clearDrawSpecs();
		sectorManager.clear();
		solidClusterRealm.clear();
		translucentClusterRealm.clear();
	}

	void clearDrawSpecs() {
		solidDrawList.close();
		solidDrawList = DrawableRegionList.EMPTY;

		translucentDrawList.close();
		translucentDrawList = DrawableRegionList.EMPTY;

		for (int i = 0; i < 4; ++i) {
			shadowDrawLists[i].close();
			shadowDrawLists[i] = DrawableRegionList.EMPTY;
		}
	}

	void renderSolidTerrain() {
		final ProfilerFiller prof = Minecraft.getInstance().getProfiler();
		prof.push("render_solid");
		MatrixState.set(MatrixState.REGION);
		solidDrawList.draw(this);
		MatrixState.set(MatrixState.CAMERA);
		prof.pop();
	}

	void renderTranslucentTerrain() {
		final ProfilerFiller prof = Minecraft.getInstance().getProfiler();
		prof.push("render_translucent");
		MatrixState.set(MatrixState.REGION);
		translucentDrawList.draw(this);
		MatrixState.set(MatrixState.CAMERA);
		prof.pop();
	}

	void renderShadowLayer(int cascadeIndex) {
		final ProfilerFiller prof = Minecraft.getInstance().getProfiler();
		prof.push("render_shadow");
		MatrixState.set(MatrixState.REGION);
		shadowDrawLists[cascadeIndex].draw(this);
		MatrixState.set(MatrixState.CAMERA);
		prof.pop();
	}
}
