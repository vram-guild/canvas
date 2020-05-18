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

import static grondag.canvas.apiimpl.util.MeshEncodingHelper.BASE_QUAD_STRIDE;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.BASE_VERTEX_STRIDE;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.EMPTY;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_BITS;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_MATERIAL;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_STRIDE;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.HEADER_TAG;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.VERTEX_NORMAL;
import static grondag.canvas.apiimpl.util.MeshEncodingHelper.VERTEX_X;

import com.google.common.base.Preconditions;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.util.GeometryHelper;
import grondag.canvas.apiimpl.util.MeshEncodingHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.apiimpl.util.TextureHelper;
import grondag.canvas.light.LightmapHd;

/**
 * Almost-concrete implementation of a mutable quad. The only missing part is {@link #emit()},
 * because that depends on where/how it is used. (Mesh encoding vs. render-time transformation).
 */
public abstract class MutableQuadViewImpl extends QuadViewImpl implements QuadEmitter {
	// UGLY - need a lighting result class?
	public LightmapHd blockLight = null;
	public LightmapHd skyLight = null;
	public LightmapHd aoShade = null;
	// PERF: pack into one array for LOR?
	public final float[] u = new float[4];
	public final float[] v = new float[4];
	// vanilla light outputs
	public final float[] ao = new float[4];

	public final void begin(int[] data, int baseIndex) {
		this.data = data;
		this.baseIndex = baseIndex;
		clear();
	}

	public void clear() {
		System.arraycopy(EMPTY, 0, data, baseIndex, MeshEncodingHelper.MAX_QUAD_STRIDE);
		isFaceNormalInvalid = true;
		isGeometryInvalid = true;
		nominalFaceId = ModelHelper.NULL_FACE_ID;
		normalFlags(0);
		// tag(0); seems redundant - handled by array copy
		colorIndex(-1);
		cullFace(null);
		material(Canvas.MATERIAL_STANDARD);
	}

	@Override
	public final MutableQuadViewImpl material(RenderMaterial material) {
		if (material == null) {
			material = Canvas.MATERIAL_STANDARD;
		}

		data[baseIndex + HEADER_MATERIAL] = ((RenderMaterialImpl.CompositeMaterial)material).index();

		assert RenderMaterialImpl.byIndex(data[baseIndex + HEADER_MATERIAL]) == material;

		return this;
	}

	public final MutableQuadViewImpl cullFace(int faceId) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.cullFace(data[baseIndex + HEADER_BITS], faceId);
		nominalFaceId = faceId;
		return this;
	}

	@Override
	@Deprecated
	public final MutableQuadViewImpl cullFace(Direction face) {
		return cullFace(ModelHelper.toFaceIndex(face));
	}

	public final MutableQuadViewImpl lightFace(int faceId) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.lightFace(data[baseIndex + HEADER_BITS], faceId);
		return this;
	}

	@Deprecated
	public final MutableQuadViewImpl lightFace(Direction face) {
		Preconditions.checkNotNull(face);
		return lightFace(ModelHelper.toFaceIndex(face));
	}

	public final MutableQuadViewImpl nominalFace(int faceId) {
		nominalFaceId = faceId;
		return this;
	}

	@Override
	@Deprecated
	public final MutableQuadViewImpl nominalFace(Direction face) {
		return nominalFace(ModelHelper.toFaceIndex(face));
	}

	@Override
	public final MutableQuadViewImpl colorIndex(int colorIndex) {
		data[baseIndex + HEADER_COLOR_INDEX] = colorIndex;
		return this;
	}

	@Override
	public final MutableQuadViewImpl tag(int tag) {
		data[baseIndex + HEADER_TAG] = tag;
		return this;
	}

	@Override
	public final MutableQuadViewImpl fromVanilla(int[] quadData, int startIndex, boolean isItem) {
		System.arraycopy(quadData, startIndex, data, baseIndex + HEADER_STRIDE, BASE_QUAD_STRIDE);
		invalidateShape();
		return this;
	}

	// TODO: remove?
	public boolean isFaceAligned() {
		return (geometryFlags() & GeometryHelper.AXIS_ALIGNED_FLAG) != 0;
	}

	// TODO: remove?
	public boolean needsDiffuseShading(int textureIndex) {
		return textureIndex == 0 && !material().disableDiffuse(textureIndex);
	}

	@Override
	public MutableQuadViewImpl pos(int vertexIndex, float x, float y, float z) {
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		data[index] = Float.floatToRawIntBits(x);
		data[index + 1] = Float.floatToRawIntBits(y);
		data[index + 2] = Float.floatToRawIntBits(z);
		invalidateShape();
		return this;
	}

	public MutableQuadViewImpl x(int vertexIndex, float x) {
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		data[index] = Float.floatToRawIntBits(x);
		invalidateShape();
		return this;
	}

	public MutableQuadViewImpl y(int vertexIndex, float y) {
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		data[index + 1] = Float.floatToRawIntBits(y);
		invalidateShape();
		return this;
	}

	public MutableQuadViewImpl z(int vertexIndex, float z) {
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		data[index + 2] = Float.floatToRawIntBits(z);
		invalidateShape();
		return this;
	}

	public void normalFlags(int flags) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.normalFlags(data[baseIndex + HEADER_BITS], flags);
	}

	@Override
	public MutableQuadViewImpl normal(int vertexIndex, float x, float y, float z) {
		normalFlags(normalFlags() | (1 << vertexIndex));
		data[baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_NORMAL] = NormalHelper.packNormal(x, y, z, 0);
		return this;
	}

	/**
	 * Internal helper method. Copies face normals to vertex normals lacking one.
	 */
	public final void populateMissingNormals() {
		final int normalFlags = this.normalFlags();

		if (normalFlags == 0b1111) {
			return;
		}

		final int packedFaceNormal = NormalHelper.packNormal(faceNormal(), 0);

		for (int v = 0; v < 4; v++) {
			if ((normalFlags & (1 << v)) == 0) {
				data[baseIndex + v * BASE_VERTEX_STRIDE + VERTEX_NORMAL] = packedFaceNormal;
			}
		}

		normalFlags(0b1111);
	}

	@Override
	public MutableQuadViewImpl lightmap(int vertexIndex, int lightmap) {
		data[baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_LIGHTMAP] = lightmap;
		return this;
	}

	@Override
	public MutableQuadViewImpl spriteColor(int vertexIndex, int spriteIndex, int color) {
		data[baseIndex + colorOffset(vertexIndex, spriteIndex)] = color;
		return this;
	}

	@Override
	public MutableQuadViewImpl sprite(int vertexIndex, int spriteIndex, float u, float v) {
		final int i = baseIndex + colorOffset(vertexIndex, spriteIndex) + 1;
		data[i] = Float.floatToRawIntBits(u);
		data[i + 1] = Float.floatToRawIntBits(v);
		return this;
	}

	@Override
	public MutableQuadViewImpl spriteBake(int spriteIndex, Sprite sprite, int bakeFlags) {
		TextureHelper.bakeSprite(this, spriteIndex, sprite, bakeFlags);
		return this;
	}

	/** avoids call overhead in fallback consumer */
	public void setupVanillaFace(int cullFaceId, int lightFaceId) {
		final int bits = MeshEncodingHelper.cullFace(data[baseIndex + HEADER_BITS], cullFaceId);
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.lightFace(bits, lightFaceId);

		nominalFaceId = lightFaceId;
		isFaceNormalInvalid = true;
		isGeometryInvalid = true;
		packedFaceNormal = -1;
	}
}
