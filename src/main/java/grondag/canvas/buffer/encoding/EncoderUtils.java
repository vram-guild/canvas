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

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.NormalHelper;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;
import grondag.canvas.texture.SpriteInfoTexture;
import grondag.canvas.wip.state.WipRenderMaterial;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

abstract class EncoderUtils {
	private static final int NO_AO_SHADE = 0x7F000000;

	static void bufferQuad1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final VertexConsumer buff = context.consumer(quad.material());

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

		final boolean emissive = quad.material().emissive(0);

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppend(i, matrix, buff);

			final int color = quad.spriteColor(i, 0);
			buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);

			buff.texture(quad.spriteU(i, 0), quad.spriteV(i, 0));
			buff.overlay(overlay);
			buff.light(emissive ? VertexEncoder.FULL_BRIGHTNESS : quad.lightmap(i));

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
	static void colorizeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int colorIndex = quad.colorIndex();

		// PERF: don't swap red blue on white quad (most of em)
		if (colorIndex == -1 || quad.material().disableColorIndex()) {
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

	static void bufferQuadDirect1(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4fExt matrix = (Matrix4fExt) (Object) context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final float[] aoData = quad.ao;
		final WipRenderMaterial mat = quad.material();
		final VertexCollectorImpl buff0 = context.collectors.get(mat);
		final int[] appendData = context.appendData;

		assert mat.blendMode() != BlendMode.DEFAULT;

		final int shaderFlags = mat.shaderFlags << 16;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		final int spriteIdCoord = SpriteInfoTexture.BLOCKS.coordinate(quad.spriteId());

		assert spriteIdCoord <= 0xFFFF;

		int k = 0;

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppend(i, matrix, appendData, k);
			k += 3;

			appendData[k++] = quad.vertexColor(i);
			appendData[k++] = quad.spriteBufferU(i) | (quad.spriteBufferV(i) << 16);

			final int packedLight = quad.lightmap(i);
			final int blockLight = (packedLight & 0xFF);
			final int skyLight = ((packedLight >> 16) & 0xFF);
			appendData[k++] = blockLight | (skyLight << 8) | shaderFlags;

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			final int ao = aoData == null ? NO_AO_SHADE : ((Math.round(aoData[i] * 254) - 127) << 24);
			appendData[k++] = transformedNormal | ao;

			appendData[k++] = spriteIdCoord;
		}

		buff0.add(appendData, k);
	}

	static void applyBlockLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		// FIX: per-vertex light maps will be ignored unless we bake a custom HD map
		// or retain vertex light maps in buffer format and logic in shader to take max

		if (!quad.material().disableAo() && MinecraftClient.isAmbientOcclusionEnabled()) {
			context.aoCalc().compute(quad);
		} else {
			if (Configurator.semiFlatLighting) {
				context.aoCalc().computeFlat(quad);
			} else {
				// TODO: in HD path don't do this
				final int brightness = context.flatBrightness(quad);
				quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), brightness));
				quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), brightness));
				quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), brightness));
				quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), brightness));

				if (Configurator.hdLightmaps()) {
					context.aoCalc().computeFlatHd(quad, brightness);
				}
			}
		}
	}

	static void applyItemLighting(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final int lightmap = context.brightness();
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), lightmap));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), lightmap));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), lightmap));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), lightmap));
	}
}
