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

package grondag.canvas.shader.wip.encoding;

import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.shader.wip.WipRenderState;
import grondag.canvas.shader.wip.WipVertexState;

import net.minecraft.client.render.VertexConsumer;

public abstract class WipVertexAdapter implements WipVertexCollector {
	protected WipRenderState materialState;

	protected final int[] vertexData = new int[8];
	protected int colorIndex;
	protected int textureIndex;
	protected int materialIndex;
	protected int lightIndex;
	protected int normalIndex;

	protected WipVertexPacker packer;

	@Override
	public VertexConsumer texture(float u, float v) {
		// WIP - detect texture atlas and derive textureID when used by vanilla suppliers
		return texture(Math.round(u * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v * MeshEncodingHelper.UV_UNIT_VALUE) << 16));
	}

	@Override
	public VertexConsumer overlay(int u, int v) {
		// WIP - set vertex state flags depending on values
		return this;
	}

	// low to high: block, sky, ao, <blank>
	@Override
	public VertexConsumer light(int block, int sky) {
		vertexData[lightIndex] = (block & 0xFF) | ((sky & 0xFF) << 8);
		return this;
	}

	@Override
	public WipVertexCollector packedLightWithAo(int packedLight, int ao) {
		vertexData[lightIndex] = (packedLight & 0xFF) | ((packedLight >> 8) & 0xFF00) | (ao << 16);
		return this;
	}

	@Override
	public WipVertexCollector vertexState(int vertexState) {
		vertexData[materialIndex] = (vertexData[materialIndex] & 0x0000FFFF) | (WipVertexState.conditionIndex(vertexState) << 16);
		vertexData[normalIndex] = (vertexData[normalIndex] & 0x00FFFFFF) | (WipVertexState.shaderFlags(vertexState) << 24);
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
		vertexData[colorIndex] = color;
		return this;
	}

	// packed unsigned shorts
	@Override
	public WipVertexCollector texture(int packedTexture) {
		vertexData[textureIndex] = packedTexture;
		return this;
	}

	@Override
	public WipVertexCollector texture(int packedTexture, int spriteId) {
		vertexData[textureIndex] = packedTexture;
		vertexData[materialIndex] = (vertexData[materialIndex] & 0xFFFF0000) | spriteId;
		return this;
	}

	@Override
	public WipVertexCollector packedNormal(int packedNormal) {
		// PERF: change default normal packing to unsigned when signed format no longer used
		vertexData[normalIndex] = (vertexData[normalIndex] & 0xFF000000) | NormalHelper.repackToUnsigned(packedNormal);
		return this;
	}


}
