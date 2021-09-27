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

package grondag.canvas.terrain.region;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;
import grondag.canvas.terrain.occlusion.geometry.OcclusionResult;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;

public class RegionBuildState {
	/** value for new regions that never been built or have been built and then closed. */
	public static final RegionBuildState UNBUILT = new RegionBuildState();

	final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();
	OcclusionResult occlusionResult = RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT;

	@Nullable
	int[] translucentState;

	public List<BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	/**
	 * Persists data for translucency resort if needed, also performing initial sort.
	 * Should be called after vertex collection is complete.
	 */
	public void prepareTranslucentIfNeeded(Vec3 sortPos, RegionRenderSector sector, VertexCollectorList collectors) {
		final ArrayVertexCollector buffer = collectors.getIfExists(TerrainRenderStates.TRANSLUCENT_TERRAIN);

		if (buffer != null && !buffer.isEmpty()) {
			buffer.sortTerrainQuads(sortPos, sector);
			translucentState = buffer.saveState(translucentState);
		}
	}

	public OcclusionResult getOcclusionResult() {
		return occlusionResult;
	}

	public void setOcclusionResult(OcclusionResult occlusionResult) {
		this.occlusionResult = occlusionResult;
	}

	public boolean canOcclude() {
		return occlusionResult != RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT;
	}
}
