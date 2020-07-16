/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
 ******************************************************************************/


package grondag.canvas.apiimpl;

import java.util.HashMap;
import java.util.function.BooleanSupplier;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.RenderMaterialImpl.CompositeMaterial;
import grondag.canvas.apiimpl.RenderMaterialImpl.Finder;
import grondag.canvas.apiimpl.mesh.MeshBuilderImpl;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.buffer.encoding.VertexEncoders;
import grondag.canvas.compat.LitematicaHolder;
import grondag.canvas.light.AoVertexClampFunction;
import grondag.canvas.light.LightmapHd;
import grondag.canvas.light.LightmapHdTexture;
import grondag.canvas.material.MaterialState;
import grondag.canvas.perf.ChunkRebuildCounters;
import grondag.canvas.pipeline.ProcessShaders;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.terrain.ChunkColorCache;
import grondag.canvas.terrain.ProtoRenderRegion;
import grondag.canvas.terrain.TerrainModelSpace;
import grondag.frex.api.Renderer;
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.ShaderBuilder;

public class Canvas implements Renderer {
	public static final Canvas INSTANCE = new Canvas();

	public static final RenderMaterialImpl.CompositeMaterial MATERIAL_STANDARD = INSTANCE.materialFinder().find();

	static {
		INSTANCE.registerMaterial(RenderMaterial.MATERIAL_STANDARD, MATERIAL_STANDARD);
	}

	private final HashMap<Identifier, CompositeMaterial> materialMap = new HashMap<>();
	private final HashMap<Identifier, MaterialShaderImpl> shaderMap = new HashMap<>();
	private final HashMap<Identifier, MaterialConditionImpl> conditionMap = new HashMap<>();

	private Canvas() { }

	@Override
	public MeshBuilder meshBuilder() {
		return new MeshBuilderImpl();
	}

	@Override
	public Finder materialFinder() {
		return new RenderMaterialImpl.Finder();
	}

	@Override
	public CompositeMaterial materialById(Identifier id) {
		return materialMap.get(id);
	}

	@Override
	public boolean registerMaterial(Identifier id, RenderMaterial material) {
		if (materialMap.containsKey(id)) {
			return false;
		}

		// cast to prevent acceptance of impostor implementations
		materialMap.put(id, (CompositeMaterial) material);
		return true;
	}

	public void reload() {
		CanvasMod.LOG.info(I18n.translate("info.canvas.reloading"));
		ProtoRenderRegion.reload();
		BlockRenderContext.reload();
		ChunkRebuildCounters.reset();
		ChunkColorCache.invalidate();
		AoVertexClampFunction.reload();
		GlShaderManager.INSTANCE.reload();
		LightmapHdTexture.reload();
		LightmapHd.reload();
		MaterialShaderManager.INSTANCE.reload();
		MaterialState.reload();
		VertexEncoders.reload();
		TerrainModelSpace.reload();
		ProcessShaders.reload();
		LitematicaHolder.litematicaReload.run();
	}

	@Override
	public int maxSpriteDepth() {
		return RenderMaterialImpl.MAX_SPRITE_DEPTH;
	}

	@Override
	public ShaderBuilder shaderBuilder() {
		return new ShaderBuilderImpl();
	}

	@Override
	public MaterialShaderImpl shaderById(Identifier id) {
		return shaderMap.get(id);
	}

	@Override
	public boolean registerShader(Identifier id, MaterialShader shader) {
		if (shaderMap.containsKey(id)) {
			return false;
		}

		// cast to prevent acceptance of impostor implementations
		shaderMap.put(id, (MaterialShaderImpl) shader);
		return true;
	}

	@Override
	public MaterialCondition createCondition(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
		return new MaterialConditionImpl(supplier, affectBlocks, affectItems);
	}

	@Override
	public MaterialCondition conditionById(Identifier id) {
		return conditionMap.get(id);
	}

	@Override
	public boolean registerCondition(Identifier id, MaterialCondition condition) {
		if (conditionMap.containsKey(id)) {
			return false;
		}
		// cast to prevent acceptance of impostor implementations
		conditionMap.put(id, (MaterialConditionImpl) condition);
		return true;
	}
}
