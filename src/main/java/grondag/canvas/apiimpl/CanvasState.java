/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.apiimpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import io.vram.frex.base.renderer.ao.AoFace;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.CanvasBlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.CanvasEntityBlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.CanvasItemRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.light.color.LightDataManager;
import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.shader.GlMaterialProgramManager;
import grondag.canvas.shader.GlProgramManager;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.MaterialProgram;
import grondag.canvas.shader.PreReleaseShaderCompat;
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.ChunkColorCache;

public class CanvasState {
	public static void handleRecompileKeybind() {
		final boolean recompilePressed = CanvasMod.RECOMPILE.consumeClick();

		while (true) {
			// consume all clicks
			if (!CanvasMod.RECOMPILE.consumeClick()) {
				break;
			}
		}

		if (recompilePressed) {
			recompile(false);
		}
	}

	public static void recompile() {
		recompile(false);
	}

	private static int loopCounter = 0;

	private static boolean recompilePipeline() {
		final boolean prevColorLightsState = Pipeline.coloredLightsEnabled();
		final boolean prevAdvancedCullingState = Pipeline.advancedTerrainCulling();

		PipelineLoader.reload(Minecraft.getInstance().getResourceManager());
		PipelineManager.reload();

		final boolean coloredLightsChanged = Pipeline.coloredLightsEnabled() && !prevColorLightsState;
		final boolean requireCullingRebuild = Pipeline.advancedTerrainCulling() && !prevAdvancedCullingState;

		return coloredLightsChanged || requireCullingRebuild;
	}

	private static void recompile(boolean alreadyReloaded) {
		CanvasMod.LOG.info(I18n.get("info.canvas.recompile"));

		final boolean requireReload = recompilePipeline();

		if (!alreadyReloaded && loopCounter < 2 && requireReload) {
			CanvasMod.LOG.info(I18n.get("info.canvas.recompile_needs_reload"));
			loopCounter++;
			Minecraft.getInstance().levelRenderer.allChanged();
			return;
		}

		LightDataManager.reload();
		PreReleaseShaderCompat.reload();
		MaterialProgram.reload();
		GlShaderManager.INSTANCE.reload();
		GlProgramManager.INSTANCE.reload();
		GlMaterialProgramManager.INSTANCE.reload();
		// LightmapHdTexture.reload();
		// LightmapHd.reload();
		TextureMaterialState.reload();
		ShaderDataManager.reload();
		Timekeeper.configOrPipelineReload();

		if (loopCounter > 1) {
			CanvasMod.LOG.warn("Reloading recursively twice or more. This isn't supposed to happen.");
		}

		loopCounter = 0;
	}

	public static void reload() {
		CanvasMod.LOG.info(I18n.get("info.canvas.reloading"));
		PackedInputRegion.reload();
		CanvasBlockRenderContext.reload();
		CanvasEntityBlockRenderContext.reload();
		CanvasItemRenderContext.reload();
		ChunkRebuildCounters.reset();
		ChunkColorCache.invalidate();
		AoFace.clampExteriorVertices(Configurator.clampExteriorVertices);

		recompile(true);
	}
}
