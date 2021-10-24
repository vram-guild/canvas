/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.buffer.input;

import java.nio.IntBuffer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;

public class ShadowVertexCollector extends BaseVertexCollector {
	protected final SimpleVertexCollector common;
	protected final SimpleVertexCollector colorOnly;

	public ShadowVertexCollector(RenderState renderState, int[] target) {
		super(renderState, target);
		common = new SimpleVertexCollector(renderState, target);
		colorOnly = new SimpleVertexCollector(renderState, target);
	}

	@Override
	public void commit(boolean castShadow) {
		(castShadow ? common : colorOnly).commit(quadStrideInts);
		integerSize += quadStrideInts;
	}

	@Override
	public final void clear() {
		integerSize = 0;
		common.clear();
		colorOnly.clear();
	}

	@Override
	public int commonVertexCount() {
		return common.vertexCount();
	}

	@Override
	public void commit(int size) {
		throw new UnsupportedOperationException("Commit on ShadowVertexCollector must provide castShadowl");
	}

	@Override
	public void commit(int effectiveFaceIndex, boolean castShadow) {
		commit(castShadow);
	}

	@Override
	public void toBuffer(IntBuffer intBuffer, int targetIndex) {
		if (!common.isEmpty()) {
			common.toBuffer(intBuffer, targetIndex);
			targetIndex += common.integerSize;
		}

		if (!colorOnly.isEmpty()) {
			colorOnly.toBuffer(intBuffer, targetIndex);
			targetIndex += colorOnly.integerSize;
		}
	}

	@Override
	public void toBuffer(TransferBuffer targetBuffer, int bufferTargetIndex) {
		if (!common.isEmpty()) {
			common.toBuffer(targetBuffer, bufferTargetIndex);
			bufferTargetIndex += common.integerSize;
		}

		if (!colorOnly.isEmpty()) {
			colorOnly.toBuffer(targetBuffer, bufferTargetIndex);
			bufferTargetIndex += colorOnly.integerSize;
		}
	}

	@Override
	public void sortIfNeeded() {
		// NOOP
	}

	@Override
	public boolean sorted() {
		return false;
	}

	@Override
	public boolean sortTerrainQuads(Vec3 sortPos, RegionRenderSector sector) {
		throw new UnsupportedOperationException("ShadowVertexCollector vertex collector does not support sortTerrainQuads.");
	}

	@Override
	public @Nullable int[] saveState(@Nullable int[] translucentState) {
		throw new UnsupportedOperationException("ShadowVertexCollector vertex collector does not support saveState.");
	}

	@Override
	public void loadState(int[] state) {
		throw new UnsupportedOperationException("ShadowVertexCollector vertex collector does not support loadState");
	}

	@Override
	public FaceBucket[] vertexBuckets() {
		return null;
	}
}
