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

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

// WIP2: move this to FREX
public interface FrexVertexConsumer extends VertexConsumer {
	/**
	 * Sets state to be included with normals and material if they are included.  Call once
	 * whenever material changes, including default state or revert
	 * to default state of the render state.
	 */
	FrexVertexConsumer material(RenderMaterial material);

	FrexVertexConsumer defaultMaterial(RenderMaterial material);

	FrexVertexConsumer vertex(float x, float y, float z);

	/**
	 * @param color rgba - alpha is high byte, red and blue pre-swapped if needed
	 */
	FrexVertexConsumer color(int color);

	@Override
	default FrexVertexConsumer vertex(double x, double y, double z) {
		vertex((float) x, (float) y, (float) z);
		return this;
	}

	@Override
	default FrexVertexConsumer color(int red, int green, int blue, int alpha) {
		return color(packColor(red, green, blue, alpha));
	}

	static int packNormalizedUV(float u, float v) {
		return Math.round(u * MeshEncodingHelper.UV_UNIT_VALUE) | (Math.round(v * MeshEncodingHelper.UV_UNIT_VALUE) << 16);
	}

	static int packColor(int red, int green, int blue, int alpha) {
		return red | (green << 8) | (blue << 16) | (alpha << 24);
	}

	static int packColor(float red, float green, float blue, float alpha) {
		return packColor((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
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

	@Override
	default FrexVertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
		final Matrix4fExt mat = (Matrix4fExt) (Object) matrix;

		final float tx = mat.a00() * x + mat.a01() * y + mat.a02() * z + mat.a03();
		final float ty = mat.a10() * x + mat.a11() * y + mat.a12() * z + mat.a13();
		final float tz = mat.a20() * x + mat.a21() * y + mat.a22() * z + mat.a23();

		return this.vertex(tx, ty, tz);
	}

	@Override
	default FrexVertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
		final Matrix3fExt mat = (Matrix3fExt) (Object) matrix;

		final float tx = mat.a00() * x + mat.a01() * y + mat.a02() * z;
		final float ty = mat.a10() * x + mat.a11() * y + mat.a12() * z;
		final float tz = mat.a20() * x + mat.a21() * y + mat.a22() * z;

		return this.normal(tx, ty, tz);
	}

	@Override
	FrexVertexConsumer texture(float u, float v);

	@Override
	FrexVertexConsumer overlay(int u, int v);

	@Override
	FrexVertexConsumer light(int u, int v);

	@Override
	FrexVertexConsumer normal(float x, float y, float z);

	int VANILLA_FULL_BRIGHTNESS = 0xF000F0;
}
