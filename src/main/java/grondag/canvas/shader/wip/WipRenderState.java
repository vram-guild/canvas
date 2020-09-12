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

package grondag.canvas.shader.wip;

import grondag.canvas.shader.wip.encoding.WipModelOrigin;
import grondag.canvas.shader.wip.encoding.WipVertexFormat;
import grondag.fermion.bits.BitPacker32;

/**
 * Primitives with the same state have the same vertex encoding,
 * same uniform state and same GL draw state. Analogous to RenderLayer<p>
 *
 * Also serves as the key for vertex collection. Primitives with the same state
 * can share the same draw call and should be packed contiguously in the buffer.<p>
 *
 * Primitives must have the same sorting requirements, which for all but the translucent
 * collection keys means there is no sorting. Translucent primitives that require sorting
 * all belong to a small handful of collectors.<p>
 *
 * Vertex data with different state can share the same buffer and should be
 * packed in glState, uniformState order for best performance.
 */
@SuppressWarnings("rawtypes")
public class WipRenderState {
	private final int bits;
	public final int index;

	/**
	 * true only for translucent
	 */
	public boolean sorted() {
		return SORTED.getValue(bits);
	}

	/**
	 * True when the material has vertex color and thus
	 * color should be included in the vertex format.
	 */
	public boolean hasColor() {
		return HAS_COLOR.getValue(bits);
	}

	/**
	 * True when material has vertex normals and thus
	 * normals should be included in the vertex format.
	 */
	public boolean hasNormal() {
		return HAS_NORMAL.getValue(bits);
	}

	//	/**
	//	 * True when material has a primary texture and thus
	//	 * includes UV coordinates in the vertex format.
	//	 * Canvas may compact or alter UV packing to allow for
	//	 * multi-map textures that share UV coordinates.
	//	 */
	//	public boolean hasTexture() {
	//		return HAS_TEXTURE.getValue(bits);
	//	}

	/**
	 * True if world lighting is passed to the renderer for this material.
	 * In vanilla lighting, this is done by a lightmap UV coordinate.
	 * Canvas may compact this, or may not pass it to the renderer at all,
	 * depending on the lighting model. True still indicates the material
	 * should be affected by world lighting.<p>
	 *
	 * UGLY: should this be hasWorldLight instead?  Semantics are messy.
	 */
	public boolean hasLightMap() {
		return HAS_LIGHTMAP.getValue(bits);
	}

	/**
	 * True if the material has a color overlay.  In vanilla this is passed
	 * as UV coordinates for a specialized texture that must be included
	 * in the vertex attributes. Canvas may convey this via bit flags instead.
	 */
	public boolean hasOverlay() {
		return HAS_OVERLAY.getValue(bits);
	}

	/**
	 * OpenGL primitive constant. Determines number of vertices.
	 *
	 * Currently used in vanilla are...
	 * GL_LINES
	 * GL_LINE_STRIP (currently GUI only)
	 * GL_TRIANGLE_STRIP (currently GUI only)
	 * GL_TRIANGLE_FAN (currently GUI only)
	 * GL_QUADS
	 */
	public int primitive() {
		return PRIMITIVE.getValue(bits);
	}

	public final WipVertexFormat format;
	public final WipModelOrigin modelOrigin;
	public final WipGlState glState;
	public final WipUniformState uniformState;
	public final int vertexStrideInts;

	private WipRenderState(int bits) {
		this.bits = bits;
		// TODO: all of these
		glState = null;
		uniformState = null;
		modelOrigin = ORIGIN.getValue(bits);
		format = WipVertexFormat.forState(this);
		vertexStrideInts = format.vertexStrideInts;
		index = nextIndex++;
	}

	public static final int MAX_INDEX = 4096;
	private static int nextIndex = 0;

	private static final BitPacker32<Void> PACKER = new BitPacker32<> (null, null);

	private static final BitPacker32.BooleanElement SORTED = PACKER.createBooleanElement();
	private static final BitPacker32.IntElement PRIMITIVE = PACKER.createIntElement(8);
	private static final BitPacker32.BooleanElement HAS_COLOR = PACKER.createBooleanElement();
	private static final BitPacker32.BooleanElement HAS_NORMAL = PACKER.createBooleanElement();
	private static final BitPacker32.BooleanElement ALLOWS_CONDITIONS = PACKER.createBooleanElement();
	private static final BitPacker32.BooleanElement HAS_LIGHTMAP = PACKER.createBooleanElement();
	private static final BitPacker32.BooleanElement HAS_OVERLAY = PACKER.createBooleanElement();
	private static final BitPacker32<Void>.EnumElement<WipModelOrigin> ORIGIN = PACKER.createEnumElement(WipModelOrigin.class);

	static {
		assert PACKER.bitLength() <= 32;
	}
}
