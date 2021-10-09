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

import com.google.common.base.Preconditions;

import io.vram.frex.api.mesh.QuadView;
import io.vram.frex.api.model.ModelHelper;

/**
 * Holds all the array offsets and bit-wise encoders/decoders for
 * packing/unpacking quad data in an array of integers. All of this is
 * implementation-specific - that's why it isn't a "helper" class.
 */
public abstract class MeshEncodingHelper {
	public static final int HEADER_MATERIAL = 0;
	public static final int HEADER_COLOR_INDEX = 1;
	public static final int HEADER_BITS = 2;
	public static final int HEADER_TAG = 3;
	public static final int HEADER_SPRITE = 4;
	public static final int HEADER_FACE_NORMAL = 5;
	public static final int HEADER_FACE_TANGENT = 6;
	public static final int HEADER_FIRST_VERTEX_TANGENT = 7;
	/** Tangent vectors are stored in header so that vertex data can be efficiently translated to/from vanilla. */
	public static final int HEADER_STRIDE = HEADER_FIRST_VERTEX_TANGENT + 4;
	public static final int VERTEX_X = 0;
	public static final int VERTEX_Y = 1;
	public static final int VERTEX_Z = 2;
	public static final int VERTEX_COLOR = 3;
	public static final int VERTEX_U = 4;
	public static final int VERTEX_V = 5;
	public static final int VERTEX_LIGHTMAP = 6;
	public static final int VERTEX_NORMAL = 7;

	public static final int VERTEX_X0 = HEADER_STRIDE + VERTEX_X;
	public static final int VERTEX_Y0 = HEADER_STRIDE + VERTEX_Y;
	public static final int VERTEX_Z0 = HEADER_STRIDE + VERTEX_Z;
	public static final int VERTEX_COLOR0 = HEADER_STRIDE + VERTEX_COLOR;
	public static final int VERTEX_U0 = HEADER_STRIDE + VERTEX_U;
	public static final int VERTEX_V0 = HEADER_STRIDE + VERTEX_V;
	public static final int VERTEX_LIGHTMAP0 = HEADER_STRIDE + VERTEX_LIGHTMAP;
	public static final int VERTEX_NORMAL0 = HEADER_STRIDE + VERTEX_NORMAL;

	// Must be power of two for shift constant to work
	public static final int MESH_VERTEX_STRIDE = 8;
	public static final int MESH_VERTEX_STRIDE_SHIFT = 3;

	public static final int VERTEX_START = VERTEX_X0;
	public static final int MESH_QUAD_STRIDE = MESH_VERTEX_STRIDE * 4;
	public static final int MESH_QUAD_STRIDE_BYTES = MESH_QUAD_STRIDE * 4;
	/** Includes header. */
	public static final int TOTAL_MESH_QUAD_STRIDE = HEADER_STRIDE + MESH_QUAD_STRIDE;

	/**
	 * Used for quick clearing of quad buffers.
	 */
	public static final int[] EMPTY = new int[TOTAL_MESH_QUAD_STRIDE];

	static {
		EMPTY[HEADER_COLOR_INDEX] = -1;
		EMPTY[HEADER_BITS] = MeshEncodingHelper.cullFace(0, ModelHelper.UNASSIGNED_INDEX);
	}

	public static final int DEFAULT_HEADER_BITS;
	public static final int UV_UNIT_VALUE = 0xFFFF;
	public static final int UV_EXTRA_PRECISION = 8;
	public static final int UV_PRECISE_UNIT_VALUE = UV_UNIT_VALUE << UV_EXTRA_PRECISION;
	public static final int UV_ROUNDING_BIT = 1 << (UV_EXTRA_PRECISION - 1);
	public static final float UV_PRECISE_TO_FLOAT_CONVERSION = 1f / UV_PRECISE_UNIT_VALUE;
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

	private static final int TANGENTS_SHIFT = GEOMETRY_SHIFT + Integer.bitCount(GEOMETRY_MASK);
	private static final int TANGENTS_MASK = 0b1111;
	private static final int TANGENTS_INVERSE_MASK = ~(TANGENTS_MASK << TANGENTS_SHIFT);

	static {
		assert TANGENTS_SHIFT + 4 <= 32 : "Mesh header encoding ran out of bits";
		Preconditions.checkState(MESH_VERTEX_STRIDE == QuadView.VANILLA_VERTEX_STRIDE, "Canvas vertex stride (%s) mismatched with rendering API (%s)", MESH_VERTEX_STRIDE, QuadView.VANILLA_VERTEX_STRIDE);
		Preconditions.checkState(MESH_QUAD_STRIDE == QuadView.VANILLA_QUAD_STRIDE, "Canvas quad stride (%s) mismatched with rendering API (%s)", MESH_QUAD_STRIDE, QuadView.VANILLA_QUAD_STRIDE);
	}

	static {
		int defaultHeader = 0;
		defaultHeader = cullFace(defaultHeader, ModelHelper.UNASSIGNED_INDEX);
		defaultHeader = lightFace(defaultHeader, ModelHelper.UNASSIGNED_INDEX);
		DEFAULT_HEADER_BITS = defaultHeader;
	}

	private MeshEncodingHelper() {
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

	public static int tangentFlags(int bits) {
		return (bits >> TANGENTS_SHIFT) & TANGENTS_MASK;
	}

	public static int tangentFlags(int bits, int tangentFlags) {
		return (bits & TANGENTS_INVERSE_MASK) | ((tangentFlags & TANGENTS_MASK) << TANGENTS_SHIFT);
	}

	public static int stride() {
		return TOTAL_MESH_QUAD_STRIDE;
	}

	public static int geometryFlags(int bits) {
		return bits >> GEOMETRY_SHIFT;
	}

	public static int geometryFlags(int bits, int geometryFlags) {
		return (bits & GEOMETRY_INVERSE_MASK) | ((geometryFlags & GEOMETRY_MASK) << GEOMETRY_SHIFT);
	}

	static int packNormalizedUV(float u, float v) {
		return Math.round(u * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
	}

	static int packColor(int red, int green, int blue, int alpha) {
		return red | (green << 8) | (blue << 16) | (alpha << 24);
	}

	static int packColor(float red, float green, float blue, float alpha) {
		return packColor((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
	}

	static int packColorFromFloats(float red, float green, float blue, float alpha) {
		return packColorFromBytes((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
	}

	static int packColorFromBytes(int red, int green, int blue, int alpha) {
		return red | (green << 8) | (blue << 16) | (alpha << 24);
	}

	public static final int FULL_BRIGHTNESS = 0xF000F0;

	public static final int NORMALIZED_U0_V0 = packNormalizedUV(0, 0);
	public static final int NORMALIZED_U0_V1 = packNormalizedUV(0, 1);
	public static final int NORMALIZED_U1_V0 = packNormalizedUV(1, 0);
	public static final int NORMALIZED_U1_V1 = packNormalizedUV(1, 1);
}
