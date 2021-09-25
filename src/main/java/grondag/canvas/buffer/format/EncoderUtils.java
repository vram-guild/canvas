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

package grondag.canvas.buffer.format;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;

import grondag.canvas.apiimpl.mesh.MeshEncodingHelper;
import grondag.canvas.apiimpl.mesh.QuadEditorImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.apiimpl.util.PackedVector3f;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.mixinterface.Matrix4fExt;

public abstract class EncoderUtils {
	public static void bufferQuad(QuadEditorImpl quad, EncodingContext context, VertexConsumer buff) {
		final Matrix4fExt matrix = (Matrix4fExt) context.matrix();
		final int overlay = context.overlay();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final boolean isNormalMatrixUseful = !normalMatrix.canvas_isIdentity();

		final int quadNormalFlags = quad.normalFlags();
		// don't retrieve if won't be used
		final int faceNormal = quadNormalFlags == 0b1111 ? 0 : quad.packedFaceNormal();
		int packedNormal = 0;
		float nx = 0, ny = 0, nz = 0;

		final boolean emissive = quad.material().emissive();

		for (int i = 0; i < 4; i++) {
			quad.transformAndAppendVertex(i, matrix, buff);

			final int color = quad.vertexColor(i);
			buff.color(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF, (color >> 24) & 0xFF);

			buff.uv(quad.spriteU(i), quad.spriteV(i));
			buff.overlayCoords(overlay);
			buff.uv2(emissive ? MeshEncodingHelper.FULL_BRIGHTNESS : quad.lightmap(i));

			final int p = ((quadNormalFlags & 1 << i) == 0) ? faceNormal : quad.packedNormal(i);

			if (p != packedNormal) {
				packedNormal = p;
				final int transformedNormal = isNormalMatrixUseful ? normalMatrix.canvas_transform(packedNormal) : packedNormal;
				nx = PackedVector3f.packedX(transformedNormal);
				ny = PackedVector3f.packedY(transformedNormal);
				nz = PackedVector3f.packedZ(transformedNormal);
			}

			buff.normal(nx, ny, nz);
			buff.endVertex();
		}
	}

	/**
	 * handles block color and red-blue swizzle, common to all renders.
	 */
	public static void colorizeQuad(QuadEditorImpl quad, AbstractRenderContext context) {
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

	public static void applyBlockLighting(QuadEditorImpl quad, AbstractRenderContext context) {
		if (!quad.material().disableAo() && Minecraft.useAmbientOcclusion()) {
			context.computeAo(quad);
		} else {
			context.computeFlat(quad);
		}
	}

	public static void applyItemLighting(QuadEditorImpl quad, AbstractRenderContext context) {
		final int lightmap = context.brightness();
		quad.lightmap(0, ColorHelper.maxBrightness(quad.lightmap(0), lightmap));
		quad.lightmap(1, ColorHelper.maxBrightness(quad.lightmap(1), lightmap));
		quad.lightmap(2, ColorHelper.maxBrightness(quad.lightmap(2), lightmap));
		quad.lightmap(3, ColorHelper.maxBrightness(quad.lightmap(3), lightmap));
	}
}
