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

package grondag.canvas.wip.encoding;

import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.wip.state.WipRenderState;
import grondag.canvas.wip.state.WipVertexState;

import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_COLOR_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_LIGHT_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_NORMAL_INDEX;
import static grondag.canvas.material.MaterialVertexFormats.MATERIAL_TEXTURE_INDEX;

import net.minecraft.client.texture.Sprite;

public abstract class WipAbstractVertexCollector implements WipVertexCollector {
	protected WipRenderState materialState;

	protected final int[] vertexData = new int[8];
	protected int vertexIndex = 0;

	protected int lightBase;
	protected int overlayFlags;
	protected int spriteId = -1;
	protected float u0;
	protected float v0;
	protected float uSpanInv;
	protected float vSpanInv;

	public WipVertexCollector sprite(Sprite sprite) {
		if (materialState.texture.isAtlas()) {
			spriteId = ((SpriteExt) sprite).canvas_id();
			u0 = sprite.getMinU();
			v0 = sprite.getMinV();
			uSpanInv = 1f / (sprite.getMaxU() - u0);
			vSpanInv = 1f / (sprite.getMaxV() - v0);
		} else {
			spriteId = -1;
		}

		return this;
	}

	public WipVertexCollector spriteId(int spriteId) {
		// PERF: yeeesh - avoid using this
		if (materialState.texture.isAtlas()) {
			this.spriteId = spriteId;
			final Sprite sprite = materialState.texture.atlasInfo().fromId(spriteId);
			u0 = sprite.getMinU();
			v0 = sprite.getMinV();
			uSpanInv = 1f / (sprite.getMaxU() - u0);
			vSpanInv = 1f / (sprite.getMaxV() - v0);
		} else {
			spriteId = -1;
		}

		return this;
	}

	@Override
	public WipVertexCollector texture(float u, float v) {
		if (spriteId != -1) {
			u = (u - u0) * uSpanInv;
			v = (v - v0) * vSpanInv;
		}

		vertexData[MATERIAL_NORMAL_INDEX] = (vertexData[MATERIAL_NORMAL_INDEX] & 0xFFFF) | (spriteId << 16);
		vertexData[MATERIAL_TEXTURE_INDEX] = Math.round(u * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
		return this;
	}

	@Override
	public WipVertexCollector overlay(int u, int v) {
		if (v == 3) {
			overlayFlags = WipVertexState.HURT_OVERLAY_FLAG;
		} else if (v == 10) {
			overlayFlags = u > 7 ? WipVertexState.FLASH_OVERLAY_FLAG : 0;
		} else {
			overlayFlags = 0;
		}

		return this;
	}

	// low to high: block, sky, ao, flags
	@Override
	public WipVertexCollector light(int block, int sky) {
		vertexData[MATERIAL_LIGHT_INDEX] = lightBase | overlayFlags | (block & 0xFF) | ((sky & 0xFF) << 8);
		return this;
	}

	@Override
	public WipVertexCollector packedLightWithAo(int packedLight, int ao) {
		vertexData[MATERIAL_LIGHT_INDEX] = lightBase | overlayFlags | (packedLight & 0xFF) | ((packedLight >> 8) & 0xFF00) | (ao << 16);
		return this;
	}

	@Override
	public WipVertexCollector vertexState(int vertexState) {
		lightBase = (WipVertexState.shaderFlags(vertexState) << 24);
		return this;
	}

	@Override
	public WipVertexCollector vertex(float x, float y, float z) {
		vertexData[0] = Float.floatToRawIntBits(x);
		vertexData[1] = Float.floatToRawIntBits(y);
		vertexData[2] = Float.floatToRawIntBits(z);
		return this;
	}

	@Override
	public WipVertexCollector color(int color) {
		vertexData[MATERIAL_COLOR_INDEX] = color;
		return this;
	}

	// packed unsigned shorts
	@Override
	public WipVertexCollector texture(int packedTexture) {
		vertexData[MATERIAL_TEXTURE_INDEX] = packedTexture;
		vertexData[MATERIAL_NORMAL_INDEX] = (vertexData[MATERIAL_NORMAL_INDEX] & 0xFFFF) | (spriteId << 16);
		return this;
	}

	@Override
	public WipVertexCollector texture(int packedTexture, int spriteId) {
		vertexData[MATERIAL_TEXTURE_INDEX] = packedTexture;
		vertexData[MATERIAL_NORMAL_INDEX] = (vertexData[MATERIAL_NORMAL_INDEX] & 0xFFFF) | (spriteId << 16);
		return this;
	}

	@Override
	public WipVertexCollector normal(float x, float y, float z) {
		// PERF: don't need to pack z
		vertexData[MATERIAL_NORMAL_INDEX] = (NormalHelper.packUnsignedNormal(x, y, z) & 0xFFFF) | (spriteId << 16);
		return this;
	}
}
