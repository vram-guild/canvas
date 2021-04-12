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

	public void enableAttributes() {
		final int limit = elements.length;

		for (int i = 0; i < limit; i++) {
			GFX.enableVertexAttribArray(i);
		}
	}

	public void disableAttributes() {
		final int limit = elements.length;

		for (int i = 0; i < limit; i++) {
			GFX.disableVertexAttribArray(i);
		}
	}

	public void bindAttributeLocations(long bufferOffset) {
		int offset = 0;
		final int limit = elements.length;

		for (int i = 0; i < limit; i++) {
			final CanvasVertexFormatElement e = elements[i];

			if (e.isInteger) {
				GFX.nglVertexAttribIPointer(i, e.elementCount, e.glConstant, vertexStrideBytes, bufferOffset + offset);
			} else {
				GFX.vertexAttribPointer(i, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset);
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
			GFX.bindAttribLocation(programID, i, e.attributeName);
		}
	}

	public int attributeCount() {
		return elements.length;
	}
}
