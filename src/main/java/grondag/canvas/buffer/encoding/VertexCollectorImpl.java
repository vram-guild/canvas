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

package grondag.canvas.buffer.encoding;

import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_COLOR_INDEX;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_LIGHT_INDEX;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_MATERIAL_INDEX;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_NORMAL_INDEX;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_QUAD_STRIDE;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_TEXTURE_INDEX;
import static grondag.canvas.buffer.format.CanvasVertexFormats.MATERIAL_VERTEX_STRIDE;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.texture.Sprite;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderStateData;
import grondag.canvas.mixinterface.SpriteExt;

public class VertexCollectorImpl implements VertexCollector {
	public final ClientVertexBuffer vertexArray = new ClientVertexBuffer();

	private static final int LAST_VERTEX_BASE_INDEX = MATERIAL_QUAD_STRIDE - MATERIAL_VERTEX_STRIDE;

	protected RenderMaterialImpl materialState;

	protected int normalBase;
	protected int overlayFlags;
	protected boolean conditionActive = true;
	protected boolean didPopulateNormal = false;
	protected int currentVertexIndex = 0;
	protected final int[] vertexData = new int[CanvasVertexFormats.MATERIAL_QUAD_STRIDE];

	public VertexCollectorImpl prepare(RenderMaterialImpl materialState) {
		clear();
		this.materialState = materialState;
		material(materialState);
		return this;
	}

	public void clear() {
		currentVertexIndex = 0;
		vertexArray.clear();
		didPopulateNormal = false;
	}

	public RenderMaterialImpl materialState() {
		return materialState;
	}

	@Override
	public VertexCollectorImpl clone() {
		throw new UnsupportedOperationException();
	}

	public int[] saveState(int[] priorState) {
		final int integerSize = vertexArray.integerSize();

		if (integerSize == 0) {
			return null;
		}

		int[] result = priorState;

		if (result == null || result.length != integerSize) {
			result = new int[integerSize];
		}

		if (integerSize > 0) {
			System.arraycopy(vertexArray.data(), 0, result, 0, integerSize);
		}

		return result;
	}

	public VertexCollectorImpl loadState(RenderMaterialImpl state, int[] stateData) {
		clear();
		materialState = state;

		if (stateData != null) {
			final int size = stateData.length;
			vertexArray.allocate(size);
			System.arraycopy(stateData, 0, vertexArray.data(), 0, size);
		}

		return this;
	}

	public void draw(boolean clear) {
		if (!vertexArray.isEmpty()) {
			drawSingle();

			if (clear) {
				clear();
			}
		}
	}

	void sortIfNeeded() {
		if (materialState.sorted) {
			vertexArray.sortQuads(0, 0, 0);
		}
	}

	/** Avoid: slow. */
	public void drawSingle() {
		// PERF: allocation - or eliminate this
		final ObjectArrayList<VertexCollectorImpl> drawList = new ObjectArrayList<>();
		drawList.add(this);
		draw(drawList);
	}

	/**
	 * Single-buffer draw, minimizes state changes.
	 * Assumes all collectors are non-empty.
	 */
	public static void draw(ObjectArrayList<VertexCollectorImpl> drawList) {
		final DrawableBuffer buffer = new DrawableBuffer(drawList);
		buffer.draw(false);
		buffer.close();
	}

	protected void emitQuad() {
		if (conditionActive) {
			final int idx = vertexArray.allocate(CanvasVertexFormats.MATERIAL_QUAD_STRIDE);
			System.arraycopy(vertexData, 0, vertexArray.data(), idx, CanvasVertexFormats.MATERIAL_QUAD_STRIDE);
		}

		currentVertexIndex = 0;
	}

	@Override
	public VertexCollector texture(float u, float v) {
		vertexData[currentVertexIndex + MATERIAL_TEXTURE_INDEX] = Float.floatToRawIntBits(u);
		vertexData[currentVertexIndex + MATERIAL_MATERIAL_INDEX] = Float.floatToRawIntBits(v);
		return this;
	}

	@Override
	public VertexCollector overlay(int u, int v) {
		setOverlay(u, v);
		return this;
	}

	@Override
	public VertexCollector overlay(int uv) {
		setOverlay(uv);
		return this;
	}

	protected void setOverlay(int uv) {
		setOverlay(uv & '\uffff', uv >> 16 & '\uffff');
	}

	protected void setOverlay (int u, int v) {
		if (v == 3) {
			// NB: these are pre-shifted to msb
			overlayFlags = RenderStateData.HURT_OVERLAY_FLAG;
		} else if (v == 10) {
			overlayFlags = u > 7 ? RenderStateData.FLASH_OVERLAY_FLAG : 0;
		} else {
			overlayFlags = 0;
		}
	}

	// low to high: block, sky, ao, flags
	@Override
	public VertexCollector light(int block, int sky) {
		setLight(block, sky);
		return this;
	}

	@Override
	public VertexCollector light(int uv) {
		setLight(uv);
		return this;
	}

	protected void setLight(int block, int sky) {
		vertexData[currentVertexIndex + MATERIAL_LIGHT_INDEX] = (block & 0xFF) | ((sky & 0xFF) << 8);
	}

	protected void setLight(int uv) {
		vertexData[currentVertexIndex + MATERIAL_LIGHT_INDEX] = (uv & 0xFF) | ((uv >> 8) & 0xFF00);
	}

	@Override
	public VertexCollector material(RenderMaterial material) {
		final RenderMaterialImpl mat = (RenderMaterialImpl) material;
		assert mat.collectorIndex == materialState.collectorIndex;
		normalBase = (mat.shaderFlags << 24);
		conditionActive = mat.condition().compute();
		return this;
	}

	@Override
	public VertexCollector vertex(float x, float y, float z) {
		vertexData[currentVertexIndex + 0] = Float.floatToRawIntBits(x);
		vertexData[currentVertexIndex + 1] = Float.floatToRawIntBits(y);
		vertexData[currentVertexIndex + 2] = Float.floatToRawIntBits(z);
		return this;
	}

	@Override
	public VertexCollector color(int color) {
		vertexData[currentVertexIndex + MATERIAL_COLOR_INDEX] = color;
		return this;
	}

	@Override
	public void fixedColor(int i, int j, int k, int l) {
		// WIP2: implement
	}

	@Override
	public void method_35666() {
		// WIP2: implement
	}

	@Override
	public VertexCollector normal(float x, float y, float z) {
		setNormal(x, y, z);
		return this;
	}

	protected void setNormal(float x, float y, float z) {
		vertexData[currentVertexIndex + MATERIAL_NORMAL_INDEX] = NormalHelper.packNormal(x, y, z) | normalBase | overlayFlags;
		didPopulateNormal = true;
	}

	// heavily used, so inlined
	@Override
	public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
		vertexData[currentVertexIndex + 0] = Float.floatToRawIntBits(x);
		vertexData[currentVertexIndex + 1] = Float.floatToRawIntBits(y);
		vertexData[currentVertexIndex + 2] = Float.floatToRawIntBits(z);
		vertexData[currentVertexIndex + MATERIAL_COLOR_INDEX] = VertexCollector.packColor(red, green, blue, alpha);
		vertexData[currentVertexIndex + MATERIAL_TEXTURE_INDEX] = Float.floatToRawIntBits(u);
		vertexData[currentVertexIndex + MATERIAL_MATERIAL_INDEX] = Float.floatToRawIntBits(v);
		setOverlay(overlay);
		setLight(light);
		setNormal(normalX, normalY, normalZ);
		next();
	}

	// PERF: MATERIAL_INDEX value never populated or used for non-atlas renders because they
	// are always drawn with uniform program control and don't need sprite ID. Could be removed.
	// However, will mean vertex length is not consistent within the same buffer.
	private void normalizeSprites() {
		if (materialState.texture.isAtlas()) {
			normalizeAtlasSprites();
		} else {
			final float u0 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX]);
			final float v0 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX]);
			vertexData[MATERIAL_TEXTURE_INDEX] = Math.round(u0 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v0 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[MATERIAL_MATERIAL_INDEX] = 0;

			final float u1 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE]);
			final float v1 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE]);
			vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE] = Math.round(u1 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v1 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE] = 0;

			final float u2 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
			final float v2 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
			vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2] = Math.round(u2 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v2 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2] = 0;

			final float u3 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3]);
			final float v3 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3]);
			vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3] = Math.round(u3 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v3 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3] = 0;
		}
	}

	private static final float ONE_THIRD = 1f / 3f;

	private void normalizeAtlasSprites() {
		final float u0 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX]);
		final float v0 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX]);
		final float u1 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE]);
		final float v1 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE]);
		final float u2 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
		final float v2 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
		final float u3 = Float.intBitsToFloat(vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3]);
		final float v3 = Float.intBitsToFloat(vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3]);

		final Sprite sprite = materialState.texture.atlasInfo().spriteFinder().find((u0 + u1 + u2) * ONE_THIRD, (v0 + v1 + v2) * ONE_THIRD);
		final int spriteId = ((SpriteExt) sprite).canvas_id();
		final float uMin = sprite.getMinU();
		final float vMin = sprite.getMinV();
		final float uSpanInv = 1f / (sprite.getMaxU() - uMin);
		final float vSpanInv = 1f / (sprite.getMaxV() - vMin);
		final int stateVec = spriteId | (materialState.index << 16);

		vertexData[MATERIAL_TEXTURE_INDEX] = Math.round((u0 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v0 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
		vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE] = Math.round((u1 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v1 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
		vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2] = Math.round((u2 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v2 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
		vertexData[MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3] = Math.round((u3 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v3 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);

		vertexData[MATERIAL_MATERIAL_INDEX] = stateVec;
		vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE] = stateVec;
		vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2] = stateVec;
		vertexData[MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3] = stateVec;
	}

	@Override
	public void next() {
		if (!didPopulateNormal) {
			normal(0, 1, 0);
		}

		final int base = currentVertexIndex;

		if (base >= LAST_VERTEX_BASE_INDEX) {
			normalizeSprites();
			emitQuad();
		} else {
			currentVertexIndex = base + MATERIAL_VERTEX_STRIDE;
		}

		didPopulateNormal = false;
	}
}
