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

import java.util.function.Predicate;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL46;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;

import grondag.canvas.config.Configurator;
import grondag.canvas.material.property.MaterialFog;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineFramebuffer;

@FunctionalInterface
interface WorldRenderPass {
	void render(WorldRenderPassContext context);

	static WorldRenderPass copyDepth(PipelineFramebuffer source, PipelineFramebuffer dest) {
		return ctx -> source.copyDepthFrom(dest);
	}

	static WorldRenderPass profilerSwap(String token) {
		return ctx -> {
			Configurator.lagFinder.swap(token);
			ctx.profiler.swap(token);
		};
	}

	static WorldRenderPass materialFog(boolean enable) {
		return ctx -> MaterialFog.allow(enable);
	}

	static WorldRenderPass lightUpdates() {
		return ctx -> ctx.mc.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
	}

	static WorldRenderPass bindFramebuffer(String name) {
		final PipelineFramebuffer fb = Pipeline.getFramebuffer(name);
		return ctx -> fb.bind();
	}

	/**
	 * Does not actually render anything - what it does do is set the current clear color.
	 * Color is captured via a mixin for use in shaders - if not called the vanilla color will not be available
	 * and any mods that hook here may not trigger.
	 */
	static WorldRenderPass setVanillaClearColor() {
		return ctx -> BackgroundRenderer.render(ctx.camera, ctx.tickDelta, ctx.mc.world, ctx.mc.options.viewDistance, ctx.gameRenderer.getSkyDarkness(ctx.tickDelta));
	}

	/**
	 * Uses currently bound fbo and current clear color, ignoring
	 * any fb clear configuration.
	 */
	static WorldRenderPass clearColorDepth() {
		return ctx -> RenderSystem.clear(GL46.GL_DEPTH_BUFFER_BIT | GL46.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
	}

	static WorldRenderPass setupVanillaSkyFog() {
		return ctx -> BackgroundRenderer.applyFog(ctx.camera, BackgroundRenderer.FogType.FOG_SKY, ctx.viewDistance, ctx.thickFog);
	}

	static WorldRenderPass renderVanillaSky() {
		// NB: fog / sky renderer normalcy get viewMatrixStack but we apply camera rotation in VertexBuffer mixin
		return ctx -> ctx.canvasWorldRenderer.renderSky(ctx.identityStack, ctx.tickDelta);
	}

	static WorldRenderPass conditional(Predicate<WorldRenderPassContext> condition, WorldRenderPass pass) {
		return ctx -> {
			if (condition.test(ctx)) {
				pass.render(ctx);
			}
		};
	}

	static WorldRenderPass chain(WorldRenderPass... passes) {
		return ctx -> {
			for (final WorldRenderPass pass : passes) {
				pass.render(ctx);
			}
		};
	}
}
