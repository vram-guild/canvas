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

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.varia.CanvasGlHelper;

public class CanvasVertexFormat {
	public final int attributeCount;

	/**
	 * Vertex stride in bytes.
	 */
	public final int vertexStrideBytes;
	public final int vertexStrideInts;
	private final CanvasVertexFormatElement[] elements;

	public CanvasVertexFormat(CanvasVertexFormatElement... elementsIn) {
		elements = elementsIn;

		int bytes = 0;
		int count = 0;

		for (final CanvasVertexFormatElement e : elements) {
			bytes += e.byteSize;

			if (e.attributeName != null) {
				count++;
			}
		}

		attributeCount = count;
		vertexStrideBytes = bytes;
		vertexStrideInts = bytes / 4;
	}

	/**
	 * Enables generic vertex attributes and binds their location.
	 * For use with non-VAO VBOs.
	 */
	public void enableAndBindAttributes(long bufferOffset) {
		CanvasGlHelper.enableAttributes(attributeCount);
		bindAttributeLocations(bufferOffset);
	}

	/**
	 * Enables generic vertex attributes and binds their location.
	 * For use with non-VBO buffers.
	 */
	public void enableDirect(long memPointer) {
		final int attributeCount = this.attributeCount;
		CanvasGlHelper.enableAttributes(attributeCount);
		int offset = 0;
		int index = 1;
		final int limit = elements.length;

		for (int i = 0; i < limit; i++) {
			final CanvasVertexFormatElement e = elements[i];

			if (e.attributeName == null) {
				assert i == 0 : "position element must be first";
				GlStateManager.vertexPointer(3, GL21.GL_FLOAT, vertexStrideBytes, memPointer);
				assert CanvasGlHelper.checkError();
				GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
				assert CanvasGlHelper.checkError();
			} else {
				if (Configurator.logGlStateChanges) {
					CanvasMod.LOG.info(String.format("GlState: glVertexAttribPointer(%d, %d, %d, %b, %d) [direct non-VBO]", index, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes));
				}

				GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, memPointer + offset);
				assert CanvasGlHelper.checkError();
			}

			offset += e.byteSize;
		}
	}

	public static void disableDirect() {
		CanvasGlHelper.enableAttributes(0);
		GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
	}

	/**
	 * Binds attribute locations without enabling them. For use with VAOs. In other
	 * cases just call {@link #enableAndBindAttributes(int)}
	 *
	 * @param attribCount How many attributes are currently enabled.  Any not in format should be bound to dummy index.
	 */
	public void bindAttributeLocations(long bufferOffset) {
		int offset = 0;
		int index = 1;
		final int limit = elements.length;
		final int attributeCount = this.attributeCount;

		// NB: <= because element 0 is vertex
		for (int i = 0; i <= attributeCount; i++) {
			if (i < limit) {
				final CanvasVertexFormatElement e = elements[i];

				if (e.attributeName != null) {
					if (Configurator.logGlStateChanges) {
						CanvasMod.LOG.info(String.format("GlState: glVertexAttribPointer(%d, %d, %d, %b, %d, %d)", index, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset));
					}

					GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset);
				}

				offset += e.byteSize;
			}
		}
	}

	/**
	 * Used by shader to bind attribute names.
	 */
	public void bindProgramAttributes(int programID) {
		int index = 1;

		for (final CanvasVertexFormatElement e : elements) {
			if (e.attributeName != null) {
				GL20.glBindAttribLocation(programID, index++, e.attributeName);
			}
		}
	}
}
