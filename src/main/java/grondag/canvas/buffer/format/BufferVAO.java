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

import java.util.function.IntSupplier;

import grondag.canvas.varia.GFX;

public class BufferVAO {
	public final CanvasVertexFormat format;
	private final IntSupplier arrayIdSupplier;
	private final IntSupplier elementIdSupplier;

	/**
	 * VAO Buffer name if enabled and initialized.
	 */
	private int vaoBufferId = 0;

	public BufferVAO(CanvasVertexFormat format, IntSupplier arrayIdSupplier, IntSupplier elementIdSupplier) {
		this.format = format;
		this.arrayIdSupplier = arrayIdSupplier;
		this.elementIdSupplier = elementIdSupplier;
	}

	public void bind() {
		bind(0);
	}
	
	public final void bind(int offset) {
		if (vaoBufferId == 0) {
			vaoBufferId = GFX.genVertexArray();
			GFX.bindVertexArray(vaoBufferId);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, arrayIdSupplier.getAsInt());
			format.bindAttributeLocations(offset);
			GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, elementIdSupplier.getAsInt());
			GFX.bindVertexArray(0);
			GFX.bindBuffer(GFX.GL_ARRAY_BUFFER, 0);
			GFX.bindBuffer(GFX.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		GFX.bindVertexArray(vaoBufferId);
	}

	public final void shutdown() {
		if (vaoBufferId != 0) {
			GFX.deleteVertexArray(vaoBufferId);
			vaoBufferId = 0;
		}
	}
}
