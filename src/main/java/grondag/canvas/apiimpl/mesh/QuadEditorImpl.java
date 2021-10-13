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

import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.EMPTY;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_BITS;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_MATERIAL;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_SPRITE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_TAG;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_QUAD_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE_SHIFT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_PRECISE_UNIT_VALUE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_NORMAL0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_U0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_X0;

import org.jetbrains.annotations.Nullable;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import io.vram.frex.api.buffer.FrexVertexConsumer;
import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.model.util.FaceUtil;
import io.vram.frex.api.model.util.PackedVector3f;
import io.vram.frex.base.renderer.mesh.MeshEncodingHelper;
import io.vram.frex.impl.texture.IndexedSprite;

import grondag.canvas.apiimpl.util.TextureHelper;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

/**
 * Almost-concrete implementation of a mutable quad. The only missing part is {@link #emit()},
 * because that depends on where/how it is used. (Mesh encoding vs. render-time transformation).
 */
public abstract class QuadEditorImpl extends QuadViewImpl implements QuadEmitter, FrexVertexConsumer {
	// PERF: pack into one array for LOR?
	public final float[] u = new float[4];
	public final float[] v = new float[4];
	// vanilla light outputs
	// PERF use integer byte values for these instead of floats
	public final float[] ao = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
	protected RenderMaterial defaultMaterial = RenderMaterial.defaultMaterial();

	private int vertexIndex = 0;

	public final void begin(int[] data, int baseIndex) {
		this.data = data;
		this.baseIndex = baseIndex;
		clear();
	}

	@Override
	public QuadEditorImpl defaultMaterial(RenderMaterial defaultMaterial) {
		this.defaultMaterial = defaultMaterial;
		return this;
	}

	/**
	 * Call before emit or mesh incorporation.
	 */
	public final void complete() {
		computeGeometry();
		packedFaceTanget();
		normalizeSpritesIfNeeded();
		vertexIndex = 0;
	}

	public void clear() {
		System.arraycopy(EMPTY, 0, data, baseIndex, MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE);
		isGeometryInvalid = true;
		isTangentInvalid = true;
		nominalFaceId = FaceUtil.UNASSIGNED_INDEX;
		material(defaultMaterial);
		isSpriteInterpolated = false;
		vertexIndex = 0;
	}

	@Override
	public final QuadEditorImpl material(RenderMaterial material) {
		if (material == null) {
			material = defaultMaterial;
		}

		data[baseIndex + HEADER_MATERIAL] = material.index();
		return this;
	}

	public final QuadEditorImpl cullFace(int faceId) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.cullFace(data[baseIndex + HEADER_BITS], faceId);
		nominalFaceId = faceId;
		return this;
	}

	@Override
	@Deprecated
	public final QuadEditorImpl cullFace(Direction face) {
		return cullFace(FaceUtil.toFaceIndex(face));
	}

	public final QuadEditorImpl nominalFace(int faceId) {
		nominalFaceId = faceId;
		return this;
	}

	@Override
	@Deprecated
	public final QuadEditorImpl nominalFace(Direction face) {
		return nominalFace(FaceUtil.toFaceIndex(face));
	}

	@Override
	public final QuadEditorImpl colorIndex(int colorIndex) {
		data[baseIndex + HEADER_COLOR_INDEX] = colorIndex;
		return this;
	}

	@Override
	public final QuadEditorImpl tag(int tag) {
		data[baseIndex + HEADER_TAG] = tag;
		return this;
	}

	private void convertVanillaUvPrecision() {
		// Convert sprite data from float to fixed precision
		int index = baseIndex + 0 * MESH_VERTEX_STRIDE + VERTEX_COLOR0 + 1;

		for (int i = 0; i < 4; ++i) {
			data[index] = (int) (Float.intBitsToFloat(data[index]) * UV_PRECISE_UNIT_VALUE);
			data[index + 1] = (int) (Float.intBitsToFloat(data[index + 1]) * UV_PRECISE_UNIT_VALUE);
			index += MESH_VERTEX_STRIDE;
		}
	}

	@Override
	public QuadEmitter fromVanilla(int[] quadData, int startIndex) {
		System.arraycopy(quadData, startIndex, data, baseIndex + HEADER_STRIDE, MESH_QUAD_STRIDE);
		convertVanillaUvPrecision();
		normalizeSprite();
		isSpriteInterpolated = false;
		isGeometryInvalid = true;
		isTangentInvalid = true;
		return this;
	}

	@Override
	public final QuadEditorImpl fromVanilla(BakedQuad quad, RenderMaterial material, Direction cullFace) {
		return fromVanilla(quad, material, FaceUtil.toFaceIndex(cullFace));
	}

	@Override
	public final QuadEditorImpl fromVanilla(BakedQuad quad, RenderMaterial material, int cullFaceId) {
		System.arraycopy(quad.getVertices(), 0, data, baseIndex + HEADER_STRIDE, MESH_QUAD_STRIDE);
		material(material);
		convertVanillaUvPrecision();
		normalizeSprite();
		isSpriteInterpolated = false;
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.cullFace(0, cullFaceId);
		nominalFaceId = FaceUtil.toFaceIndex(quad.getDirection());
		data[baseIndex + HEADER_COLOR_INDEX] = quad.getTintIndex();
		data[baseIndex + HEADER_TAG] = 0;
		isGeometryInvalid = true;
		isTangentInvalid = true;
		return this;
	}

	@Override
	public QuadEditorImpl pos(int vertexIndex, float x, float y, float z) {
		final int index = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_X0;
		data[index] = Float.floatToRawIntBits(x);
		data[index + 1] = Float.floatToRawIntBits(y);
		data[index + 2] = Float.floatToRawIntBits(z);
		isGeometryInvalid = true;
		return this;
	}

	public void normalFlags(int flags) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.normalFlags(data[baseIndex + HEADER_BITS], flags);
	}

	@Override
	public QuadEditorImpl normal(int vertexIndex, float x, float y, float z) {
		normalFlags(normalFlags() | (1 << vertexIndex));
		data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0] = PackedVector3f.pack(x, y, z);
		return this;
	}

	public void tangentFlags(int flags) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.tangentFlags(data[baseIndex + HEADER_BITS], flags);
	}

	@Override
	public QuadEditorImpl tangent(int vertexIndex, float x, float y, float z) {
		tangentFlags(tangentFlags() | (1 << vertexIndex));
		data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT] = PackedVector3f.pack(x, y, z);
		return this;
	}

	@Override
	public QuadEditorImpl lightmap(int vertexIndex, int lightmap) {
		data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_LIGHTMAP0] = lightmap;
		return this;
	}

	@Override
	public QuadEditorImpl vertexColor(int vertexIndex, int color) {
		data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_COLOR0] = color;
		return this;
	}

	public final void setSpriteNormalized() {
		isSpriteInterpolated = false;
	}

	public QuadEditorImpl spriteFloat(int vertexIndex, float u, float v) {
		final int i = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0;
		data[i] = (int) (u * UV_PRECISE_UNIT_VALUE + 0.5f);
		data[i + 1] = (int) (v * UV_PRECISE_UNIT_VALUE + 0.5f);
		isTangentInvalid = true;
		return this;
	}

	/**
	 * Must call {@link #spriteId(int, int)} separately.
	 */
	public QuadEditorImpl spritePrecise(int vertexIndex, int u, int v) {
		final int i = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0;
		data[i] = u;
		data[i + 1] = v;
		isTangentInvalid = true;
		assert !isSpriteInterpolated;
		return this;
	}

	public void normalizeSpritesIfNeeded() {
		if (isSpriteInterpolated) {
			normalizeSprite();
			isSpriteInterpolated = false;
		}
	}

	private void normalizeSprite() {
		if (material().texture().isAtlas()) {
			final TextureAtlasSprite sprite = findSprite();
			final int spriteId = ((IndexedSprite) sprite).frex_index();
			final float u0 = sprite.getU0();
			final float v0 = sprite.getV0();
			final float uSpanInv = 1f / (sprite.getU1() - u0);
			final float vSpanInv = 1f / (sprite.getV1() - v0);

			spriteFloat(0, (spriteFloatU(0) - u0) * uSpanInv, (spriteFloatV(0) - v0) * vSpanInv);
			spriteFloat(1, (spriteFloatU(1) - u0) * uSpanInv, (spriteFloatV(1) - v0) * vSpanInv);
			spriteFloat(2, (spriteFloatU(2) - u0) * uSpanInv, (spriteFloatV(2) - v0) * vSpanInv);
			spriteFloat(3, (spriteFloatU(3) - u0) * uSpanInv, (spriteFloatV(3) - v0) * vSpanInv);
			spriteId(spriteId);
		} else {
			assert false : "Attempt to normalize non-atlas sprite coordinates.";
		}
	}

	/**
	 * Same as logic in SpriteFinder but can assume sprites are mapped - avoids checks.
	 */
	private TextureAtlasSprite findSprite() {
		float u = 0;
		float v = 0;

		for (int i = 0; i < 4; i++) {
			u += spriteFloatU(i);
			v += spriteFloatV(i);
		}

		final TextureAtlasSprite result = material().texture().spriteFinder().find(u * 0.25f, v * 0.25f);

		// Handle bug in SpriteFinder that can return sprite for the wrong atlas
		if (result instanceof MissingTextureAtlasSprite) {
			return material().texture().textureAsAtlas().getSprite(MissingTextureAtlasSprite.getLocation());
		} else {
			return result;
		}
	}

	@Override
	public QuadEditorImpl uvSprite(@Nullable TextureAtlasSprite sprite, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
		isSpriteInterpolated = false;
		spriteFloat(0, u0, v0);
		spriteFloat(1, u1, v1);
		spriteFloat(2, u2, v2);
		spriteFloat(3, u3, v3);

		if (sprite != null) {
			assert material().texture().isAtlas() && material().texture().textureAsAtlas() == sprite.atlas();
			spriteId(((IndexedSprite) sprite).frex_index());
		}

		return this;
	}

	@Override
	public QuadEditorImpl uv(int vertexIndex, float u, float v) {
		// This legacy method accepts interpolated coordinates
		// and so any usage forces us to de-normalize if we are not already.
		// Otherwise any subsequent reads or transformations could be inconsistent.

		if (!isSpriteInterpolated) {
			final var mat = material();

			if (mat.texture().isAtlas()) {
				final var atlasInfo = material().texture().spriteIndex();
				final var spriteId = spriteId();

				for (int i = 0; i < 4; ++i) {
					spriteFloat(i, atlasInfo.mapU(spriteId, spriteFloatU(i)), atlasInfo.mapV(spriteId, spriteFloatV(i)));
				}

				isSpriteInterpolated = true;
			}
		}

		spriteFloat(vertexIndex, u, v);
		return this;
	}

	@Override
	public QuadEditorImpl spriteBake(TextureAtlasSprite sprite, int bakeFlags) {
		TextureHelper.bakeSprite(this, sprite, bakeFlags);
		return this;
	}

	public QuadEditorImpl spriteId(int spriteId) {
		data[baseIndex + HEADER_SPRITE] = spriteId;
		return this;
	}

	@Override
	public void endVertex() {
		// NB: We don't worry about triangles here because we only
		// use this for API calls (which accept quads) or to transcode
		// render layers that have quads as the primitive type.

		// Auto-emit when we finish a quad.
		// NB: emit will call complete which will set vertex index to zero
		if (vertexIndex == 3) {
			emit();
		} else {
			++vertexIndex;
		}
	}

	@Override
	public void defaultColor(int i, int j, int k, int l) {
		// Mojang currently only uses this for outline rendering and
		// also it would be needlessly complicated to implement here.
		// We only render quads so should never see it.
		assert false : "fixedColor call encountered in quad rendering";
	}

	@Override
	public void unsetDefaultColor() {
		// Mojang currently only uses this for outline rendering and
		// also it would be needlessly complicated to implement here.
		// We only render quads so should never see it.
		assert false : "unfixColor call encountered in quad rendering";
	}

	@Override
	public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
		vertex(x, y, z);
		color(MeshEncodingHelper.packColor(red, green, blue, alpha));
		uv(u, v);
		setOverlay(overlay);
		uv2(light);
		normal(normalX, normalY, normalZ);
		endVertex();
	}

	@Override
	public FrexVertexConsumer vertex(float x, float y, float z) {
		pos(vertexIndex, x, y, z);
		return this;
	}

	@Override
	public FrexVertexConsumer color(int color) {
		vertexColor(vertexIndex, color);
		return this;
	}

	@Override
	public FrexVertexConsumer uv(float u, float v) {
		uv(vertexIndex, u, v);
		return this;
	}

	@Override
	public FrexVertexConsumer overlayCoords(int u, int v) {
		setOverlay(u, v);
		return this;
	}

	@Override
	public FrexVertexConsumer overlayCoords(int uv) {
		setOverlay(uv);
		return this;
	}

	protected void setOverlay(int uv) {
		setOverlay(uv & '\uffff', uv >> 16 & '\uffff');
	}

	protected void setOverlay (int u, int v) {
		final var mat = material();
		final var oMat = mat.withOverlay(u, v);

		if (oMat != mat) {
			material(oMat);
		}
	}

	@Override
	public FrexVertexConsumer uv2(int block, int sky) {
		this.lightmap(vertexIndex, (block & 0xFF) | ((sky & 0xFF) << 8));
		return this;
	}

	@Override
	public FrexVertexConsumer uv2(int lightmap) {
		this.lightmap(vertexIndex, lightmap);
		return this;
	}

	@Override
	public FrexVertexConsumer normal(float x, float y, float z) {
		this.normal(vertexIndex, x, y, z);
		return this;
	}

	@Override
	public FrexVertexConsumer color(int red, int green, int blue, int alpha) {
		return color(MeshEncodingHelper.packColor(red, green, blue, alpha));
	}

	@Override
	public FrexVertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
		final Matrix4fExt mat = (Matrix4fExt) (Object) matrix;

		final float tx = mat.m00() * x + mat.m01() * y + mat.m02() * z + mat.m03();
		final float ty = mat.m10() * x + mat.m11() * y + mat.m12() * z + mat.m13();
		final float tz = mat.m20() * x + mat.m21() * y + mat.m22() * z + mat.m23();

		return this.vertex(tx, ty, tz);
	}

	@Override
	public FrexVertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
		final Matrix3fExt mat = (Matrix3fExt) (Object) matrix;

		final float tx = mat.m00() * x + mat.m01() * y + mat.m02() * z;
		final float ty = mat.m10() * x + mat.m11() * y + mat.m12() * z;
		final float tz = mat.m20() * x + mat.m21() * y + mat.m22() * z;

		return this.normal(tx, ty, tz);
	}
}
