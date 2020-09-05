package grondag.canvas.apiimpl.mesh;

import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.BASE_QUAD_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.BASE_VERTEX_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.EMPTY;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_BITS;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_MATERIAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_STRIDE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.HEADER_TAG;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.UV_PRECISE_UNIT_VALUE;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_NORMAL;
import static grondag.canvas.apiimpl.mesh.MeshEncodingHelper.VERTEX_X;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.Canvas;
import grondag.canvas.apiimpl.material.AbstractMeshMaterial;
import grondag.canvas.apiimpl.material.MeshMaterialLocator;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.apiimpl.util.TextureHelper;
import grondag.canvas.light.LightmapHd;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.texture.SpriteInfoTexture;

/**
 * Almost-concrete implementation of a mutable quad. The only missing part is {@link #emit()},
 * because that depends on where/how it is used. (Mesh encoding vs. render-time transformation).
 */
public abstract class MutableQuadViewImpl extends QuadViewImpl implements QuadEmitter {
	// UGLY - need a lighting result class?
	public LightmapHd hdLight = null;
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

	/**
	 * Call before emit or mesh incorporation.
	 */
	public final void complete() {
		computeGeometry();
		unmapSpritesIfNeeded();
	}

	public void clear() {
		System.arraycopy(EMPTY, 0, data, baseIndex, MeshEncodingHelper.MAX_QUAD_STRIDE);
		isGeometryInvalid = true;
		packedFaceNormal = -1;
		nominalFaceId = ModelHelper.NULL_FACE_ID;
		normalFlags(0);
		// tag(0); seems redundant - handled by array copy
		colorIndex(-1);
		cullFace(null);
		material(Canvas.MATERIAL_STANDARD);
		spriteMappedFlags = 0;
	}

	@Override
	public final MutableQuadViewImpl material(RenderMaterial material) {
		if (material == null) {
			material = Canvas.MATERIAL_STANDARD;
		}

		data[baseIndex + HEADER_MATERIAL] = ((MeshMaterialLocator)material).index();

		assert AbstractMeshMaterial.byIndex(data[baseIndex + HEADER_MATERIAL]) == material;

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

	private void convertVanillaUvPrecision() {
		// Convert sprite data from float to fixed precision
		int index = baseIndex + colorOffset(0, 0) + 1;

		for (int i = 0; i < 4; ++i)  {
			data[index] = (int) (Float.intBitsToFloat(data[index]) * UV_PRECISE_UNIT_VALUE);
			data[index + 1] = (int) (Float.intBitsToFloat(data[index + 1]) * UV_PRECISE_UNIT_VALUE);
			index += BASE_VERTEX_STRIDE;
		}
	}

	@Deprecated
	@Override
	public final MutableQuadViewImpl fromVanilla(int[] quadData, int startIndex, boolean isItem) {
		System.arraycopy(quadData, startIndex, data, baseIndex + HEADER_STRIDE, BASE_QUAD_STRIDE);
		convertVanillaUvPrecision();
		unmapSprite(0);
		spriteMappedFlags = 0;

		isGeometryInvalid = true;
		packedFaceNormal = -1;
		return this;
	}

	public final MutableQuadViewImpl fromVanilla(BakedQuad quad, RenderMaterial material, Direction cullFace) {
		return fromVanilla(quad, material, ModelHelper.toFaceIndex(cullFace));
	}

	public final MutableQuadViewImpl fromVanilla(BakedQuad quad, RenderMaterial material, int cullFaceId) {
		System.arraycopy(quad.getVertexData(), 0, data, baseIndex + HEADER_STRIDE, BASE_QUAD_STRIDE);
		convertVanillaUvPrecision();
		unmapSprite(0);
		spriteMappedFlags = 0;
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.cullFace(0, cullFaceId);
		nominalFaceId = ModelHelper.toFaceIndex(quad.getFace());
		data[baseIndex + HEADER_COLOR_INDEX] = quad.getColorIndex();
		data[baseIndex + HEADER_TAG] = 0;
		material(material);
		isGeometryInvalid = true;
		packedFaceNormal = -1;
		return this;
	}

	@Override
	public MutableQuadViewImpl pos(int vertexIndex, float x, float y, float z) {
		final int index = baseIndex + vertexIndex * BASE_VERTEX_STRIDE + VERTEX_X;
		data[index] = Float.floatToRawIntBits(x);
		data[index + 1] = Float.floatToRawIntBits(y);
		data[index + 2] = Float.floatToRawIntBits(z);
		isGeometryInvalid = true;
		packedFaceNormal = -1;
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

	public final void setSpriteUnmapped(int spriteIndex, boolean isNormalized) {
		final int mask = 1 << spriteIndex;

		if (isNormalized) {
			spriteMappedFlags &= ~mask;
		} else {
			spriteMappedFlags |= mask;
		}
	}

	public MutableQuadViewImpl spriteFloat(int vertexIndex, int spriteIndex, float u, float v) {
		final int i = baseIndex + colorOffset(vertexIndex, spriteIndex) + 1;
		data[i] = (int) (u * UV_PRECISE_UNIT_VALUE + 0.5f);
		data[i + 1] = (int) (v * UV_PRECISE_UNIT_VALUE + 0.5f);
		return this;
	}

	/**
	 * Must call {@link #spriteId(int, int)} separately.
	 */
	public MutableQuadViewImpl spritePrecise(int vertexIndex, int spriteIndex, int u, int v) {
		final int i = baseIndex + colorOffset(vertexIndex, spriteIndex) + 1;
		data[i] = u;
		data[i + 1] = v;
		assert isSpriteUnmapped(spriteIndex);
		return this;
	}

	public void unmapSpritesIfNeeded() {
		if (spriteMappedFlags != 0) {
			if ((spriteMappedFlags & 1) == 1) {
				unmapSprite(0);
			}

			if ((spriteMappedFlags & 2) == 2) {
				unmapSprite(1);
			}

			if ((spriteMappedFlags & 4) == 4) {
				unmapSprite(2);
			}

			spriteMappedFlags = 0;
		}
	}

	private void unmapSprite(int textureIndex) {
		final Sprite sprite = findSprite(textureIndex);
		final int spriteId = ((SpriteExt) sprite).canvas_id();
		final float u0 = sprite.getMinU();
		final float v0 = sprite.getMinV();
		final float uSpanInv = 1f / (sprite.getMaxU() - u0);
		final float vSpanInv = 1f / (sprite.getMaxV() - v0);

		spriteFloat(0, textureIndex, (spriteFloatU(0, textureIndex) - u0) * uSpanInv, (spriteFloatV(0, textureIndex) - v0) * vSpanInv);
		spriteFloat(1, textureIndex, (spriteFloatU(1, textureIndex) - u0) * uSpanInv, (spriteFloatV(1, textureIndex) - v0) * vSpanInv);
		spriteFloat(2, textureIndex, (spriteFloatU(2, textureIndex) - u0) * uSpanInv, (spriteFloatV(2, textureIndex) - v0) * vSpanInv);
		spriteFloat(3, textureIndex, (spriteFloatU(3, textureIndex) - u0) * uSpanInv, (spriteFloatV(3, textureIndex) - v0) * vSpanInv);
		spriteId(textureIndex, spriteId);
	}

	/**
	 * Same as logic in SpriteFinder but can assume sprites are mapped - avoids checks
	 */
	@SuppressWarnings("resource")
	private Sprite findSprite(int textureIndex) {
		float u = 0;
		float v = 0;

		for (int i = 0; i < 4; i++) {
			u += spriteFloatU(i, textureIndex);
			v += spriteFloatV(i, textureIndex);
		}

		return SpriteInfoTexture.instance().spriteFinder.find(u * 0.25f, v * 0.25f);
	}

	@Override
	public MutableQuadViewImpl sprite(int vertexIndex, int spriteIndex, float u, float v) {
		spriteFloat(vertexIndex, spriteIndex, u, v);

		// true for whole quad so only need for one vertex
		if (vertexIndex == 0) {
			setSpriteUnmapped(spriteIndex, false);
		} else {
			assert !isSpriteUnmapped(spriteIndex);
		}

		return this;
	}

	@Override
	public MutableQuadViewImpl spriteBake(int spriteIndex, Sprite sprite, int bakeFlags) {
		TextureHelper.bakeSprite(this, spriteIndex, sprite, bakeFlags);
		return this;
	}

	public MutableQuadViewImpl spriteId(int spriteIndex, int spriteId) {
		final int index  = spriteIdOffset(spriteIndex);
		final int d = data[index];
		data[index] = (spriteIndex & 1) == 0 ? (d & 0xFFFF0000) | spriteId : (d & 0xFFFF) | (spriteId << 16);
		return this;
	}
}
