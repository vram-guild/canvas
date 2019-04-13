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

import net.minecraft.client.render.VertexFormatElement;

public class MaterialVertextFormatElement extends VertexFormatElement {
    // openGL implementation on my dev laptop *really* wants to get vertex positions
    // via standard (GL 2.1) binding
    // slows to a crawl otherwise
    public static final MaterialVertextFormatElement POSITION_3F = new MaterialVertextFormatElement(0,
            VertexFormatElement.Format.FLOAT, VertexFormatElement.Type.POSITION, 3, null);
    public static final MaterialVertextFormatElement BASE_RGBA_4UB = new MaterialVertextFormatElement(0,
            VertexFormatElement.Format.UNSIGNED_BYTE, VertexFormatElement.Type.PADDING, 4, "in_color_0");
    public static final MaterialVertextFormatElement BASE_TEX_2F = new MaterialVertextFormatElement(1,
            VertexFormatElement.Format.FLOAT, VertexFormatElement.Type.PADDING, 2, "in_uv_0");

    /**
     * Format varies by model.<p>
     * 
     * In vanilla lighting model, Bytes 1-2 are sky and block lightmap
     * coordinates. 3rd and 4th bytes are control flags. <p>
     */
    public static final MaterialVertextFormatElement LIGHTMAPS_4UB = new MaterialVertextFormatElement(2,
            VertexFormatElement.Format.UNSIGNED_BYTE, VertexFormatElement.Type.PADDING, 4, "in_lightmap", false);

    public static final MaterialVertextFormatElement NORMAL_AO_4UB = new MaterialVertextFormatElement(3,
            VertexFormatElement.Format.BYTE, VertexFormatElement.Type.PADDING, 4, "in_normal_ao", true);

    public static final MaterialVertextFormatElement SECONDARY_RGBA_4UB = new MaterialVertextFormatElement(4,
            VertexFormatElement.Format.UNSIGNED_BYTE, VertexFormatElement.Type.PADDING, 4, "in_color_1");
    public static final MaterialVertextFormatElement SECONDARY_TEX_2F = new MaterialVertextFormatElement(5,
            VertexFormatElement.Format.FLOAT, VertexFormatElement.Type.PADDING, 2, "in_uv_1");

    public static final MaterialVertextFormatElement TERTIARY_RGBA_4UB = new MaterialVertextFormatElement(6,
            VertexFormatElement.Format.UNSIGNED_BYTE, VertexFormatElement.Type.PADDING, 4, "in_color_2");
    public static final MaterialVertextFormatElement TERTIARY_TEX_2F = new MaterialVertextFormatElement(7,
            VertexFormatElement.Format.FLOAT, VertexFormatElement.Type.PADDING, 2, "in_uv_2");

    public final String attributeName;
    public final int elementCount;
    public final int glConstant;
    public final boolean isNormalized;
    public final int byteSize;

    private MaterialVertextFormatElement(int indexIn, VertexFormatElement.Format formatIn,
            VertexFormatElement.Type usageIn, int count, String attributeName) {
        this(indexIn, formatIn, usageIn, count, attributeName, true);
    }

    private MaterialVertextFormatElement(int indexIn, VertexFormatElement.Format formatIn,
            VertexFormatElement.Type usageIn, int count, String attributeName, boolean isNormalized) {
        super(indexIn, formatIn, usageIn, count);
        this.attributeName = attributeName;
        this.elementCount = this.getCount();
        this.glConstant = this.getFormat().getGlId();
        this.byteSize = this.getSize();
        this.isNormalized = isNormalized;
    }
}
