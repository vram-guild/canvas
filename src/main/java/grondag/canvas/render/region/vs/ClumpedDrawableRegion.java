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

package grondag.canvas.render.region.vs;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.buffer.encoding.ArrayVertexCollector;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.base.AbstractDrawableRegion;

public class ClumpedDrawableRegion extends AbstractDrawableRegion<ClumpedDrawableState> {
	private ClumpedDrawableRegion(ClumpedDrawableState delegate, long packedOriginBlockPos) {
		super(delegate, packedOriginBlockPos);
	}

	static DrawableRegion pack(VertexCollectorList collectorList, boolean translucent, int byteCount, long packedOriginBlockPos) {
		final ObjectArrayList<ArrayVertexCollector> drawList = collectorList.sortedDrawList(translucent ? TRANSLUCENT : SOLID);

		if (drawList.isEmpty()) {
			return EMPTY_DRAWABLE;
		}

		final ArrayVertexCollector collector = drawList.get(0);

		// WIP: restore ability to have more than one pass in non-translucent terrain, for decals, etc.
		// Note that every render state/pass will have a separate storage and storage will control
		// the vertex offset for each.  The calls won't be batched by region so there's no advantage to
		// making them adjacent in storage and smaller allocations may be easier to manage for storage.
		assert drawList.size() == 1;
		assert collector.renderState.sorted == translucent;

		// WIP: Try having orphaned, memory-mapped GPU slabs for loading off-thread and then do transfers with copySubBuffer
		final ByteBuffer transferBuffer = TransferBufferAllocator.claim(byteCount);
		final IntBuffer intBuffer = transferBuffer.asIntBuffer();
		intBuffer.position(0);
		collector.toBuffer(intBuffer, 0);
		ClumpedDrawableStorage storage = new ClumpedDrawableStorage(transferBuffer, byteCount, packedOriginBlockPos);

		final ClumpedDrawableState drawState = new ClumpedDrawableState(collector.renderState, collector.quadCount() * 4, storage);
		return new ClumpedDrawableRegion(drawState, packedOriginBlockPos);
	}

	@Override
	protected void closeInner() {
		// NOOP
	}
}
