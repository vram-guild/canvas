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

package grondag.canvas.terrain.render;

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.MaterialVertexFormat;

public class UploadableChunk {
	protected final VboBuffer vboBuffer;
	protected final DrawableChunk drawable;

	public UploadableChunk(VertexCollectorList collectorList, MaterialVertexFormat format, boolean translucent, int bytes) {
		vboBuffer = new VboBuffer(bytes, format);
		drawable = DrawableChunk.pack(collectorList, vboBuffer, translucent);
	}

	private UploadableChunk() {
		vboBuffer = null;
		drawable = DrawableChunk.EMPTY_DRAWABLE;
	}

	/**
	 * Will be called from client thread - is where flush/unmap needs to happen.
	 */
	public DrawableChunk produceDrawable() {
		vboBuffer.upload();
		return drawable;
	}

	public static final UploadableChunk EMPTY_UPLOADABLE = new UploadableChunk() {
		@Override
		public DrawableChunk produceDrawable() {
			return DrawableChunk.EMPTY_DRAWABLE;
		}
	};
}
