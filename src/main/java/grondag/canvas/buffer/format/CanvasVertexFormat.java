/*
 * Copyright © Contributing Authors
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

import grondag.canvas.varia.GFX;

public class CanvasVertexFormat {
	/**
	 * Vertex stride in bytes.
	 */
	public final int vertexStrideBytes;
	public final int vertexStrideInts;
	public final int quadStrideInts;

	private final CanvasVertexFormatElement[] elements;

	public CanvasVertexFormat(CanvasVertexFormatElement... elementsIn) {
		elements = elementsIn;

		int bytes = 0;

		for (final CanvasVertexFormatElement e : elements) {
			bytes += e.byteSize;
		}

		vertexStrideBytes = bytes;
		vertexStrideInts = bytes / 4;
		quadStrideInts = vertexStrideInts * 4;
	}

	public void bindAttributeLocations(long bufferOffset) {
		int offset = 0;
		final int limit = elements.length;

		for (int i = 0; i < limit; i++) {
			final CanvasVertexFormatElement e = elements[i];
			GFX.enableVertexAttribArray(1 + i);

			if (e.isInteger) {
				GFX.nglVertexAttribIPointer(1 + i, e.elementCount, e.glConstant, vertexStrideBytes, bufferOffset + offset);
			} else {
				GFX.vertexAttribPointer(1 + i, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset);
			}

			offset += e.byteSize;
		}
	}

	/**
	 * Used by shader to bind attribute names.
	 */
	public void bindProgramAttributes(int programID) {
		final int limit = elements.length;

		for (int i = 0; i < limit; i++) {
			final CanvasVertexFormatElement e = elements[i];
			GFX.bindAttribLocation(programID, 1 + i, e.attributeName);
		}
	}

	public int attributeCount() {
		return elements.length;
	}
}
