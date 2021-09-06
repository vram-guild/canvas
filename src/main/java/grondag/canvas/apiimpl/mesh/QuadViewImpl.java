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

package grondag.canvas.apiimpl.mesh;

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_BITS;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_FACE_NORMAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_FACE_TANGENT;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_MATERIAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_SPRITE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_TAG;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.MESH_QUAD_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE_SHIFT;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_EXTRA_PRECISION;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_PRECISE_TO_FLOAT_CONVERSION;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_ROUNDING_BIT;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_COLOR0;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP0;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_NORMAL0;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_START;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_U0;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_V0;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_X0;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Y0;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_Z0;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.frex.api.mesh.QuadView;

/**
 * Base class for all quads / quad makers. Handles the ugly bits
 * of maintaining and encoding the quad state.
 */
public class QuadViewImpl implements QuadView {
	protected static ThreadLocal<Vec3f> FACE_NORMAL_THREADLOCAL = ThreadLocal.withInitial(Vec3f::new);
	protected int nominalFaceId = ModelHelper.NULL_FACE_ID;
	protected boolean isGeometryInvalid = true;
	protected boolean isTangentInvalid = true;

	/**
	 * Flag true when sprite is assumed to be interpolated and need normalization.
	 */
	protected boolean isSpriteInterpolated = false;

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
	public final void copyAndLoad(int[] source, int sourceIndex, int stride) {
		System.arraycopy(source, sourceIndex, data, baseIndex, stride);
		load();
	}

	/**
	 * Like {@link #load(int[], int)} but assumes array and index already set.
	 * Only does the decoding part.
	 */
	public final void load() {
		isGeometryInvalid = false;
		isTangentInvalid = false;
		nominalFaceId = lightFaceId();
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

	public int tangentFlags() {
		return MeshEncodingHelper.tangentFlags(data[baseIndex + HEADER_BITS]);
	}

	/**
	 * True if any vertex tangents has been set.
	 */
	public boolean hasVertexTangents() {
		return tangentFlags() != 0;
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
			data[baseIndex + HEADER_FACE_NORMAL] = NormalHelper.computePackedFaceNormal(this);

			final int flagsIndex = baseIndex + HEADER_BITS;

			// depends on face normal
			// NB: important to save back to array because used by geometry helper
			data[flagsIndex] = MeshEncodingHelper.lightFace(data[flagsIndex], GeometryHelper.lightFaceId(this));

			// depends on light face
			data[flagsIndex] = MeshEncodingHelper.geometryFlags(data[flagsIndex], GeometryHelper.computeShapeFlags(this));
		}
	}

	@Override
	public final void toVanilla(int[] target, int targetIndex) {
		System.arraycopy(data, baseIndex + VERTEX_START, target, targetIndex, MESH_QUAD_STRIDE);

		// Convert sprite data from fixed precision to float
		int index = targetIndex + 4;

		for (int i = 0; i < 4; ++i) {
			target[index] = Float.floatToRawIntBits(spriteU(i));
			target[index + 1] = Float.floatToRawIntBits(spriteV(i));
			index += 8;
		}
	}

	// PERF: cache this
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

	/**
	 * DO NOT USE INTERNALLY - SLOWER THAN PACKED VALUES.
	 */
	@Deprecated
	@Override
	public final Vec3f faceNormal() {
		return NormalHelper.unpackNormalTo(packedFaceNormal(), FACE_NORMAL_THREADLOCAL.get());
	}

	public int packedFaceNormal() {
		computeGeometry();
		return data[baseIndex + HEADER_FACE_NORMAL];
	}

	private int computePackedFaceTangent() {
		final float v1 = spriteFloatV(1);
		final float dv0 = v1 - spriteFloatV(0);
		final float dv1 = spriteFloatV(2) - v1;
		final float u1 = spriteFloatU(1);
		final float inverseLength = 1.0f / ((u1 - spriteFloatU(0)) * dv1 - (spriteFloatU(2) - u1) * dv0);

		final float x1 = x(1);
		final float y1 = y(1);
		final float z1 = z(1);

		final float tx = inverseLength * (dv1 * (x1 - x(0)) - dv0 * (x(2) - x1));
		final float ty = inverseLength * (dv1 * (y1 - y(0)) - dv0 * (y(2) - y1));
		final float tz = inverseLength * (dv1 * (z1 - z(0)) - dv0 * (z(2) - z1));

		return NormalHelper.packNormal(tx, ty, tz);
	}

	public int packedFaceTanget() {
		if (isGeometryInvalid) {
			isTangentInvalid = true;
			computeGeometry();
		}

		if (isTangentInvalid) {
			isTangentInvalid = false;
			final int result = computePackedFaceTangent();
			data[baseIndex + HEADER_FACE_TANGENT] = result;
			return result;
		} else {
			return data[baseIndex + HEADER_FACE_TANGENT];
		}
	}

	@Override
	public void copyTo(MutableQuadView targetIn) {
		final grondag.frex.api.mesh.MutableQuadView target = (grondag.frex.api.mesh.MutableQuadView) targetIn;

		// force geometry compute
		computeGeometry();
		// force tangent compute
		this.packedFaceTanget();

		final MutableQuadViewImpl quad = (MutableQuadViewImpl) target;

		final int len = Math.min(stride(), quad.stride());

		// copy everything except the material
		System.arraycopy(data, baseIndex + 1, quad.data, quad.baseIndex + 1, len - 1);
		quad.isSpriteInterpolated = isSpriteInterpolated;
		quad.nominalFaceId = nominalFaceId;
		quad.isGeometryInvalid = false;
		quad.isTangentInvalid = false;
	}

	@Override
	public Vec3f copyPos(int vertexIndex, Vec3f target) {
		if (target == null) {
			target = new Vec3f();
		}

		final int index = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_X0;
		target.set(Float.intBitsToFloat(data[index]), Float.intBitsToFloat(data[index + 1]), Float.intBitsToFloat(data[index + 2]));
		return target;
	}

	@Override
	public float posByIndex(int vertexIndex, int coordinateIndex) {
		return Float.intBitsToFloat(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_X0 + coordinateIndex]);
	}

	@Override
	public float x(int vertexIndex) {
		return Float.intBitsToFloat(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_X0]);
	}

	@Override
	public float y(int vertexIndex) {
		return Float.intBitsToFloat(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_Y0]);
	}

	@Override
	public float z(int vertexIndex) {
		return Float.intBitsToFloat(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_Z0]);
	}

	@Override
	public boolean hasNormal(int vertexIndex) {
		return (normalFlags() & (1 << vertexIndex)) != 0;
	}

	@Override
	public Vec3f copyNormal(int vertexIndex, Vec3f target) {
		if (hasNormal(vertexIndex)) {
			if (target == null) {
				target = new Vec3f();
			}

			return NormalHelper.unpackNormalTo(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0], target);
		} else {
			return null;
		}
	}

	public int packedNormal(int vertexIndex) {
		return hasNormal(vertexIndex) ? data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0] : packedFaceNormal();
	}

	@Override
	public float normalX(int vertexIndex) {
		return hasNormal(vertexIndex) ? NormalHelper.packedNormalX(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0]) : Float.NaN;
	}

	@Override
	public float normalY(int vertexIndex) {
		return hasNormal(vertexIndex) ? NormalHelper.packedNormalY(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0]) : Float.NaN;
	}

	@Override
	public float normalZ(int vertexIndex) {
		return hasNormal(vertexIndex) ? NormalHelper.packedNormalZ(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0]) : Float.NaN;
	}

	@Override
	public boolean hasTangent(int vertexIndex) {
		return (tangentFlags() & (1 << vertexIndex)) != 0;
	}

	@Override
	public Vec3f copyTangent(int vertexIndex, Vec3f target) {
		if (hasTangent(vertexIndex)) {
			if (target == null) {
				target = new Vec3f();
			}

			return NormalHelper.unpackNormalTo(data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT], target);
		} else {
			return null;
		}
	}

	public int packedTangent(int vertexIndex) {
		// WIP should probably not return zero here
		return hasTangent(vertexIndex) ? data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT] : 0;
	}

	@Override
	public float tangentX(int vertexIndex) {
		return hasTangent(vertexIndex) ? NormalHelper.packedNormalX(data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT]) : Float.NaN;
	}

	@Override
	public float tangentY(int vertexIndex) {
		return hasTangent(vertexIndex) ? NormalHelper.packedNormalY(data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT]) : Float.NaN;
	}

	@Override
	public float tangentZ(int vertexIndex) {
		return hasTangent(vertexIndex) ? NormalHelper.packedNormalZ(data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT]) : Float.NaN;
	}

	@Override
	public int lightmap(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_LIGHTMAP0];
	}

	@Override
	public int vertexColor(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_COLOR0];
	}

	protected final boolean isSpriteNormalized() {
		return !isSpriteInterpolated;
	}

	protected final float spriteFloatU(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0] * UV_PRECISE_TO_FLOAT_CONVERSION;
	}

	protected final float spriteFloatV(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_V0] * UV_PRECISE_TO_FLOAT_CONVERSION;
	}

	@Override
	public float spriteU(int vertexIndex) {
		return isSpriteNormalized() && material().texture.isAtlas()
			? material().texture.atlasInfo().mapU(spriteId(), spriteFloatU(vertexIndex))
			: spriteFloatU(vertexIndex);
	}

	@Override
	public float spriteV(int vertexIndex) {
		return isSpriteNormalized() && material().texture.isAtlas()
			? material().texture.atlasInfo().mapV(spriteId(), spriteFloatV(vertexIndex))
			: spriteFloatV(vertexIndex);
	}

	/**
	 * Fixed precision value suitable for transformations.
	 */
	public int spritePreciseU(int vertexIndex) {
		assert isSpriteNormalized();
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0];
	}

	/**
	 * Fixed precision value suitable for transformations.
	 */
	public int spritePreciseV(int vertexIndex) {
		assert isSpriteNormalized();
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_V0];
	}

	/**
	 * Rounds precise fixed-precision sprite value to an unsigned short value.
	 *
	 * <p>NB: This logic is inlined into quad encoders. Those are complicated enough
	 * that JIT may not reliably inline them at runtime.  If this logic is updated
	 * it must be updated there also.
	 */
	public static int roundSpriteData(int rawVal) {
		return (rawVal + UV_ROUNDING_BIT) >> UV_EXTRA_PRECISION;
	}

	/**
	 * Rounded, unsigned short value suitable for vertex buffer.
	 */
	public int spriteBufferU(int vertexIndex) {
		assert isSpriteNormalized();
		return roundSpriteData(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0]);
	}

	/**
	 * Rounded, unsigned short value suitable for vertex buffer.
	 */
	public int spriteBufferV(int vertexIndex) {
		assert isSpriteNormalized();
		return roundSpriteData(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_V0]);
	}

	public int spriteId() {
		return data[baseIndex + HEADER_SPRITE];
	}

	public void transformAndAppendVertex(final int vertexIndex, final Matrix4fExt matrix, final VertexConsumer buff) {
		final int[] data = this.data;
		final int index = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_X0;
		final float x = Float.intBitsToFloat(data[index]);
		final float y = Float.intBitsToFloat(data[index + 1]);
		final float z = Float.intBitsToFloat(data[index + 2]);

		final float xOut = matrix.a00() * x + matrix.a01() * y + matrix.a02() * z + matrix.a03();
		final float yOut = matrix.a10() * x + matrix.a11() * y + matrix.a12() * z + matrix.a13();
		final float zOut = matrix.a20() * x + matrix.a21() * y + matrix.a22() * z + matrix.a23();

		buff.vertex(xOut, yOut, zOut);
	}
}
