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

package grondag.canvas.chunk;

import grondag.canvas.buffer.allocation.VboBuffer;
import grondag.canvas.buffer.packing.BufferPacker;
import grondag.canvas.buffer.packing.BufferPackingList;
import grondag.canvas.buffer.packing.VertexCollectorList;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.material.MaterialVertexFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class UploadableChunk {
	protected final ObjectArrayList<DrawableDelegate> delegates;
	protected final VboBuffer vboBuffer;

	/** Does not retain packing list reference */
	public UploadableChunk(BufferPackingList packingList, VertexCollectorList collectorList, MaterialVertexFormat format) {
		vboBuffer = new VboBuffer(packingList.totalBytes(), format);
		delegates = BufferPacker.pack(packingList, collectorList, vboBuffer);
	}

	/**
	 * Will be called from client thread - is where flush/unmap needs to happen.
	 */
	public DrawableChunk produceDrawable() {
		vboBuffer.upload();
		return new DrawableChunk(delegates, vboBuffer);
	}
}
