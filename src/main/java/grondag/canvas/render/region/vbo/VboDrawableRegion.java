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

package grondag.canvas.render.region.vbo;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.StaticDrawBuffer;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.base.AbstractDrawableRegion;

public class VboDrawableRegion extends AbstractDrawableRegion<VboDrawableState> {
	private VboDrawableRegion(VboDrawableState delegate, long packedOriginBlockPos) {
		super(delegate, packedOriginBlockPos);
	}

	public static DrawableRegion pack(VertexCollectorList collectorList, StaticDrawBuffer vboBuffer, boolean translucent, int byteCount, long packedOriginBlockPos) {
		final ObjectArrayList<ArrayVertexCollector> drawList = collectorList.sortedDrawList(translucent ? TRANSLUCENT : SOLID);

		if (drawList.isEmpty()) {
			return EMPTY_DRAWABLE;
		}

		final ArrayVertexCollector collector = drawList.get(0);

		// WIP: restore ability to have more than one pass in non-translucent terrain, for decals, etc.
		assert drawList.size() == 1;
		assert collector.renderState.sorted == translucent;

		final IntBuffer intBuffer = vboBuffer.intBuffer();
		intBuffer.position(0);
		collector.toBuffer(intBuffer, 0);

		final VboDrawableState delegate = new VboDrawableState(collector.renderState, collector.quadCount() * 4, 0, vboBuffer);
		return new VboDrawableRegion(delegate, packedOriginBlockPos);
	}

	@Override
	protected void closeInner() {
		// NOOP
	}
}
