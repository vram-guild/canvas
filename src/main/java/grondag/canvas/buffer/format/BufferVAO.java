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

package grondag.canvas.buffer.format;

import grondag.canvas.varia.GFX;

public class BufferVAO {
	public final CanvasVertexFormat format;
	private final Runnable bufferBinding;

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = 0;

	public BufferVAO(CanvasVertexFormat format, Runnable bufferBinding) {
		this.format = format;
		this.bufferBinding = bufferBinding;
	}

	public void bind() {
		bind(0);
	}
	
	public final void bind(int offset) {
		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);
			bufferBinding.run();
			format.enableAttributes();
			format.bindAttributeLocations(offset);
		} else {
			GFX.bindVertexArray(vaoBufferId);
		}
	}

	public final void shutdown() {
		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}
	}
}
