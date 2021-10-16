/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.apiimpl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import io.vram.frex.api.mesh.MeshBuilder;
import io.vram.frex.api.renderer.ConditionManager;
import io.vram.frex.api.renderer.MaterialManager;
import io.vram.frex.api.renderer.MaterialShaderManager;
import io.vram.frex.api.renderer.MaterialTextureManager;
import io.vram.frex.api.renderer.Renderer;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.mesh.BaseMeshBuilder;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.light.AoVertexClampFunction;
import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.material.state.CanvasMaterialManager;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.shader.GlProgramManager;
import grondag.canvas.shader.GlShader;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.MaterialProgramManager;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.PreReleaseShaderCompat;
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.ChunkColorCache;

public class Canvas implements Renderer {
	private static Canvas instance = new Canvas();

	public static Canvas instance() {
		return instance;
	}

	private Canvas() { }

	@Override
	public MeshBuilder meshBuilder() {
		return new BaseMeshBuilder();
	}

	public void reload() {
		CanvasMod.LOG.info(I18n.get("info.canvas.reloading"));
		PackedInputRegion.reload();
		BlockRenderContext.reload();
		EntityBlockRenderContext.reload();
		ItemRenderContext.reload();
		ChunkRebuildCounters.reset();
		ChunkColorCache.invalidate();
		AoVertexClampFunction.reload();

		recompile();
	}

	public void recompile() {
		PipelineLoader.reload(Minecraft.getInstance().getResourceManager());
		Pipeline.reload();
		PreReleaseShaderCompat.reload();
		GlShader.forceReloadErrors();
		GlShaderManager.INSTANCE.reload();
		GlProgramManager.INSTANCE.reload();
		MaterialProgramManager.INSTANCE.reload();
		// LightmapHdTexture.reload();
		// LightmapHd.reload();
		TextureMaterialState.reload();
		ShaderDataManager.reload();
		Timekeeper.configOrPipelineReload();
	}

	@Override
	public ConditionManager conditions() {
		return MaterialConditionImpl.REGISTRY;
	}

	@Override
	public MaterialTextureManager textures() {
		return CanvasTextureManager.INSTANCE;
	}

	@Override
	public MaterialShaderManager shaders() {
		return MaterialShaderId.MANAGER;
	}

	@Override
	public MaterialManager materials() {
		return CanvasMaterialManager.INSTANCE;
	}
}
