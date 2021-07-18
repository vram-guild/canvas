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

package grondag.canvas.render.terrain.vbo;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.input.ArrayVertexCollector;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.buffer.render.StaticDrawBuffer;
import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.render.terrain.base.AbstractDrawableRegion;
import grondag.canvas.render.terrain.base.DrawableRegion;

public class VboDrawableRegion extends AbstractDrawableRegion<VboDrawableState> {
	private VboDrawableRegion(VboDrawableState delegate, long packedOriginBlockPos) {
		super(delegate, packedOriginBlockPos);
	}

	public static DrawableRegion pack(VertexCollectorList collectorList, TransferBuffer buffer, StaticDrawBuffer vboBuffer, boolean translucent, int byteCount, long packedOriginBlockPos) {
		final ObjectArrayList<ArrayVertexCollector> drawList = collectorList.sortedDrawList(translucent ? TRANSLUCENT : SOLID);

		if (drawList.isEmpty()) {
			return EMPTY_DRAWABLE;
		}

		final ArrayVertexCollector collector = drawList.get(0);

		// WIP: restore ability to have more than one pass in non-translucent terrain, for decals, etc.
		assert drawList.size() == 1;
		assert collector.renderState.sorted == translucent;

		collector.toBuffer(0, buffer, 0);

		final VboDrawableState delegate = new VboDrawableState(collector.quadCount() * 4, 0, vboBuffer);
		return new VboDrawableRegion(delegate, packedOriginBlockPos);
	}

	@Override
	protected void closeInner() {
		// NOOP
	}
}
