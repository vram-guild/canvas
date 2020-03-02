/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.buffer.packing;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.allocation.AllocationProvider;
import grondag.canvas.chunk.draw.DelegateLists;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.material.MaterialState;
import grondag.canvas.material.MaterialVertexFormat;

public class BufferPacker {

	// PERF: stash in context or worker thread
	private static final ThreadLocal<BufferPacker> THREADLOCAL = ThreadLocal.withInitial(BufferPacker::new);

	ObjectArrayList<DrawableDelegate> delegates;
	VertexCollectorList collectorList;
	AllocationProvider allocator;

	private BufferPacker() {
		//private
	}

	/** Does not retain packing list reference */
	public static ObjectArrayList<DrawableDelegate> pack(BufferPackingList packingList, VertexCollectorList collectorList, AllocationProvider allocator) {
		final BufferPacker packer = THREADLOCAL.get();
		final ObjectArrayList<DrawableDelegate> result = DelegateLists.getReadyDelegateList();
		packer.delegates = result;
		packer.collectorList = collectorList;
		packer.allocator = allocator;
		packingList.forEach(packer);
		packer.delegates = null;
		packer.collectorList = null;
		packer.allocator = null;
		return result;
	}

	public void accept(MaterialState materialState, int vertexStart, int vertexCount) {
		final VertexCollectorImpl collector = collectorList.get(materialState);
		final MaterialVertexFormat format = collector.materialState().bufferFormat;
		final int stride = format.vertexStrideBytes;

		allocator.claimAllocation(vertexCount * stride, ref -> {
			final int byteOffset = ref.byteOffset();
			final int byteCount = ref.byteCount();
			final int intLength = byteCount / 4;

			final IntBuffer intBuffer = ref.intBuffer();
			intBuffer.position(byteOffset / 4);

			// FIX: don't think this logic would actual work with split buffers
			// because the start position in the collector is always the same.
			// Either simplify and assume a single buffer (wouldn't need this lambda)
			// or make it actually work.
			collector.toBuffer(intBuffer, vertexStart * stride / 4, intLength);

			delegates.add(DrawableDelegate.claim(ref, materialState, byteCount / stride, format));
		});
	}
}
