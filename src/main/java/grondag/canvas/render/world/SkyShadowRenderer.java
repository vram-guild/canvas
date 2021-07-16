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

package grondag.canvas.render.world;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.buffer.encoding.DrawableStream;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.shader.data.ShadowMatrixData;
import grondag.canvas.varia.GFX;

public class SkyShadowRenderer {
	private static boolean active = false;
	private static boolean renderEntityShadows = false;
	private static int cascade;

	private static void begin() {
		assert !active;
		active = true;
		final int size = Pipeline.skyShadowSize;
		RenderSystem.viewport(0, 0, size, size);
	}

	private static void end() {
		assert active;
		active = false;
		RenderSystem.viewport(0, 0, PipelineManager.width(), PipelineManager.height());
	}

	public static boolean isActive() {
		return active;
	}

	public static void render(CanvasWorldRenderer canvasWorldRenderer, DrawableStream entityBuffer, DrawableStream shadowExtrasBuffer) {
		if (Pipeline.shadowsEnabled()) {
			begin();

			for (cascade = 0; cascade < ShadowMatrixData.CASCADE_COUNT; ++cascade) {
				Pipeline.skyShadowFbo.bind();
				GFX.framebufferTextureLayer(GFX.GL_FRAMEBUFFER, GFX.GL_DEPTH_ATTACHMENT, Pipeline.shadowMapDepth, 0, cascade);
				renderInner(canvasWorldRenderer, entityBuffer, shadowExtrasBuffer);
			}

			Pipeline.defaultFbo.bind();

			end();
		}
	}

	private static void renderInner(CanvasWorldRenderer canvasWorldRenderer, DrawableStream entityBuffer, DrawableStream shadowExtrasBuffer) {
		Pipeline.skyShadowFbo.clear();

		canvasWorldRenderer.worldRenderState.renderShadowLayer(cascade);

		if (Pipeline.config().skyShadow.allowEntities && MinecraftClient.getInstance().options.entityShadows) {
			entityBuffer.draw(true);
			shadowExtrasBuffer.draw(true);
		}
	}

	/** Preserves entityShadows option state, overwriting it temporarily if needed to prevent vanilla from rendering shadows. */
	public static void suppressEntityShadows(MinecraftClient mc) {
		if (Pipeline.shadowsEnabled()) {
			renderEntityShadows = mc.options.entityShadows;
			mc.options.entityShadows = false;
		}
	}

	/** Restores entityShadows option state. */
	public static void restoreEntityShadows(MinecraftClient mc) {
		if (Pipeline.shadowsEnabled()) {
			mc.options.entityShadows = renderEntityShadows;
		}
	}

	public static int cascade() {
		return cascade;
	}
}
