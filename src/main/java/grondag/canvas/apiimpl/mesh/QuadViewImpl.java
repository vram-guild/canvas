/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.apiimpl.mesh;

import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.frex.api.mesh.QuadView;

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.BASE_QUAD_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.BASE_VERTEX_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_BITS;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_MATERIAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_SPRITE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_TAG;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_EXTRA_PRECISION;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_PRECISE_TO_FLOAT_CONVERSION;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_ROUNDING_BIT;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_COLOR;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_NORMAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_START;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_X;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Y;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Z;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

/**
 * Base class for all quads / quad makers. Handles the ugly bits
 * of maintaining and encoding the quad state.
 */
public class QuadViewImpl implements QuadView {
	protected final Vector3f faceNormal = new Vector3f();
	protected int nominalFaceId = ModelHelper.NULL_FACE_ID;
	protected boolean isGeometryInvalid = true;
	protected int packedFaceNormal = -1;

	/**
	 * flag true when sprite is assumed to be interpolated and need normalization
	 */
	protected boolean spriteMappedFlag = false;

	/**
	 * Size and where it comes from will vary in subtypes. But in all cases quad is fully encoded to array.
	 */
	protected int[] data;

	/**
	 * Beginning of the quad. Also the header index.
	 */
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
	 * Copies data from source, leaves baseIndex unchanged. For mesh iteration.
	 */
	public final void copyAndload(int[] source, int sourceIndex, int stride) {
		System.arraycopy(source, sourceIndex, data, baseIndex, stride);
		load();
	}

	/**
	 * Like {@link #load(int[], int)} but assumes array and index already set.
	 * Only does the decoding part.
	 */
	public final void load() {
		isGeometryInvalid = false;
		nominalFaceId = lightFaceId();
		// face normal isn't encoded
		NormalHelper.computeFaceNormal(faceNormal, this);
		packedFaceNormal = -1;
	}

	/**
	 * Reference to underlying array. Use with caution. Meant for fast renderer access
	 */
	public int[] data() {
		return data;
	}

	public int normalFlags() {
		return MeshEncodingHelper.normalFlags(data[baseIndex + HEADER_BITS]);
	}

	/**
	 * True if any vertex normal has been set.
	 */
	public boolean hasVertexNormals() {
		return normalFlags() != 0;
	}

	/**
	 * Index after header where vertex data starts (first 28 will be vanilla format.
	 */
	public int vertexStart() {
		return baseIndex + HEADER_STRIDE;
	}

	/**
	 * Length of encoded quad in array, including header.
	 */
	public final int stride() {
		return MeshEncodingHelper.stride();
	}

	/**
	 * gets flags used for lighting - lazily computed via {@link GeometryHelper#computeShapeFlags(QuadView)}.
	 */
	public int geometryFlags() {
		computeGeometry();
		return MeshEncodingHelper.geometryFlags(data[baseIndex + HEADER_BITS]);
	}

	protected void computeGeometry() {
		if (isGeometryInvalid) {
			isGeometryInvalid = false;

			NormalHelper.computeFaceNormal(faceNormal, this);
			packedFaceNormal = -1;

			final int headerIndex = baseIndex + HEADER_BITS;

			// depends on face normal
			// NB: important to save back to array because used by geometry helper
			data[headerIndex] = MeshEncodingHelper.lightFace(data[headerIndex], GeometryHelper.lightFaceId(this));

			// depends on light face
			data[baseIndex + HEADER_BITS] = MeshEncodingHelper.geometryFlags(data[headerIndex], GeometryHelper.computeShapeFlags(this));
		}
	}

	@Override
	public final void toVanilla(int[] target, int targetIndex) {
		System.arraycopy(data, baseIndex + VERTEX_START, target, targetIndex, BASE_QUAD_STRIDE);

		// Convert sprite data from fixed precision to float
		int index = targetIndex + 4;

		for (int i = 0; i < 4; ++i) {
			target[index] = Float.floatToRawIntBits(spriteU(i));
			target[index + 1] = Float.floatToRawIntBits(spriteV(i));
			index += 8;
		}
	}

	@Override
	public final RenderMaterialImpl material() {
		return RenderMaterialImpl.fromIndex(data[baseIndex + HEADER_MATERIAL]);
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
		computeGeometry();
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
		computeGeometry();
		return faceNormal;
	}

	public int packedFaceNormal() {
		computeGeometry();
		int result = packedFaceNormal;

		if (result == -1) {
			result = NormalHelper.packNormal(faceNormal);
			packedFaceNormal = result;
		}

		return result;
	}

	@Override
	public void copyTo(MutableQuadView targetIn) {
		final grondag.frex.api.mesh.MutableQuadView target = (grondag.frex.api.mesh.MutableQuadView) targetIn;

		// forces geometry compute
		final int packedNormal = packedFaceNormal();

		final MutableQuadViewImpl quad = (MutableQuadViewImpl) target;

		final int len = Math.min(stride(), quad.stride());

		// copy everything except the material
		System.arraycopy(data, baseIndex + 1, quad.data, quad.baseIndex + 1, len - 1);
		quad.spriteMappedFlag = spriteMappedFlag;
		quad.faceNormal.set(faceNormal.getX(), faceNormal.getY(), faceNormal.getZ());
		quad.packedFaceNormal = packedNormal;
		quad.nominalFaceId = nominalFaceId;
		quad.isGeometryInvalid = false;
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

	protected int colorOffset(int vertexIndex) {
		return vertexIndex * BASE_VERTEX_STRIDE + VERTEX_COLOR;
	}

	@Override
	public int vertexColor(int vertexIndex) {
		return data[baseIndex + colorOffset(vertexIndex)];
	}

	protected final boolean isSpriteUnmapped() {
		return !spriteMappedFlag;
	}

	protected final float spriteFloatU(int vertexIndex) {
		return data[baseIndex + colorOffset(vertexIndex) + 1] * UV_PRECISE_TO_FLOAT_CONVERSION;
	}

	protected final float spriteFloatV(int vertexIndex) {
		return data[baseIndex + colorOffset(vertexIndex) + 2] * UV_PRECISE_TO_FLOAT_CONVERSION;
	}

	@Override
	public float spriteU(int vertexIndex) {
		return isSpriteUnmapped()
		? SpriteInfoTexture.BLOCKS.mapU(spriteId(), spriteFloatU(vertexIndex))
		: spriteFloatU(vertexIndex);
	}

	@Override
	public float spriteV(int vertexIndex) {
		return isSpriteUnmapped()
		? SpriteInfoTexture.BLOCKS.mapV(spriteId(), spriteFloatV(vertexIndex))
		: spriteFloatV(vertexIndex);
	}

	/**
	 * Fixed precision value suitable for transformations
	 */
	public int spritePreciseU(int vertexIndex) {
		assert isSpriteUnmapped();
		return data[baseIndex + colorOffset(vertexIndex) + 1];
	}

	/**
	 * Fixed precision value suitable for transformations
	 */
	public int spritePreciseV(int vertexIndex) {
		assert isSpriteUnmapped();
		return data[baseIndex + colorOffset(vertexIndex) + 2];
	}

	/**
	 * Rounded, unsigned short value suitable for vertex buffer
	 */
	public int spriteBufferU(int vertexIndex) {
		assert isSpriteUnmapped();
		return (data[baseIndex + colorOffset(vertexIndex) + 1] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION;
	}

	/**
	 * Rounded, unsigned short value suitable for vertex buffer
	 */
	public int spriteBufferV(int vertexIndex) {
		assert isSpriteUnmapped();
		return (data[baseIndex + colorOffset(vertexIndex) + 2] + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION;
	}

	public int spriteId() {
		return data[baseIndex + HEADER_SPRITE];
	}

	public void transformAndAppend(final int vertexIndex, final Matrix4fExt matrix, final int[] appendData, final int targetIndex) {
		final int[] data = this.data;
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		final float x = Float.intBitsToFloat(data[index]);
		final float y = Float.intBitsToFloat(data[index + 1]);
		final float z = Float.intBitsToFloat(data[index + 2]);

		final float xOut = matrix.a00() * x + matrix.a01() * y + matrix.a02() * z + matrix.a03();
		final float yOut = matrix.a10() * x + matrix.a11() * y + matrix.a12() * z + matrix.a13();
		final float zOut = matrix.a20() * x + matrix.a21() * y + matrix.a22() * z + matrix.a23();

		appendData[targetIndex] = Float.floatToRawIntBits(xOut);
		appendData[targetIndex + 1] = Float.floatToRawIntBits(yOut);
		appendData[targetIndex + 2] = Float.floatToRawIntBits(zOut);
	}

	public void transformAndAppend(final int vertexIndex, final Matrix4fExt matrix, final VertexConsumer buff) {
		final int[] data = this.data;
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		final float x = Float.intBitsToFloat(data[index]);
		final float y = Float.intBitsToFloat(data[index + 1]);
		final float z = Float.intBitsToFloat(data[index + 2]);

		final float xOut = matrix.a00() * x + matrix.a01() * y + matrix.a02() * z + matrix.a03();
		final float yOut = matrix.a10() * x + matrix.a11() * y + matrix.a12() * z + matrix.a13();
		final float zOut = matrix.a20() * x + matrix.a21() * y + matrix.a22() * z + matrix.a23();

		buff.vertex(xOut, yOut, zOut);
	}

	public void transformAndAppend(final int vertexIndex, final Matrix4fExt matrix, final float[] out) {
		final int[] data = this.data;
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		final float x = Float.intBitsToFloat(data[index]);
		final float y = Float.intBitsToFloat(data[index + 1]);
		final float z = Float.intBitsToFloat(data[index + 2]);

		out[0] = matrix.a00() * x + matrix.a01() * y + matrix.a02() * z + matrix.a03();
		out[1] = matrix.a10() * x + matrix.a11() * y + matrix.a12() * z + matrix.a13();
		out[2] = matrix.a20() * x + matrix.a21() * y + matrix.a22() * z + matrix.a23();
	}
}
