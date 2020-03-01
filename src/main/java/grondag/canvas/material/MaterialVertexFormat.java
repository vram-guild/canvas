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

package grondag.canvas.material;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL20;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.packing.AbstractVertexCollector;
import grondag.canvas.varia.CanvasGlHelper;

public class MaterialVertexFormat {
	private static int nextIndex = 0;

	public final int attributeCount;

	/** vertex stride in bytes */
	public final int vertexStrideBytes;
	public final int vertexStrideInts;

	private final MaterialVertextFormatElement[] elements;

	public final int index = nextIndex++;

	MaterialVertexFormat(MaterialVertextFormatElement... elementsIn) {
		elements = elementsIn;

		int bytes = 0;
		int count = 0;
		for (final MaterialVertextFormatElement e : elements) {
			bytes += e.byteSize;
			if (e.attributeName != null) {
				count++;
			}
		}
		attributeCount = count;
		vertexStrideBytes = bytes;
		vertexStrideInts = bytes / 4;
	}

	public void encode(MutableQuadViewImpl q, OldVertexEncodingContext context, AbstractVertexCollector output) {
		final MaterialVertextFormatElement[] elements = this.elements;
		for(int i = 0; i < 4; i++) {
			for(final MaterialVertextFormatElement e : elements) {
				e.encoder.encode(q, i, context, output);
			}
		}
	}

	/**
	 * Enables generic vertex attributes and binds their location.
	 * For use with non-VAO VBOs
	 */
	public void enableAndBindAttributes(int bufferOffset) {
		final int attribCount = CanvasGlHelper.enableAttributes(attributeCount, false);
		bindAttributeLocations(bufferOffset, attribCount);
	}

	/**
	 * Enables generic vertex attributes and binds their location.
	 * For use with non-VBO buffers.
	 */
	public void enableAndBindAttributes(ByteBuffer buffer, int bufferOffset) {
		final int attribCount = CanvasGlHelper.enableAttributes(attributeCount, false);
		int offset = 0;
		int index = 1;
		final int limit = elements.length;
		// NB: <= because element 0 is vertex
		for(int i = 0; i <= attribCount; i++) {
			if(i < limit) {
				final MaterialVertextFormatElement e = elements[i];
				if (e.attributeName != null) {
					buffer.position(bufferOffset + offset);
					if(Configurator.logGlStateChanges) {
						CanvasMod.LOG.info(String.format("GlState: glVertexAttribPointer(%d, %d, %d, %b, %d, %s) [non-VBO]", index, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, buffer.toString()));
					}
					GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, buffer);
				}
				offset += e.byteSize;
			} else {
				// dummy attribute
				buffer.position(bufferOffset + 12);
				if(Configurator.logGlStateChanges) {
					CanvasMod.LOG.info(String.format("GlState: glVertexAttribPointer(%d, %d, %d, %b, %d, %s) [non-VBO, dummy]", index, 4, GL20.GL_BYTE, false, vertexStrideBytes, buffer.toString()));
				}
				GL20.glVertexAttribPointer(index++, 4, GL20.GL_BYTE, false, vertexStrideBytes, buffer);
			}
		}
	}

	/**
	 * Binds attribute locations without enabling them. For use with VAOs. In other
	 * cases just call {@link #enableAndBindAttributes(int)}
	 * @param attribCount How many attributes are currently enabled.  Any not in format should be bound to dummy index.
	 */
	public void bindAttributeLocations(int bufferOffset, int attribCount) {
		int offset = 0;
		int index = 1;
		final int limit = elements.length;
		// NB: <= because element 0 is vertex
		for(int i = 0; i <= attribCount; i++) {
			if(i < limit) {
				final MaterialVertextFormatElement e = elements[i];
				if (e.attributeName != null) {
					if(Configurator.logGlStateChanges) {
						CanvasMod.LOG.info(String.format("GlState: glVertexAttribPointer(%d, %d, %d, %b, %d, %d)", index, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset));
					}
					GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset);
				}
				offset += e.byteSize;
			} else {
				// dummy attribute
				if(Configurator.logGlStateChanges) {
					CanvasMod.LOG.info(String.format("GlState: glVertexAttribPointer(%d, %d, %d, %b, %d, %d) [dummy]", index, 4, GL20.GL_BYTE, false, vertexStrideBytes, bufferOffset + 12));
				}
				GL20.glVertexAttribPointer(index++, 4, GL20.GL_BYTE, false, vertexStrideBytes, bufferOffset + 12);
			}
		}
	}

	/**
	 * Used by shader to bind attribute names.
	 */
	public void bindProgramAttributes(int programID) {
		int index = 1;
		for (final MaterialVertextFormatElement e : elements) {
			if (e.attributeName != null) {
				GL20.glBindAttribLocation(programID, index++, e.attributeName);
			}
		}
	}
}
