/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
