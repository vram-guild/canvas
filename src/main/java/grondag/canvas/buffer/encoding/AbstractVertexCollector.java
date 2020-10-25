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

package grondag.canvas.buffer.encoding;

import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.material.state.RenderContextState;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.material.state.RenderStateData;
import grondag.canvas.mixinterface.SpriteExt;

import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_COLOR_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_LIGHT_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_MATERIAL_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_NORMAL_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_QUAD_STRIDE;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_TEXTURE_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_VERTEX_STRIDE;

import net.minecraft.client.texture.Sprite;

public abstract class AbstractVertexCollector implements VertexCollector {
	private static final int LAST_VERTEX_BASE_INDEX = MATERIAL_QUAD_STRIDE - MATERIAL_VERTEX_STRIDE;

	protected RenderMaterialImpl materialState;
	protected final RenderContextState contextState;

	protected final int[] vertexData = new int[MATERIAL_QUAD_STRIDE];
	protected int baseVertexIndex = 0;
	protected int firstVertexIndex = 0;

	protected int normalBase;
	protected int overlayFlags;
	protected boolean conditionActive = true;
	protected boolean didPopulateNormal = false;

	public AbstractVertexCollector(RenderContextState contextState) {
		this.contextState = contextState;
	}

	@Override
	public VertexCollector texture(float u, float v) {
		vertexData[baseVertexIndex + MATERIAL_TEXTURE_INDEX] = Float.floatToRawIntBits(u);
		vertexData[baseVertexIndex + MATERIAL_MATERIAL_INDEX] = Float.floatToRawIntBits(v);
		return this;
	}

	@Override
	public VertexCollector overlay(int u, int v) {
		if (v == 3) {
			// NB: these are pre-shifted to msb
			overlayFlags = RenderStateData.HURT_OVERLAY_FLAG;
		} else if (v == 10) {
			overlayFlags = u > 7 ? RenderStateData.FLASH_OVERLAY_FLAG : 0;
		} else {
			overlayFlags = 0;
		}

		return this;
	}

	// low to high: block, sky, ao, flags
	@Override
	public VertexCollector light(int block, int sky) {
		vertexData[baseVertexIndex + MATERIAL_LIGHT_INDEX] = (block & 0xFF) | ((sky & 0xFF) << 8);
		return this;
	}

	@Override
	public VertexCollector packedLightWithAo(int packedLight, int ao) {
		vertexData[baseVertexIndex + MATERIAL_LIGHT_INDEX] = (packedLight & 0xFF) | ((packedLight >> 8) & 0xFF00) | (ao << 16);
		return this;
	}

	@Override
	public VertexCollector vertexState(RenderMaterialImpl material) {
		// WIP2: should assert collector key doesn't change here but not currently visible
		normalBase = (material.shaderFlags << 24);
		conditionActive = material.condition().compute();
		return this;
	}

	@Override
	public VertexCollector vertex(float x, float y, float z) {
		vertexData[baseVertexIndex + 0] = Float.floatToRawIntBits(x);
		vertexData[baseVertexIndex + 1] = Float.floatToRawIntBits(y);
		vertexData[baseVertexIndex + 2] = Float.floatToRawIntBits(z);
		return this;
	}

	@Override
	public VertexCollector color(int color) {
		vertexData[baseVertexIndex + MATERIAL_COLOR_INDEX] = color;
		return this;
	}

	@Override
	public VertexCollector normal(float x, float y, float z) {
		vertexData[baseVertexIndex + MATERIAL_NORMAL_INDEX] = NormalHelper.packNormal(x, y, z) | normalBase | overlayFlags;
		didPopulateNormal = true;
		return this;
	}

	private void normalizeSprites() {
		if (materialState.texture.isAtlas()) {
			normalizeAtlasSprites();
		} else {
			final float u0 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX]);
			final float v0 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX]);
			vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX] = Math.round(u0 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v0 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX] = 0;

			final float u1 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE]);
			final float v1 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE]);
			vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE] = Math.round(u1 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v1 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE] = 0;

			final float u2 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
			final float v2 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
			vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2] = Math.round(u2 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v2 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2] = 0;

			final float u3 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3]);
			final float v3 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3]);
			vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3] =Math.round(u3 * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v3 * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
			vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3] = 0;
		}
	}

	private static final float ONE_THIRD = 1f / 3f;

	private void normalizeAtlasSprites() {
		final float u0 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX]);
		final float v0 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX]);
		final float u1 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE]);
		final float v1 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE]);
		final float u2 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
		final float v2 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2]);
		final float u3 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3]);
		final float v3 = Float.intBitsToFloat(vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3]);

		final Sprite sprite = materialState.texture.atlasInfo().spriteFinder().find((u0 + u1 + u2) * ONE_THIRD, (v0 + v1 + v2) * ONE_THIRD);
		final int spriteId = ((SpriteExt) sprite).canvas_id();
		final float uMin = sprite.getMinU();
		final float vMin = sprite.getMinV();
		final float uSpanInv = 1f / (sprite.getMaxU() - uMin);
		final float vSpanInv = 1f / (sprite.getMaxV() - vMin);

		vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX] = Math.round((u0 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v0 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
		vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE] = Math.round((u1 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v1 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
		vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 2] = Math.round((u2 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v2 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
		vertexData[firstVertexIndex + MATERIAL_TEXTURE_INDEX + MATERIAL_VERTEX_STRIDE * 3] = Math.round((u3 - uMin) * uSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round((v3 - vMin) * vSpanInv * MeshEncodingHelper.UV_UNIT_VALUE) << 16);

		vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX] = spriteId;
		vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE] = spriteId;
		vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 2] = spriteId;
		vertexData[firstVertexIndex + MATERIAL_MATERIAL_INDEX + MATERIAL_VERTEX_STRIDE * 3] = spriteId;
	}

	@Override
	public void next() {
		if (!didPopulateNormal) {
			normal(0, 1, 0);
		}

		final int base = baseVertexIndex;

		if (base == LAST_VERTEX_BASE_INDEX) {
			baseVertexIndex = 0;
			normalizeSprites();
			emitQuad();
		} else {
			baseVertexIndex = base + MATERIAL_VERTEX_STRIDE;
		}

		didPopulateNormal = false;
	}

	protected abstract void emitQuad();
}
