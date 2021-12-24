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

package grondag.canvas.terrain.region;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import grondag.canvas.buffer.input.DrawableVertexCollector;
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
		final DrawableVertexCollector buffer = collectors.getIfExists(TerrainRenderStates.TRANSLUCENT_TERRAIN);

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
