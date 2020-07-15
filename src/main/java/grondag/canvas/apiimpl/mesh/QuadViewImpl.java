/*******************************************************************************
 * Copyright 2019, 2020 grondag
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


package grondag.canvas.apiimpl.mesh;

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.BASE_QUAD_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.BASE_VERTEX_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_BITS;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_MATERIAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_SPRITE_LOW;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_TAG;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.TEXTURE_OFFSET_MINUS;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.TEXTURE_QUAD_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.TEXTURE_VERTEX_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_COLOR;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_NORMAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_START;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_X;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Y;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Z;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.texture.SpriteInfoTexture;

/**
 * Base class for all quads / quad makers. Handles the ugly bits
 * of maintaining and encoding the quad state.
 */
public class QuadViewImpl implements QuadView {
	protected int nominalFaceId = ModelHelper.NULL_FACE_ID;
	protected boolean isGeometryInvalid = true;
	protected final Vector3f faceNormal = new Vector3f();
	protected int packedFaceNormal = -1;
	protected boolean isFaceNormalInvalid = true;

	/** flag true when sprite is assumed to be interpolated and need normalization */
	protected int spriteInterpolationFlags = 0;

	/** Size and where it comes from will vary in subtypes. But in all cases quad is fully encoded to array. */
	protected int[] data;

	/** Beginning of the quad. Also the header index. */
	protected int baseIndex = 0;

	/**
	 * Use when subtype is "attached" to a pre-existing array.
	 * Sets data reference and index and decodes state from array.
	 */
	final void load(int[] data, int baseIndex) {
		this.data = data;
		this.baseIndex = baseIndex;
		load();
	}

	/**
	 * Used on vanilla quads or other quads that don't have encoded shape info
	 * to signal that such should be computed when requested.
	 */
	public final void invalidateShape() {
		isFaceNormalInvalid = true;
		isGeometryInvalid = true;
		packedFaceNormal = -1;
	}

	/**
	 * Like {@link #load(int[], int)} but assumes array and index already set.
	 * Only does the decoding part.
	 */
	public final void load() {
		// face normal isn't encoded but geometry flags are
		isFaceNormalInvalid = true;
		packedFaceNormal = -1;
		isGeometryInvalid = false;
		nominalFaceId = lightFace().ordinal();
	}

	/** Reference to underlying array. Use with caution. Meant for fast renderer access */
	public int[] data() {
		return data;
	}

	public int normalFlags() {
		return MeshEncodingHelper.normalFlags(data[baseIndex + HEADER_BITS]);
	}

	/** True if any vertex normal has been set. */
	public boolean hasVertexNormals() {
		return normalFlags() != 0;
	}

	/**
	 * Index after header where vertex data starts (first 28 will be vanilla format.
	 */
	public int vertexStart() {
		return baseIndex + HEADER_STRIDE;
	}

	/** Length of encoded quad in array, including header. */
	public final int stride() {
		return MeshEncodingHelper.stride(material().spriteDepth());
	}

	/** gets flags used for lighting - lazily computed via {@link GeometryHelper#computeShapeFlags(QuadView)}. */
	public int geometryFlags() {
		if (isGeometryInvalid) {
			isGeometryInvalid = false;
			final int result = GeometryHelper.computeShapeFlags(this);
			data[baseIndex + HEADER_BITS] = MeshEncodingHelper.geometryFlags(data[baseIndex + HEADER_BITS], result);
			return result;
		} else {
			return MeshEncodingHelper.geometryFlags(data[baseIndex + HEADER_BITS]);
		}
	}

	/**
	 * Used to override geometric analysis for compatibility edge case.
	 */
	public void geometryFlags(int flags) {
		isGeometryInvalid = false;
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.geometryFlags(data[baseIndex + HEADER_BITS], flags);
	}

	@Override
	public final void toVanilla(int textureIndex, int[] target, int targetIndex, boolean isItem) {
		System.arraycopy(data, baseIndex + VERTEX_START, target, targetIndex, BASE_QUAD_STRIDE);

		if (isSpriteNormalized(0)) {
			int index = targetIndex + 4;

			for (int i = 0; i < 4; ++i)  {
				target[index] = Float.floatToRawIntBits(spriteU(i, 0));
				target[index + 1] = Float.floatToRawIntBits(spriteV(i, 0));
				index += 8;
			}
		}
	}

	@Override
	public final RenderMaterialImpl.CompositeMaterial material() {
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
			result = NormalHelper.packNormal(faceNormal(), 0);
			packedFaceNormal = result;
		}
		return result;
	}

	@Override
	public void copyTo(MutableQuadView target) {
		final MutableQuadViewImpl quad = (MutableQuadViewImpl) target;

		final int len = Math.min(stride(), quad.stride());

		// copy everything except the header/material
		System.arraycopy(data, baseIndex + 1, quad.data, quad.baseIndex + 1, len - 1);
		quad.spriteInterpolationFlags = spriteInterpolationFlags;

		quad.isFaceNormalInvalid = isFaceNormalInvalid;

		if (!isFaceNormalInvalid) {
			quad.faceNormal.set(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
			quad.packedFaceNormal = packedFaceNormal;
		}

		quad.lightFace(lightFaceId());
		quad.colorIndex(colorIndex());
		quad.tag(tag());
		quad.cullFace(cullFaceId());
		quad.nominalFaceId = nominalFaceId;
		quad.normalFlags(normalFlags());
	}

	@Override
	public Vector3f copyPos(int vertexIndex, Vector3f target) {
		if (target == null) {
			target = new Vector3f();
		}

		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		target.set(Float.intBitsToFloat(data[index]), Float.intBitsToFloat(data[index + 1]), Float.intBitsToFloat(data[index + 2]));
		return target;
	}

	@Override
	public float posByIndex(int vertexIndex, int coordinateIndex) {
		return Float.intBitsToFloat(data[baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X + coordinateIndex]);
	}

	@Override
	public float x(int vertexIndex) {
		return Float.intBitsToFloat(data[baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X]);
	}

	@Override
	public float y(int vertexIndex) {
		return Float.intBitsToFloat(data[baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_Y]);
	}

	@Override
	public float z(int vertexIndex) {
		return Float.intBitsToFloat(data[baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_Z]);
	}

	@Override
	public boolean hasNormal(int vertexIndex) {
		return (normalFlags() & (1 << vertexIndex)) != 0;
	}

	protected final int normalIndex(int vertexIndex) {
		return baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_NORMAL;
	}

	@Override
	public Vector3f copyNormal(int vertexIndex, Vector3f target) {
		if (hasNormal(vertexIndex)) {
			if (target == null) {
				target = new Vector3f();
			}

			final int normal = data[normalIndex(vertexIndex)];
			target.set(NormalHelper.getPackedNormalComponent(normal, 0), NormalHelper.getPackedNormalComponent(normal, 1), NormalHelper.getPackedNormalComponent(normal, 2));
			return target;
		} else {
			return null;
		}
	}

	public int packedNormal(int vertexIndex) {
		return hasNormal(vertexIndex) ? data[normalIndex(vertexIndex)] : packedFaceNormal();
	}

	@Override
	public float normalX(int vertexIndex) {
		return hasNormal(vertexIndex) ? NormalHelper.getPackedNormalComponent(data[normalIndex(vertexIndex)], 0) : Float.NaN;
	}

	@Override
	public float normalY(int vertexIndex) {
		return hasNormal(vertexIndex) ? NormalHelper.getPackedNormalComponent(data[normalIndex(vertexIndex)], 1) : Float.NaN;
	}

	@Override
	public float normalZ(int vertexIndex) {
		return hasNormal(vertexIndex) ? NormalHelper.getPackedNormalComponent(data[normalIndex(vertexIndex)], 2) : Float.NaN;
	}

	@Override
	public int lightmap(int vertexIndex) {
		return data[baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_LIGHTMAP];
	}

	protected int colorOffset(int vertexIndex, int spriteIndex) {
		return spriteIndex == 0 ? vertexIndex * BASE_VERTEX_STRIDE + VERTEX_COLOR
				: TEXTURE_OFFSET_MINUS + spriteIndex * TEXTURE_QUAD_STRIDE + vertexIndex * TEXTURE_VERTEX_STRIDE;
	}

	@Override
	public int spriteColor(int vertexIndex, int spriteIndex) {
		return data[baseIndex + colorOffset(vertexIndex, spriteIndex)];
	}

	protected final boolean isSpriteNormalized(int spriteIndex) {
		return (spriteInterpolationFlags & (1 << spriteIndex)) == 0;
	}

	public final float spriteRawU(int vertexIndex, int spriteIndex) {
		return Float.intBitsToFloat(data[baseIndex + colorOffset(vertexIndex, spriteIndex) + 1]);
	}

	public final float spriteRawV(int vertexIndex, int spriteIndex) {
		return Float.intBitsToFloat(data[baseIndex + colorOffset(vertexIndex, spriteIndex) + 2]);
	}

	@Override
	public float spriteU(int vertexIndex, int spriteIndex) {
		return isSpriteNormalized(spriteIndex)
				? SpriteInfoTexture.instance().denormalizeU(spriteId(spriteIndex), spriteRawU(vertexIndex, spriteIndex))
						: spriteRawU(vertexIndex, spriteIndex);
	}

	@Override
	public float spriteV(int vertexIndex, int spriteIndex) {
		return isSpriteNormalized(spriteIndex)
				? SpriteInfoTexture.instance().denormalizeV(spriteId(spriteIndex), spriteRawV(vertexIndex, spriteIndex))
						: spriteRawV(vertexIndex, spriteIndex);
	}

	// PERF: store as fixed point value and return as bitwise-rounded short
	public float spriteNormalizedU(int vertexIndex, int spriteIndex) {
		return isSpriteNormalized(spriteIndex)
				? spriteRawU(vertexIndex, spriteIndex) :
					SpriteInfoTexture.instance().denormalizeU(spriteId(spriteIndex), spriteRawU(vertexIndex, spriteIndex));
	}

	// PERF: store as fixed point value and return as bitwise-rounded short
	public float spriteNormalizedV(int vertexIndex, int spriteIndex) {
		return isSpriteNormalized(spriteIndex)
				? spriteRawV(vertexIndex, spriteIndex) :
					SpriteInfoTexture.instance().denormalizeV(spriteId(spriteIndex), spriteRawV(vertexIndex, spriteIndex));
	}

	protected  int spriteIdOffset(int spriteIndex) {
		return baseIndex + HEADER_SPRITE_LOW + (spriteIndex >> 1);
	}

	public int spriteId(int spriteIndex) {
		return (spriteIndex & 1) == 0 ? data[spriteIdOffset(spriteIndex)] & 0xFFFF : (data[spriteIdOffset(spriteIndex)] >> 16) & 0xFFFF;
	}
}
