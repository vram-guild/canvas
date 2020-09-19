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

import net.minecraft.client.render.VertexConsumer;

public interface WipVertexCollector extends VertexConsumer {
	/**
	 * Does not output any vertex data - sets state to be included
	 * with normals and material if they are included.  Call once
	 * whenever material changes, including default state or revert
	 * to default state of the render state.
	 */
	WipVertexCollector vertexState(int vertexState);

	WipVertexCollector vertex(float x, float y, float z);

	/**
	 * @param color rgba - alpha is high byte, red and blue pre-swapped if needed
	 */
	WipVertexCollector color(int color);

	/**
	 *
	 * @param packedTexture 2 unsigned shorts, relative to the sprite if texture is an atlas, u low, v high
	 */
	WipVertexCollector texture(int packedTexture);

	WipVertexCollector texture(int packedTexture, int spriteId);

	/**
	 *
	 * @param packedLight standard packed lightmap
	 * @param ao 0-255
	 * @return
	 */
	WipVertexCollector packedLightWithAo(int packedLight, int ao);

	@Override
	default WipVertexCollector vertex(double x, double y, double z) {
		vertex((float) x, (float) y, (float) z);
		return this;
	}

	@Override
	default WipVertexCollector color(int red, int green, int blue, int alpha) {
		return color(red | (green << 8) | (blue << 16) | (alpha << 24));
	}

	static int packNormalizedUV(float u, float v) {
		return Math.round(u * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
	}

	int NORMALIZED_U0_V0 = packNormalizedUV(0, 0);
	int NORMALIZED_U0_V1 = packNormalizedUV(0, 1);
	int NORMALIZED_U1_V0 = packNormalizedUV(1, 0);
	int NORMALIZED_U1_V1 = packNormalizedUV(1, 1);

	static int packColorFromFloats(float red, float green, float blue, float alpha) {
		return packColorFromBytes((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
	}

	static int packColorFromBytes(int red, int green, int blue, int alpha) {
		return red | (green << 8) | (blue << 16) | (alpha << 24);
	}
}
