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

package grondag.canvas.apiimpl.util;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

/**
 * Holds all the array offsets and bit-wise encoders/decoders for
 * packing/unpacking quad data in an array of integers. All of this is
 * implementation-specific - that's why it isn't a "helper" class.
 */
public abstract class MeshEncodingHelper {
    private MeshEncodingHelper() {
    }

    public static final int HEADER_MATERIAL = 0;
    public static final int HEADER_COLOR_INDEX = 1;
    public static final int HEADER_BITS = 2;
    public static final int HEADER_TAG = 3;
    public static final int HEADER_STRIDE = 4;

    // our internal format always include packed normals
    public static final int VERTEX_START_OFFSET = HEADER_STRIDE;
    public static final int VANILLA_STRIDE = 28;
    public static final int NORMALS_OFFSET = VERTEX_START_OFFSET + VANILLA_STRIDE;
    public static final int NORMALS_STRIDE = 4;
    // normals are followed by 0-2 sets of color/uv coordinates
    public static final int TEXTURE_STRIDE = 12;
    /**
     * is one tex stride less than the actual base, because when used tex index is
     * >= 1
     */
    public static final int TEXTURE_OFFSET_MINUS = NORMALS_OFFSET + NORMALS_STRIDE - TEXTURE_STRIDE;
    public static final int SECOND_TEXTURE_OFFSET = NORMALS_OFFSET + NORMALS_STRIDE;
    public static final int THIRD_TEXTURE_OFFSET = SECOND_TEXTURE_OFFSET + TEXTURE_STRIDE;
    public static final int MAX_STRIDE = HEADER_STRIDE + VANILLA_STRIDE + NORMALS_STRIDE
            + TEXTURE_STRIDE * (RenderMaterialImpl.MAX_SPRITE_DEPTH - 1);

    /** used for quick clearing of quad buffers */
    public static final int[] EMPTY = new int[MAX_STRIDE];

    private static final int DIRECTION_MASK = 7;
    private static final int CULL_SHIFT = 0;
    private static final int CULL_INVERSE_MASK = ~(DIRECTION_MASK << CULL_SHIFT);
    private static final int LIGHT_SHIFT = CULL_SHIFT + Integer.bitCount(DIRECTION_MASK);
    private static final int LIGHT_INVERSE_MASK = ~(DIRECTION_MASK << LIGHT_SHIFT);
    private static final int NORMALS_SHIFT = LIGHT_SHIFT + Integer.bitCount(DIRECTION_MASK);
    private static final int NORMALS_MASK = 0b1111;
    private static final int NORMALS_INVERSE_MASK = ~(NORMALS_MASK << NORMALS_SHIFT);
    private static final int GEOMETRY_SHIFT = NORMALS_SHIFT + Integer.bitCount(NORMALS_MASK);
    private static final int GEOMETRY_MASK = 0b111;
    private static final int GEOMETRY_INVERSE_MASK = ~(GEOMETRY_MASK << GEOMETRY_SHIFT);

    public static final int DEFAULT_HEADER_BITS;
    
    static {
        int defaultHeader = 0;
        defaultHeader = cullFace(defaultHeader, ModelHelper.NULL_FACE_ID);
        defaultHeader = lightFace(defaultHeader, ModelHelper.NULL_FACE_ID);
        DEFAULT_HEADER_BITS = defaultHeader;
    }
    public static int cullFace(int bits) {
        return (bits >> CULL_SHIFT) & DIRECTION_MASK;
    }

    public static int cullFace(int bits, int face) {
        return (bits & CULL_INVERSE_MASK) | (face << CULL_SHIFT);
    }

    public static int lightFace(int bits) {
        return (bits >> LIGHT_SHIFT) & DIRECTION_MASK;
    }

    public static int lightFace(int bits, int face) {
        return (bits & LIGHT_INVERSE_MASK) | (face << LIGHT_SHIFT);
    }

    public static int normalFlags(int bits) {
        return (bits >> NORMALS_SHIFT) & NORMALS_MASK;
    }

    public static int normalFlags(int bits, int normalFlags) {
        return (bits & NORMALS_INVERSE_MASK) | ((normalFlags & NORMALS_MASK) << NORMALS_SHIFT);
    }

    public static int stride(int textureDepth) {
        return SECOND_TEXTURE_OFFSET - TEXTURE_STRIDE + textureDepth * TEXTURE_STRIDE;
    }

    public static int geometryFlags(int bits) {
        return bits >> GEOMETRY_SHIFT;
    }

    public static int geometryFlags(int bits, int geometryFlags) {
        return (bits & GEOMETRY_INVERSE_MASK) | ((geometryFlags & GEOMETRY_MASK) << GEOMETRY_SHIFT);
    }
}
