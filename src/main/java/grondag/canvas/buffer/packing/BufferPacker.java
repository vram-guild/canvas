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

import grondag.canvas.buffer.allocation.VboBuffer;
import grondag.canvas.chunk.draw.DelegateLists;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.material.MaterialState;

public class BufferPacker {
	private BufferPacker() {
		//private
	}

	/** Does not retain packing list reference */
	public static ObjectArrayList<DrawableDelegate> pack(BufferPackingList packingList, VertexCollectorList collectorList, VboBuffer buffer) {
		final ObjectArrayList<DrawableDelegate> result = DelegateLists.getReadyDelegateList();
		final int size = packingList.size();
		final IntBuffer intBuffer = buffer.intBuffer();
		final int stride =  buffer.format.vertexStrideInts;
		int position = 0;

		intBuffer.position(0);

		for (int i = 0; i < size; i++) {
			final MaterialState materialState = packingList.getMaterialState(i);

			final VertexCollectorImpl collector = collectorList.getIfExists(materialState);
			assert collector != null;

			final int vertexCount = packingList.getCount(i);

			//			intBuffer.position(vertexStart * stride);
			collector.toBuffer(intBuffer, vertexCount * stride);

			result.add(DrawableDelegate.claim(materialState, position, vertexCount));
			position += vertexCount;
		}

		return result;
	}
}
