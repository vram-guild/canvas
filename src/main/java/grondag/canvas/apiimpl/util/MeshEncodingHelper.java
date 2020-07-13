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

import com.google.common.base.Preconditions;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.RenderMaterialImpl;

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

	public static final int VERTEX_X;
	public static final int VERTEX_Y;
	public static final int VERTEX_Z;
	public static final int VERTEX_COLOR;
	public static final int VERTEX_U;
	public static final int VERTEX_V;
	public static final int VERTEX_LIGHTMAP;
	public static final int VERTEX_NORMAL;
	public static final int BASE_VERTEX_STRIDE;

	public static final int BASE_QUAD_STRIDE_BYTES;
	public static final int MIN_QUAD_STRIDE;
	public static final int VERTEX_START;
	public static final int BASE_QUAD_STRIDE;

	// normals are followed by 0-2 sets of color/uv coordinates
	public static final int TEXTURE_VERTEX_STRIDE;
	public static final int TEXTURE_QUAD_STRIDE;

	/**
	 * is one tex stride less than the actual base, because when used tex index is >= 1
	 */
	public static final int TEXTURE_OFFSET_MINUS;
	public static final int SECOND_TEXTURE_OFFSET;
	public static final int THIRD_TEXTURE_OFFSET;
	public static final int MAX_QUAD_STRIDE;

	static {
		VERTEX_X = HEADER_STRIDE + 0;
		VERTEX_Y = HEADER_STRIDE + 1;
		VERTEX_Z = HEADER_STRIDE + 2;
		VERTEX_COLOR = HEADER_STRIDE + 3;
		VERTEX_U = HEADER_STRIDE + 4;
		VERTEX_V = VERTEX_U + 1;
		VERTEX_LIGHTMAP = HEADER_STRIDE + 6;
		VERTEX_NORMAL = HEADER_STRIDE + 7;
		BASE_VERTEX_STRIDE = 8;
		BASE_QUAD_STRIDE = BASE_VERTEX_STRIDE * 4;
		BASE_QUAD_STRIDE_BYTES = BASE_QUAD_STRIDE * 4;
		MIN_QUAD_STRIDE = HEADER_STRIDE + BASE_QUAD_STRIDE;

		Preconditions.checkState(BASE_VERTEX_STRIDE == QuadView.VANILLA_VERTEX_STRIDE, "Canvas vertex stride (%s) mismatched with rendering API (%s)", BASE_VERTEX_STRIDE, QuadView.VANILLA_VERTEX_STRIDE);
		Preconditions.checkState(BASE_QUAD_STRIDE == QuadView.VANILLA_QUAD_STRIDE, "Canvas quad stride (%s) mismatched with rendering API (%s)", BASE_QUAD_STRIDE, QuadView.VANILLA_QUAD_STRIDE);

		VERTEX_START = VERTEX_X;

		// base quad followed by 0-2 sets of color/uv coordinates
		TEXTURE_VERTEX_STRIDE = 3;
		TEXTURE_QUAD_STRIDE = TEXTURE_VERTEX_STRIDE * 4;

		/**
		 * is one tex stride less than the actual base, because when used tex index is >= 1
		 */
		TEXTURE_OFFSET_MINUS = MIN_QUAD_STRIDE - TEXTURE_QUAD_STRIDE;
		SECOND_TEXTURE_OFFSET = MIN_QUAD_STRIDE;
		THIRD_TEXTURE_OFFSET = SECOND_TEXTURE_OFFSET + TEXTURE_QUAD_STRIDE;
		MAX_QUAD_STRIDE = MIN_QUAD_STRIDE + TEXTURE_QUAD_STRIDE * (RenderMaterialImpl.MAX_SPRITE_DEPTH - 1);
	}

	/** used for quick clearing of quad buffers */
	public static final int[] EMPTY = new int[MAX_QUAD_STRIDE];

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
		return SECOND_TEXTURE_OFFSET - TEXTURE_QUAD_STRIDE + textureDepth * TEXTURE_QUAD_STRIDE;
	}

	public static int geometryFlags(int bits) {
		return bits >> GEOMETRY_SHIFT;
	}

	public static int geometryFlags(int bits, int geometryFlags) {
		return (bits & GEOMETRY_INVERSE_MASK) | ((geometryFlags & GEOMETRY_MASK) << GEOMETRY_SHIFT);
	}
}
