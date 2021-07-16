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

package grondag.canvas.render.region.vf;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.base.AbstractDrawableRegion;
import grondag.canvas.vf.TerrainVertexFetch;
import grondag.canvas.vf.storage.VfStorageReference;

public class VfDrawableRegion extends AbstractDrawableRegion<VfDrawableState> {
	protected VfDrawableRegion(VfDrawableState delegate, long packedOriginBlockPos) {
		super(delegate, packedOriginBlockPos);
	}

	@Override
	protected void closeInner() {
		// NOOP
	}

	public static DrawableRegion pack(VertexCollectorList collectorList, boolean translucent, int byteCount, long packedOriginBlockPos) {
		final ObjectArrayList<ArrayVertexCollector> drawList = collectorList.sortedDrawList(translucent ? TRANSLUCENT : SOLID);

		if (drawList.isEmpty()) {
			return EMPTY_DRAWABLE;
		}

		final ArrayVertexCollector collector = drawList.get(0);

		// WIP: restore ability to have more than one pass in non-translucent terrain, for decals, etc.
		assert drawList.size() == 1;
		assert collector.renderState.sorted == translucent;

		final int quadIntCount = collector.quadCount() * VfFormat.VF_QUAD_STRIDE;
		final int[] vfData = new int[quadIntCount];
		System.arraycopy(collector.data(), 0, vfData, 0, quadIntCount);
		final VfStorageReference vfbr = VfStorageReference.of(vfData);
		TerrainVertexFetch.QUADS.enqueue(vfbr);
		return new VfDrawableRegion(new VfDrawableState(collector.renderState, collector.quadCount() * 4, vfbr), packedOriginBlockPos);
	}
}
