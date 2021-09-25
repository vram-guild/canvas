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

package grondag.canvas.apiimpl;

import java.util.function.BooleanSupplier;

import io.vram.frex.api.material.MaterialCondition;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.mesh.MeshBuilder;
import io.vram.frex.api.renderer.Renderer;
import io.vram.frex.api.renderer.RendererConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.mesh.MeshBuilderImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.light.AoVertexClampFunction;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.material.state.MaterialFinderImpl;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.config.PipelineLoader;
import grondag.canvas.shader.GlProgramManager;
import grondag.canvas.shader.GlShader;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.MaterialProgramManager;
import grondag.canvas.shader.PreReleaseShaderCompat;
import grondag.canvas.shader.data.ShaderDataManager;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.ChunkColorCache;

public class Canvas implements Renderer {
	private static Canvas instance = new Canvas();

	public static Canvas instance() {
		return instance;
	}

	public static void initialize() {
		RendererConsumer.accept(instance);
	}

	private final Object2ObjectOpenHashMap<ResourceLocation, RenderMaterialImpl> materialMap = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectOpenHashMap<ResourceLocation, MaterialConditionImpl> conditionMap = new Object2ObjectOpenHashMap<>();

	private Canvas() { }

	@Override
	public MeshBuilder meshBuilder() {
		return new MeshBuilderImpl();
	}

	@Override
	public MaterialFinderImpl materialFinder() {
		return new MaterialFinderImpl();
	}

	@Override
	public RenderMaterialImpl materialById(ResourceLocation id) {
		return materialMap.get(id);
	}

	@Override
	public boolean registerMaterial(ResourceLocation id, RenderMaterial material) {
		if (materialMap.containsKey(id)) {
			return false;
		}

		// cast to prevent acceptance of impostor implementations
		materialMap.put(id, (RenderMaterialImpl) material);
		return true;
	}

	@Override
	public boolean registerOrUpdateMaterial(ResourceLocation id, RenderMaterial material) {
		// cast to prevent acceptance of impostor implementations
		return materialMap.put(id, (RenderMaterialImpl) material) == null;
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
		PipelineLoader.INSTANCE.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
		Pipeline.reload();
		PreReleaseShaderCompat.reload();
		GlShader.forceReloadErrors();
		GlShaderManager.INSTANCE.reload();
		GlProgramManager.INSTANCE.reload();
		MaterialProgramManager.INSTANCE.reload();
		// LightmapHdTexture.reload();
		// LightmapHd.reload();
		MaterialTextureState.reload();
		ShaderDataManager.reload();
		Timekeeper.configOrPipelineReload();
	}

	@Override
	public MaterialCondition createCondition(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
		return MaterialConditionImpl.create(supplier, affectBlocks, affectItems);
	}

	@Override
	public MaterialCondition conditionById(ResourceLocation id) {
		return conditionMap.get(id);
	}

	@Override
	public boolean registerCondition(ResourceLocation id, MaterialCondition condition) {
		if (conditionMap.containsKey(id)) {
			return false;
		}

		// cast to prevent acceptance of impostor implementations
		conditionMap.put(id, (MaterialConditionImpl) condition);
		return true;
	}
}
