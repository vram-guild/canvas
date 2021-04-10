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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.texture.SpriteInfoTexture;

public abstract class EncoderUtils {
	public static final int FULL_BRIGHTNESS = 0xF000F0;

	public static void bufferQuad(MutableQuadViewImpl quad, AbstractRenderContext context, VertexConsumer buff) {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();

		int packedNormal = 0;
		float nx = 0, ny = 0, nz = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			final int transformedNormal = normalMatrix.canvas_transform(packedNormal);
			nx = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
			ny = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
			nz = NormalHelper.getPackedNormalComponent(transformedNormal, 2);
		}

		final boolean emissive = quad.material().emissive();

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, buff);

			final int color = quad.spriteColor(i, 0);
			buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);

			buff.texture(quad.spriteU(i, 0), quad.spriteV(i, 0));
			buff.overlay(overlay);
			buff.light(emissive ? FULL_BRIGHTNESS : quad.lightmap(i));

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					final int transformedNormal = normalMatrix.canvas_transform(packedNormal);
					nx = NormalHelper.getPackedNormalComponent(transformedNormal, 0);
					ny = NormalHelper.getPackedNormalComponent(transformedNormal, 1);
					nz = NormalHelper.getPackedNormalComponent(transformedNormal, 2);
				}
			}

			buff.normal(nx, ny, nz);
			buff.next();
		}
	}

	/**
	 * handles block color and red-blue swizzle, common to all renders.
	 */
	public static void colorizeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int colorIndex = quad.colorIndex();

		// PERF: don't swap red blue on white quad (most of em)
		if (colorIndex == -1 || quad.material().disableColorIndex) {
			quad.vertexColor(0, ColorHelper.swapRedBlueIfNeeded(quad.vertexColor(0)));
			quad.vertexColor(1, ColorHelper.swapRedBlueIfNeeded(quad.vertexColor(1)));
			quad.vertexColor(2, ColorHelper.swapRedBlueIfNeeded(quad.vertexColor(2)));
			quad.vertexColor(3, ColorHelper.swapRedBlueIfNeeded(quad.vertexColor(3)));
		} else {
			final int indexedColor = context.indexedColor(colorIndex);
			quad.vertexColor(0, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.vertexColor(0))));
			quad.vertexColor(1, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.vertexColor(1))));
			quad.vertexColor(2, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.vertexColor(2))));
			quad.vertexColor(3, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.vertexColor(3))));
		}
	}

	public static void bufferQuadDirect(MutableQuadViewImpl quad, AbstractRenderContext context, VertexAppender buff) {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final RenderMaterialImpl mat = quad.material();

		assert mat.blendMode != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags << 24;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		int spriteIdCoord = SpriteInfoTexture.BLOCKS.coordinate(quad.spriteId());

		assert spriteIdCoord <= 0xFFFF;

		spriteIdCoord |= (mat.index << 16);

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, buff);

			buff.append(quad.vertexColor(i));
			buff.append(quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16));
			buff.append(spriteIdCoord);

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			final int ao = aoData == null ? 255 : (Math.round(aoData[i] * 255));
			buff.append(blockLight | (skyLight << 8) | (ao << 16));

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			buff.append(transformedNormal | shaderFlags);
		}
	}

	public static void applyBlockLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		if (!quad.material().disableAo() && MinecraftClient.isAmbientOcclusionEnabled()) {
			context.computeAo(quad);
		} else {
			context.computeFlat(quad);
		}
	}

	public static void applyItemLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int lightmap = context.brightness();
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), lightmap));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), lightmap));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), lightmap));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), lightmap));
	}
}
