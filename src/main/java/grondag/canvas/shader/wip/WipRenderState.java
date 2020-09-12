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

import javax.annotation.Nullable;

import grondag.canvas.shader.wip.encoding.WipModelOrigin;
import grondag.canvas.shader.wip.encoding.WipVertexCollectorImpl;
import grondag.canvas.shader.wip.encoding.WipVertexFormat;
import grondag.fermion.bits.BitPacker64;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

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
	private final long bits;
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
	public final int vertexStrideInts;
	public final WipTextureState texture;

	private WipRenderState(long bits) {
		this.bits = bits;
		modelOrigin = ORIGIN.getValue(bits);
		texture = WipTextureState.fromIndex(TEXTURE.getValue(bits));

		format = WipVertexFormat.forFlags(
			HAS_COLOR.getValue(bits),
			texture != WipTextureState.NO_TEXTURE,
			texture.isAtlas || HAS_CONDITION.getValue(bits),
			HAS_LIGHTMAP.getValue(bits),
			HAS_NORMAL.getValue(bits));

		vertexStrideInts = format.vertexStrideInts;

		index = nextIndex++;
	}

	public void draw(WipVertexCollectorImpl collector) {
		// TODO Auto-generated method stub

	}

	public static final int MAX_COUNT = 4096;
	private static int nextIndex = 0;
	private static final WipRenderState[] STATES = new WipRenderState[MAX_COUNT];
	private static final Long2ObjectOpenHashMap<WipRenderState> MAP = new Long2ObjectOpenHashMap<>(4096, Hash.VERY_FAST_LOAD_FACTOR);

	private static final BitPacker64<Void> PACKER = new BitPacker64<> (null, null);

	// GL State comes first for sorting
	private static final BitPacker64.IntElement TEXTURE = PACKER.createIntElement(WipTextureState.MAX_TEXTURE_STATES);

	// These don't affect GL state but do affect encoding - must be buffered separately
	private static final BitPacker64.BooleanElement SORTED = PACKER.createBooleanElement();
	private static final BitPacker64.IntElement PRIMITIVE = PACKER.createIntElement(8);
	private static final BitPacker64.BooleanElement HAS_COLOR = PACKER.createBooleanElement();
	private static final BitPacker64.BooleanElement HAS_LIGHTMAP = PACKER.createBooleanElement();
	private static final BitPacker64.BooleanElement HAS_NORMAL = PACKER.createBooleanElement();
	private static final BitPacker64.BooleanElement HAS_CONDITION = PACKER.createBooleanElement();

	private static final BitPacker64<Void>.EnumElement<WipModelOrigin> ORIGIN = PACKER.createEnumElement(WipModelOrigin.class);

	static {
		assert PACKER.bitLength() <= 64;
	}

	public static WipRenderState fromLayer(RenderLayer renderLayer) {
		// TODO Auto-generated method stub
		return null;
	}

	public static WipRenderState fromIndex(int index) {
		return STATES[index];
	}

	public static class Builder {
		long bits;
		// GL State
		// WIP: transparency
		// WIP: depth test
		// WIP: cull
		// WIP: enable lightmap
		// WIP: framebuffer target(s) - add emissive and other targets to vanilla
		// WIP: write mask state
		// WIP: line width
		// WIP: texture binding
		// WIP: texture setting - may need to be uniform or conditional compile if fixed pipeline filtering doesn't work
		// sets up outline, glint or default texturing
		// these probably won't work as-is with shaders because they use texture env settings
		// so may be best to leave them for now

		// Uniform state
		// WIP: fog - colored, black or off - could go in vertex state but doesn't change much

		public Builder sorted(boolean sorted) {
			bits = SORTED.setValue(sorted, bits);
			return this;
		}

		public Builder hasColor(boolean hasColor) {
			bits = HAS_COLOR.setValue(hasColor, bits);
			return this;
		}

		public Builder hasLightmap(boolean hasLightmap) {
			bits = HAS_LIGHTMAP.setValue(hasLightmap, bits);
			return this;
		}

		public Builder hasNormal(boolean hasNormal) {
			bits = HAS_NORMAL.setValue(hasNormal, bits);
			return this;
		}

		public Builder hasCondition(boolean hasCondition) {
			bits = HAS_CONDITION.setValue(hasCondition, bits);
			return this;
		}

		public Builder primitive(int primitive) {
			assert primitive <= 7;
			bits = PRIMITIVE.setValue(primitive, bits);
			return this;
		}

		public Builder texture(@Nullable Identifier id) {
			final int val = id == null ? WipTextureState.NO_TEXTURE.index : WipTextureState.fromId(id).index;
			bits = TEXTURE.setValue(val, bits);
			return this;
		}

		public WipRenderState build() {
			WipRenderState result = MAP.get(bits);

			if (result == null) {
				result = new WipRenderState(bits);
				MAP.put(bits, result);
				STATES[result.index] = result;
			}

			return result;
		}
	}
}
