/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.apiimpl.mesh;

import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_BITS;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FACE_NORMAL;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FACE_TANGENT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_MATERIAL;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_SPRITE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_TAG;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_QUAD_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE_SHIFT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_EXTRA_PRECISION;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_PRECISE_TO_FLOAT_CONVERSION;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_ROUNDING_BIT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_NORMAL0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_START;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_U0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_V0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_X0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Y0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_Z0;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;

import net.minecraft.core.Direction;

import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.api.mesh.QuadView;
import io.vram.frex.api.model.util.FaceUtil;
import io.vram.frex.api.model.util.GeometryUtil;
import io.vram.frex.api.model.util.PackedVector3f;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;

import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix4fExt;

/**
 * Base class for all quads / quad makers. Handles the ugly bits
 * of maintaining and encoding the quad state.
 */
public class QuadViewImpl implements QuadView {
	protected int nominalFaceId = FaceUtil.UNASSIGNED_INDEX;
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
	 * gets flags used for lighting - lazily computed via {@link GeometryUtil#computeShapeFlags(QuadView)}.
	 */
	public int geometryFlags() {
		computeGeometry();
		return MeshEncodingHelper.geometryFlags(data[baseIndex + HEADER_BITS]);
	}

	protected void computeGeometry() {
		if (isGeometryInvalid) {
			isGeometryInvalid = false;
			data[baseIndex + HEADER_FACE_NORMAL] = PackedVector3f.computePackedFaceNormal(this);

			final int flagsIndex = baseIndex + HEADER_BITS;

			// depends on face normal
			// NB: important to save back to array because used by geometry helper
			data[flagsIndex] = MeshEncodingHelper.lightFace(data[flagsIndex], GeometryUtil.lightFaceId(this));

			// depends on light face
			data[flagsIndex] = MeshEncodingHelper.geometryFlags(data[flagsIndex], GeometryUtil.computeShapeFlags(this));
		}
	}

	@Override
	public final void toVanilla(int[] target, int targetIndex) {
		System.arraycopy(data, baseIndex + VERTEX_START, target, targetIndex, MESH_QUAD_STRIDE);

		// Convert sprite data from fixed precision to float
		int index = targetIndex + 4;

		for (int i = 0; i < 4; ++i) {
			target[index] = Float.floatToRawIntBits(u(i));
			target[index + 1] = Float.floatToRawIntBits(v(i));
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
	public final Direction lightFace() {
		return FaceUtil.faceFromIndex(lightFaceId());
	}

	public final int cullFaceId() {
		return MeshEncodingHelper.cullFace(data[baseIndex + HEADER_BITS]);
	}

	/**
	 * Based on geometry instead of metadata.  More reliable for terrain backface culling.
	 */
	public final int effectiveCullFaceId() {
		return (geometryFlags() & GeometryUtil.LIGHT_FACE_FLAG) == 0 ? FaceUtil.UNASSIGNED_INDEX : lightFaceId();
	}

	@Override
	public final Direction cullFace() {
		return FaceUtil.faceFromIndex(cullFaceId());
	}

	@Override
	public final Direction nominalFace() {
		return FaceUtil.faceFromIndex(nominalFaceId);
	}

	@Override
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

		return PackedVector3f.pack(tx, ty, tz);
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
	public void copyTo(QuadEmitter target) {
		// force geometry compute
		computeGeometry();
		// force tangent compute
		this.packedFaceTanget();

		final QuadEditorImpl quad = (QuadEditorImpl) target;

		// copy everything except the material
		System.arraycopy(data, baseIndex, quad.data, quad.baseIndex, MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE);
		quad.isSpriteInterpolated = isSpriteInterpolated;
		quad.nominalFaceId = nominalFaceId;
		quad.isGeometryInvalid = false;
		quad.isTangentInvalid = false;
	}

	@Override
	public Vector3f copyPos(int vertexIndex, Vector3f target) {
		if (target == null) {
			target = new Vector3f();
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
	public int packedNormal(int vertexIndex) {
		return hasNormal(vertexIndex) ? data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0] : packedFaceNormal();
	}

	@Override
	public boolean hasTangent(int vertexIndex) {
		return (tangentFlags() & (1 << vertexIndex)) != 0;
	}

	@Override
	public Vector3f copyTangent(int vertexIndex, Vector3f target) {
		if (hasTangent(vertexIndex)) {
			if (target == null) {
				target = new Vector3f();
			}

			return PackedVector3f.unpackTo(data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT], target);
		} else {
			return null;
		}
	}

	@Override
	public int packedTangent(int vertexIndex) {
		// WIP should probably not return zero here
		return hasTangent(vertexIndex) ? data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT] : 0;
	}

	@Override
	public int lightmap(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_LIGHTMAP0];
	}

	@Override
	public int vertexColor(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_COLOR0];
	}

	protected final float spriteFloatU(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0] * UV_PRECISE_TO_FLOAT_CONVERSION;
	}

	protected final float spriteFloatV(int vertexIndex) {
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_V0] * UV_PRECISE_TO_FLOAT_CONVERSION;
	}

	@Override
	public float u(int vertexIndex) {
		return !isSpriteInterpolated && material().texture.isAtlas()
			? material().texture.spriteIndex().mapU(spriteId(), spriteFloatU(vertexIndex))
			: spriteFloatU(vertexIndex);
	}

	@Override
	public float v(int vertexIndex) {
		return !isSpriteInterpolated && material().texture.isAtlas()
			? material().texture.spriteIndex().mapV(spriteId(), spriteFloatV(vertexIndex))
			: spriteFloatV(vertexIndex);
	}

	@Override
	public float uSprite(int vertexIndex) {
		assert !isSpriteInterpolated;
		return spriteFloatU(vertexIndex);
	}

	@Override
	public float vSprite(int vertexIndex) {
		assert !isSpriteInterpolated;
		return spriteFloatV(vertexIndex);
	}

	/**
	 * Fixed precision value suitable for transformations.
	 */
	public int spritePreciseU(int vertexIndex) {
		assert !isSpriteInterpolated;
		return data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0];
	}

	/**
	 * Fixed precision value suitable for transformations.
	 */
	public int spritePreciseV(int vertexIndex) {
		assert !isSpriteInterpolated;
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
		assert !isSpriteInterpolated;
		return roundSpriteData(data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0]);
	}

	/**
	 * Rounded, unsigned short value suitable for vertex buffer.
	 */
	public int spriteBufferV(int vertexIndex) {
		assert !isSpriteInterpolated;
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

		final float xOut = matrix.m00() * x + matrix.m01() * y + matrix.m02() * z + matrix.m03();
		final float yOut = matrix.m10() * x + matrix.m11() * y + matrix.m12() * z + matrix.m13();
		final float zOut = matrix.m20() * x + matrix.m21() * y + matrix.m22() * z + matrix.m23();

		buff.vertex(xOut, yOut, zOut);
	}
}
