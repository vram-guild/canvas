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

package grondag.canvas.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;

public class DrawBetterer {
	private static BufferBuilder fillBufferBuilder;
	private static Matrix4f fillMatrix4f;

	public static void beginBatchFill(Matrix4f fillMatrix4fIn) {
		fillMatrix4f = fillMatrix4fIn;
		fillBufferBuilder = BufferBuilderStore.claim();
	}

	public static boolean handleFill(int x0, int y0, int x1, int y1, int colorBGRA) {
		if (fillMatrix4f == null) {
			return false;
		}

		if (!fillBufferBuilder.isBuilding()) {
			fillBufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		}

		int swap;

		if (x0 < x1) {
			swap = x0;
			x0 = x1;
			x1 = swap;
		}

		if (y0 < y1) {
			swap = y0;
			y0 = y1;
			y1 = swap;
		}

		float a = (colorBGRA >> 24 & 255) / 255.0F;
		float r = (colorBGRA >> 16 & 255) / 255.0F;
		float g = (colorBGRA >> 8 & 255) / 255.0F;
		float b = (colorBGRA & 255) / 255.0F;

		final BufferBuilder bufferBuilder = fillBufferBuilder;
		final Matrix4f matrix4f = fillMatrix4f;

		bufferBuilder.vertex(matrix4f, x0, y1, 0.0F).color(r, g, b, a).next();
		bufferBuilder.vertex(matrix4f, x1, y1, 0.0F).color(r, g, b, a).next();
		bufferBuilder.vertex(matrix4f, x1, y0, 0.0F).color(r, g, b, a).next();
		bufferBuilder.vertex(matrix4f, x0, y0, 0.0F).color(r, g, b, a).next();

		return true;
	}

	public static void endBatchFile() {
		assert fillMatrix4f != null;

		if (fillBufferBuilder.isBuilding()) {
			fillBufferBuilder.end();
			RenderSystem.enableBlend();
			RenderSystem.disableTexture();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			BufferRenderer.draw(fillBufferBuilder);
			RenderSystem.enableTexture();
			RenderSystem.disableBlend();
		}

		fillBufferBuilder = BufferBuilderStore.release(fillBufferBuilder);
		fillMatrix4f = null;
	}
}
