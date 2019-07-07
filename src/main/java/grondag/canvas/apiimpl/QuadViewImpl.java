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

package grondag.canvas.apiimpl;

import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_BITS;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_MATERIAL;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_TAG;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.NORMALS_OFFSET;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.SECOND_TEXTURE_OFFSET;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.TEXTURE_OFFSET_MINUS;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.TEXTURE_STRIDE;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.THIRD_TEXTURE_OFFSET;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.VANILLA_STRIDE;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.VERTEX_START_OFFSET;

import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.light.LightmapHd;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;

/**
 * Base class for all quads / quad makers. Handles the ugly bits of maintaining
 * and encoding the quad state.
 */
public class QuadViewImpl implements QuadView {
    protected int nominalFaceId = ModelHelper.NULL_FACE_ID;
    
    /**
     * indicate if vertex normal has been set - bits correspond to vertex ordinals
     */
    protected int normalFlags;
    protected int geometryFlags;
    protected boolean isGeometryInvalid = true;
    protected final Vector3f faceNormal = new Vector3f();
    protected int packedFaceNormal = -1;
    protected boolean isFaceNormalInvalid = true;

    /**
     * Size and where it comes from will vary in subtypes. But in all cases quad is
     * fully encoded to array.
     */
    protected int[] data;

    /** Beginning of the quad. Also the header index. */
    protected int baseIndex = 0;
    
    //UGLY
    public LightmapHd blockLight = null;
    public LightmapHd skyLight = null;
    public LightmapHd aoShade = null;
    public float[][] w = new float[4][4];
    public float[] u = new float[4];
    public float[] v = new float[4];

    /**
     * Use when subtype is "attached" to a pre-existing array. Sets data reference
     * and index and decodes state from array.
     */
    final void load(int[] data, int baseIndex) {
        this.data = data;
        this.baseIndex = baseIndex;
        load();
    }

    /**
     * Used on vanilla quads or other quads that don't have encoded shape info to
     * signal that such should be computed when requested.
     */
    public final void invalidateShape() {
        isFaceNormalInvalid = true;
        isGeometryInvalid = true;
        packedFaceNormal = -1;
    }

    /**
     * Like {@link #load(int[], int)} but assumes array and index already set. Only
     * does the decoding part.
     */
    public final void load() {
        // face normal isn't encoded but geometry flags are
        isFaceNormalInvalid = true;
        packedFaceNormal = -1;
        isGeometryInvalid = false;
        decodeHeader();
    }

    /**
     * Reference to underlying array. Use with caution. Meant for fast renderer
     * access
     */
    public int[] data() {
        return data;
    }

    /** True if any vertex normal has been set. */
    public boolean hasVertexNormals() {
        return normalFlags != 0;
    }

    /**
     * Index after header where vertex data starts (first 28 will be vanilla format.
     */
    public int vertexStart() {
        return baseIndex + VERTEX_START_OFFSET;
    }

    /** Length of encoded quad in array, including header. */
    final int stride() {
        return MeshEncodingHelper.stride(material().spriteDepth());
    }

    /** reads state from header - vertex attributes are saved directly */
    protected void decodeHeader() {
        final int bits = data[baseIndex + HEADER_BITS];
        geometryFlags = MeshEncodingHelper.geometryFlags(bits);
        nominalFaceId = this.lightFaceId();
        normalFlags = MeshEncodingHelper.normalFlags(bits);
    }

    /** writes state to header - vertex attributes are saved directly */
    protected void encodeHeader() {
        int bits = MeshEncodingHelper.geometryFlags(data[baseIndex + HEADER_BITS], geometryFlags);
        bits = MeshEncodingHelper.normalFlags(bits, normalFlags);
        data[baseIndex + HEADER_BITS] = bits;
    }

    /**
     * gets flags used for lighting - lazily computed via
     * {@link GeometryHelper#computeShapeFlags(QuadView)}
     */
    public int geometryFlags() {
        if (isGeometryInvalid) {
            isGeometryInvalid = false;
            geometryFlags = GeometryHelper.computeShapeFlags(this);
        }
        return geometryFlags;
    }

    /**
     * Used to override geometric analysis for compatibility edge case
     */
    public void geometryFlags(int flags) {
        isGeometryInvalid = false;
        geometryFlags = flags;
    }

    @Override
    public final void toVanilla(int textureIndex, int[] target, int targetIndex, boolean isItem) {
        System.arraycopy(data, vertexStart(), target, targetIndex, 28);

        if (textureIndex > 0) {
            copyColorUV(textureIndex, target, targetIndex);
        }

        if (isItem) {
            copyNormals(target, targetIndex);
        }
    }

    /**
     * Internal helper method. Copies color and UV for the given texture to target,
     * assuming vanilla format.
     */
    public final void copyColorUV(int textureIndex, int[] target, int targetIndex) {
        int indexTo = targetIndex + 3;
        int indexFrom;
        int strideFrom;
        if (textureIndex == 0) {
            indexFrom = baseIndex + VERTEX_START_OFFSET + 3;
            strideFrom = 7;
        } else {
            indexFrom = baseIndex + (textureIndex == 1 ? SECOND_TEXTURE_OFFSET : THIRD_TEXTURE_OFFSET);
            strideFrom = 3;
        }
        for (int i = 0; i < 4; i++) {
            System.arraycopy(data, indexFrom, target, indexTo, 3);
            indexTo += 7;
            indexFrom += strideFrom;
        }
    }

    /**
     * Internal helper method. Copies packed normals to target, assuming vanilla
     * format.
     */
    public final void copyNormals(int[] target, int targetIndex) {
        final int normalFlags = this.normalFlags;
        final int packedFaceNormal = normalFlags == 0b1111 ? 0 : NormalHelper.packNormal(faceNormal(), 0);
        final int normalsIndex = baseIndex + NORMALS_OFFSET;
        for (int v = 0; v < 4; v++) {
            final int packed = (normalFlags & (1 << v)) == 0 ? packedFaceNormal : data[normalsIndex + v];
            target[targetIndex + v * 7 + 6] = packed;
        }
    }

    @Override
    public final RenderMaterialImpl.Value material() {
        return RenderMaterialImpl.byIndex(data[baseIndex + HEADER_MATERIAL]);
    }

    @Override
    public final int colorIndex() {
        return data[baseIndex + HEADER_COLOR_INDEX];
    }

    @Override
    public final int tag() {
        return data[baseIndex + HEADER_TAG];
    }

    public final int lightFaceId() {
        return MeshEncodingHelper.lightFace(data[baseIndex + HEADER_BITS]);
    }
    
    @Override
    @Deprecated
    public final Direction lightFace() {
        return ModelHelper.faceFromIndex(lightFaceId());
    }

    public final int cullFaceId() {
        return MeshEncodingHelper.cullFace(data[baseIndex + HEADER_BITS]);
    }
    
    @Override
    @Deprecated
    public final Direction cullFace() {
        return ModelHelper.faceFromIndex(cullFaceId());
    }

    @Override
    @Deprecated
    public final Direction nominalFace() {
        return ModelHelper.faceFromIndex(nominalFaceId);
    }

    @Override
    public final Vector3f faceNormal() {
        if (isFaceNormalInvalid) {
            NormalHelper.computeFaceNormal(faceNormal, this);
            isFaceNormalInvalid = false;
        }
        return faceNormal;
    }

    public int packedFaceNormal() {
        int result = packedFaceNormal;
        if(result == -1) {
            result = NormalHelper.packNormal(this.faceNormal(), 0);
            packedFaceNormal = result;
        }
        return result;
    }
    
    @Override
    public void copyTo(MutableQuadView target) {
        MutableQuadViewImpl quad = (MutableQuadViewImpl) target;

        int len = Math.min(this.stride(), quad.stride());

        // copy everything except the header/material
        System.arraycopy(data, baseIndex + 1, quad.data, quad.baseIndex + 1, len - 1);
        quad.isFaceNormalInvalid = this.isFaceNormalInvalid;
        if (!this.isFaceNormalInvalid) {
            quad.faceNormal.set(this.faceNormal.x(), this.faceNormal.y(), this.faceNormal.z());
            quad.packedFaceNormal = this.packedFaceNormal;
        }
        quad.lightFace(lightFaceId());
        quad.colorIndex(colorIndex());
        quad.tag(tag());
        quad.cullFace(cullFaceId());
        quad.nominalFace(nominalFaceId);
        quad.normalFlags = this.normalFlags;
    }

    @Override
    public Vector3f copyPos(int vertexIndex, Vector3f target) {
        if (target == null) {
            target = new Vector3f();
        }
        final int index = vertexStart() + vertexIndex * 7;
        target.set(Float.intBitsToFloat(data[index]), Float.intBitsToFloat(data[index + 1]),
                Float.intBitsToFloat(data[index + 2]));
        return target;
    }

    @Override
    public float posByIndex(int vertexIndex, int coordinateIndex) {
        return Float.intBitsToFloat(data[vertexStart() + vertexIndex * 7 + coordinateIndex]);
    }

    @Override
    public float x(int vertexIndex) {
        return Float.intBitsToFloat(data[vertexStart() + vertexIndex * 7]);
    }

    @Override
    public float y(int vertexIndex) {
        return Float.intBitsToFloat(data[vertexStart() + vertexIndex * 7 + 1]);
    }

    @Override
    public float z(int vertexIndex) {
        return Float.intBitsToFloat(data[vertexStart() + vertexIndex * 7 + 2]);
    }

    @Override
    public boolean hasNormal(int vertexIndex) {
        return (normalFlags & (1 << vertexIndex)) != 0;
    }

    @Override
    public Vector3f copyNormal(int vertexIndex, Vector3f target) {
        if (hasNormal(vertexIndex)) {
            if (target == null) {
                target = new Vector3f();
            }
            final int normal = data[vertexStart() + VANILLA_STRIDE + vertexIndex];
            target.set(NormalHelper.getPackedNormalComponent(normal, 0),
                    NormalHelper.getPackedNormalComponent(normal, 1), NormalHelper.getPackedNormalComponent(normal, 2));
            return target;
        } else {
            return null;
        }
    }

    public int packedNormal(int vertexIndex) {
        return hasNormal(vertexIndex) ? data[baseIndex + VERTEX_START_OFFSET + VANILLA_STRIDE + vertexIndex] : packedFaceNormal();
    }
    
    @Override
    public float normalX(int vertexIndex) {
        return hasNormal(vertexIndex)
                ? NormalHelper.getPackedNormalComponent(
                        data[baseIndex + VERTEX_START_OFFSET + VANILLA_STRIDE + vertexIndex], 0)
                : Float.NaN;
    }

    @Override
    public float normalY(int vertexIndex) {
        return hasNormal(vertexIndex)
                ? NormalHelper.getPackedNormalComponent(
                        data[baseIndex + VERTEX_START_OFFSET + VANILLA_STRIDE + vertexIndex], 1)
                : Float.NaN;
    }

    @Override
    public float normalZ(int vertexIndex) {
        return hasNormal(vertexIndex)
                ? NormalHelper.getPackedNormalComponent(
                        data[baseIndex + VERTEX_START_OFFSET + VANILLA_STRIDE + vertexIndex], 2)
                : Float.NaN;
    }

    @Override
    public int lightmap(int vertexIndex) {
        return data[baseIndex + vertexIndex * 7 + 6 + VERTEX_START_OFFSET];
    }

    protected int colorIndex(int vertexIndex, int textureIndex) {
        return textureIndex == 0 ? vertexIndex * 7 + 3 + VERTEX_START_OFFSET
                : TEXTURE_OFFSET_MINUS + textureIndex * TEXTURE_STRIDE + vertexIndex * 3;
    }

    @Override
    public int spriteColor(int vertexIndex, int textureIndex) {
        return data[baseIndex + colorIndex(vertexIndex, textureIndex)];
    }

    @Override
    public float spriteU(int vertexIndex, int textureIndex) {
        return Float.intBitsToFloat(data[baseIndex + colorIndex(vertexIndex, textureIndex) + 1]);
    }

    @Override
    public float spriteV(int vertexIndex, int textureIndex) {
        return Float.intBitsToFloat(data[baseIndex + colorIndex(vertexIndex, textureIndex) + 2]);
    }
}
