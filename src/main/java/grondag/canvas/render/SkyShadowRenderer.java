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

package grondag.canvas.render;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL46;

import net.minecraft.client.MinecraftClient;

import grondag.canvas.buffer.encoding.DrawableBuffer;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.varia.MatrixState;

public class SkyShadowRenderer {
	private static boolean active = false;
	private static boolean renderEntityShadows = false;
	private static int cascade;

	private static void begin() {
		assert !active;
		active = true;
		final int size = Pipeline.skyShadowSize;
		PipelineManager.setProjection(size, size);
		RenderSystem.viewport(0, 0, size, size);
	}

	private static void end() {
		assert active;
		active = false;
		PipelineManager.setProjection(PipelineManager.width(), PipelineManager.height());
		RenderSystem.viewport(0, 0, PipelineManager.width(), PipelineManager.height());
	}

	public static boolean isActive() {
		return active;
	}

	public static void render(CanvasWorldRenderer canvasWorldRenderer, double cameraX, double cameraY, double cameraZ, DrawableBuffer entityBuffer, DrawableBuffer shadowExtrasBuffer) {
		if (Pipeline.skyShadowFbo != null) {
			// Viewport call (or something else) seems to be messing up fixed-function matrix state
			RenderSystem.pushMatrix();

			begin();

			for (cascade = 0; cascade < MatrixState.CASCADE_COUNT; ++cascade) {
				Pipeline.skyShadowFbo.bind();
				GL46.glFramebufferTextureLayer(GL46.GL_FRAMEBUFFER, FramebufferInfo.DEPTH_ATTACHMENT, Pipeline.shadowMapDepth, 0, cascade);
				renderInner(canvasWorldRenderer, cameraX, cameraY, cameraZ, entityBuffer, shadowExtrasBuffer);
			}

			Pipeline.defaultFbo.bind();

			end();

			RenderSystem.popMatrix();
		}
	}

	private static void renderInner(CanvasWorldRenderer canvasWorldRenderer, double cameraX, double cameraY, double cameraZ, DrawableBuffer entityBuffer, DrawableBuffer shadowExtrasBuffer) {
		Pipeline.skyShadowFbo.clear();

		// WIP: will need purpose-specific methods for each frustum/render type
		MatrixState.set(MatrixState.REGION);
		canvasWorldRenderer.renderTerrainLayer(false, cameraX, cameraY, cameraZ);
		MatrixState.set(MatrixState.CAMERA);

		if (Pipeline.config().skyShadow.allowEntities && MinecraftClient.getInstance().options.entityShadows) {
			entityBuffer.draw(true);
			shadowExtrasBuffer.draw(true);
		}
	}

	/** Preserves entityShadows option state, overwriting it temporarily if needed to prevent vanilla from rendering shadows. */
	public static void suppressEntityShadows(MinecraftClient mc) {
		if (Pipeline.skyShadowFbo != null) {
			renderEntityShadows = mc.options.entityShadows;
			mc.options.entityShadows = false;
		}
	}

	/** Restores entityShadows option state. */
	public static void restoreEntityShadows(MinecraftClient mc) {
		if (Pipeline.skyShadowFbo != null) {
			mc.options.entityShadows = renderEntityShadows;
		}
	}

	public static int cascade() {
		return cascade;
	}
}
