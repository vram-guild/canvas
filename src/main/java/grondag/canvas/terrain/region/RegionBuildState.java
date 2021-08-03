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

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.render.terrain.RegionRenderSectorMap.RegionRenderSector;
import grondag.canvas.terrain.occlusion.geometry.RegionOcclusionCalculator;

@Environment(EnvType.CLIENT)
public class RegionBuildState {
	/** value for new regions that never been built or have been built and then closed. */
	public static final RegionBuildState UNBUILT = new RegionBuildState();

	final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();
	int[] occlusionData = RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT;

	@Nullable
	int[] translucentState;

	public List<BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	/**
	 * Persists data for translucency resort if needed, also performing initial sort.
	 * Should be called after vertex collection is complete.
	 */
	public void prepareTranslucentIfNeeded(Vec3d sortPos, RegionRenderSector sector, VertexCollectorList collectors) {
		final ArrayVertexCollector buffer = collectors.getIfExists(RenderLayerHelper.TRANSLUCENT_TERRAIN);

		if (buffer != null && !buffer.isEmpty()) {
			buffer.sortTerrainQuads(sortPos, sector);
			translucentState = buffer.saveState(translucentState);
		}
	}

	public int[] getOcclusionData() {
		return occlusionData;
	}

	public void setOcclusionData(int[] occlusionData) {
		this.occlusionData = occlusionData;
	}

	public boolean canOcclude() {
		return occlusionData != RegionOcclusionCalculator.EMPTY_OCCLUSION_RESULT;
	}
}
