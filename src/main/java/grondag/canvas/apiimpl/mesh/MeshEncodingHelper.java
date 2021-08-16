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

import com.google.common.base.Preconditions;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

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
	public static final int HEADER_STRIDE = 5;
	public static final int VERTEX_X = 0;
	public static final int VERTEX_Y = 1;
	public static final int VERTEX_Z = 2;
	public static final int VERTEX_COLOR = 3;
	public static final int VERTEX_U = 4;
	public static final int VERTEX_V = 5;
	public static final int VERTEX_LIGHTMAP = 6;
	public static final int VERTEX_NORMAL = 7;
	public static final int FIRST_VERTEX_X;
	public static final int FIRST_VERTEX_Y;
	public static final int FIRST_VERTEX_Z;
	public static final int FIRST_VERTEX_COLOR;
	public static final int FIRST_VERTEX_U;
	public static final int FIRST_VERTEX_V;
	public static final int FIRST_VERTEX_LIGHTMAP;
	public static final int FIRST_VERTEX_NORMAL;
	public static final int BASE_VERTEX_STRIDE;
	public static final int BASE_QUAD_STRIDE_BYTES;
	public static final int MIN_QUAD_STRIDE;
	public static final int VERTEX_START;
	public static final int BASE_QUAD_STRIDE;
	// normals are followed by 0-2 sets of color/uv coordinates
	public static final int TEXTURE_VERTEX_STRIDE;
	public static final int TEXTURE_QUAD_STRIDE;

	/**
	 * Is one tex stride less than the actual base, because when used tex index is >= 1.
	 */
	public static final int TEXTURE_OFFSET_MINUS;
	public static final int MAX_QUAD_STRIDE;

	/**
	 * Used for quick clearing of quad buffers.
	 */
	public static final int[] EMPTY;
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

	static {
		FIRST_VERTEX_X = HEADER_STRIDE + VERTEX_X;
		FIRST_VERTEX_Y = HEADER_STRIDE + VERTEX_Y;
		FIRST_VERTEX_Z = HEADER_STRIDE + VERTEX_Z;
		FIRST_VERTEX_COLOR = HEADER_STRIDE + VERTEX_COLOR;
		FIRST_VERTEX_U = HEADER_STRIDE + VERTEX_U;
		FIRST_VERTEX_V = HEADER_STRIDE + VERTEX_V;
		FIRST_VERTEX_LIGHTMAP = HEADER_STRIDE + VERTEX_LIGHTMAP;
		FIRST_VERTEX_NORMAL = HEADER_STRIDE + VERTEX_NORMAL;
		BASE_VERTEX_STRIDE = 8;
		BASE_QUAD_STRIDE = BASE_VERTEX_STRIDE * 4;
		BASE_QUAD_STRIDE_BYTES = BASE_QUAD_STRIDE * 4;
		MIN_QUAD_STRIDE = HEADER_STRIDE + BASE_QUAD_STRIDE;

		Preconditions.checkState(BASE_VERTEX_STRIDE == QuadView.VANILLA_VERTEX_STRIDE, "Canvas vertex stride (%s) mismatched with rendering API (%s)", BASE_VERTEX_STRIDE, QuadView.VANILLA_VERTEX_STRIDE);
		Preconditions.checkState(BASE_QUAD_STRIDE == QuadView.VANILLA_QUAD_STRIDE, "Canvas quad stride (%s) mismatched with rendering API (%s)", BASE_QUAD_STRIDE, QuadView.VANILLA_QUAD_STRIDE);

		VERTEX_START = FIRST_VERTEX_X;

		// base quad followed by 0-2 sets of color/uv coordinates
		TEXTURE_VERTEX_STRIDE = 3;
		TEXTURE_QUAD_STRIDE = TEXTURE_VERTEX_STRIDE * 4;

		/**
		 * is one tex stride less than the actual base, because when used tex index is >= 1
		 */
		TEXTURE_OFFSET_MINUS = MIN_QUAD_STRIDE - TEXTURE_QUAD_STRIDE;
		MAX_QUAD_STRIDE = MIN_QUAD_STRIDE + TEXTURE_QUAD_STRIDE;
		EMPTY = new int[MAX_QUAD_STRIDE];
	}

	static {
		int defaultHeader = 0;
		defaultHeader = cullFace(defaultHeader, ModelHelper.NULL_FACE_ID);
		defaultHeader = lightFace(defaultHeader, ModelHelper.NULL_FACE_ID);
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

	public static int stride() {
		return MIN_QUAD_STRIDE;
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
