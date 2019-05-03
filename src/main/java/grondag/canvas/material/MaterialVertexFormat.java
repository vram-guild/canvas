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
import java.util.Collection;

import org.lwjgl.opengl.GL20;

import grondag.canvas.varia.CanvasGlHelper;

public class MaterialVertexFormat {

    public final int attributeCount;
    
    /** vertex stride in bytes */
    public final int vertexStrideBytes;

    private final MaterialVertextFormatElement[] elements;

    public MaterialVertexFormat(Collection<MaterialVertextFormatElement> elementsIn) {
        elements = new MaterialVertextFormatElement[elementsIn.size()];
        elementsIn.toArray(elements);
        
        int bytes = 0;
        int count = 0;
        for (MaterialVertextFormatElement e : elements) {
            bytes += e.byteSize;
            if (e.attributeName != null)
                count++;
        }
        this.attributeCount = count;
        this.vertexStrideBytes = bytes;
    }

    /**
     * Enables generic vertex attributes and binds their location.
     * For use with non-VAO VBOs
     */
    public void enableAndBindAttributes(int bufferOffset) {
        CanvasGlHelper.enableAttributes(this.attributeCount);
        bindAttributeLocations(bufferOffset);
    }

    /**
     * Enables generic vertex attributes and binds their location.
     * For use with non-VBO buffers.
     */
    public void enableAndBindAttributes(ByteBuffer buffer, int bufferOffset) {
        CanvasGlHelper.enableAttributes(this.attributeCount);
        int offset = 0;
        int index = 1;
        for (MaterialVertextFormatElement e : elements) {
            if (e.attributeName != null) {
                buffer.position(bufferOffset + offset);
                GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, buffer);
            }
            offset += e.byteSize;
        }
    }
    
    /**
     * Binds attribute locations without enabling them. For use with VAOs. In other
     * cases just call {@link #enableAndBindAttributes(int)}
     */
    public void bindAttributeLocations(int bufferOffset) {
        int offset = 0;
        int index = 1;
        for (MaterialVertextFormatElement e : elements) {
            if (e.attributeName != null) {
                GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, vertexStrideBytes, bufferOffset + offset);
            }
            offset += e.byteSize;
        }
    }

    /**
     * Used by shader to bind attribute names.
     */
    public void bindProgramAttributes(int programID) {
        int index = 1;
        for (MaterialVertextFormatElement e : elements) {
            if (e.attributeName != null) {
                GL20.glBindAttribLocation(programID, index++, e.attributeName);
            }
        }
    }
}
