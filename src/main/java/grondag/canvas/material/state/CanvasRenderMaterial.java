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

package grondag.canvas.material.state;

import static grondag.canvas.material.state.MaterialStateEncoder.FLAG_SHIFT;

import com.google.common.base.Strings;

import net.minecraft.resources.ResourceLocation;

import io.vram.frex.api.material.MaterialCondition;
import io.vram.frex.api.material.MaterialShader;
import io.vram.frex.api.texture.MaterialTexture;
import io.vram.frex.base.renderer.BaseConditionManager.BaseMaterialCondition;
import io.vram.frex.base.renderer.BaseMaterialShader;
import io.vram.frex.base.renderer.material.BaseMaterialManager;
import io.vram.frex.base.renderer.material.BaseMaterialView;
import io.vram.frex.base.renderer.material.BaseRenderMaterial;
import io.vram.frex.base.renderer.util.ResourceCache;

import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.mixinterface.TextureAtlasExt;
import grondag.canvas.shader.MaterialShaderIndexer;
import grondag.canvas.shader.ProgramType;
import grondag.canvas.texture.MaterialIndexer;

public class CanvasRenderMaterial extends BaseRenderMaterial {
	protected final ResourceCache<MaterialIndexer> indexer;
	protected final long collectorKey;
	protected final int collectorIndex;
	protected final int shaderFlags;
	protected final RenderState renderState;
	protected final long materialKey;

	protected final MaterialTexture materialTexture;
	protected final TextureMaterialState texture;

	protected final BaseMaterialShader shaderId;

	protected final int vertexShaderIndex;
	protected final ResourceLocation vertexShaderId;
	protected final int fragmentShaderIndex;
	protected final ResourceLocation fragmentShaderId;

	protected final int depthVertexShaderIndex;
	protected final ResourceLocation depthVertexShaderId;
	protected final String depthVertexShader;
	protected final int depthFragmentShaderIndex;
	protected final ResourceLocation depthFragmentShaderId;
	protected final String depthFragmentShader;
	//protected final boolean primaryTargetTransparency;

	/**
	 * Will be always visible condition in vertex-controlled render state.
	 * This is ensured by the state mask.
	 */
	protected final BaseMaterialCondition condition;

	// PERF: use discardsTexture() to avoid overhead of animated textures
	public CanvasRenderMaterial(BaseMaterialManager<CanvasRenderMaterial> manager, int index, BaseMaterialView finder) {
		super(manager, index, finder);

		materialTexture = manager.textures.textureFromIndex(textureIndex);
		texture = TextureMaterialState.fromId(materialTexture.id());
		condition = (BaseMaterialCondition) manager.conditions.conditionFromIndex(conditionIndex());

		collectorKey = MaterialStateEncoder.encodeCollectorKey(this, texture);
		materialKey = MaterialStateEncoder.encodeMaterialKey(this);

		// Main purpose of persisting this stuff is run-time debug
		// May also avoid a few pointer chases.
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey);
		shaderFlags = (int) (materialKey >>> FLAG_SHIFT) & 0xFFFF;
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		shaderId = (BaseMaterialShader) manager.shaders.shaderFromIndex(shaderIndex());
		vertexShaderIndex = shaderId.vertexIndex;
		fragmentShaderIndex = shaderId.fragmentIndex;
		vertexShaderId = shaderId.vertexId;
		fragmentShaderId = shaderId.fragmentId;

		depthVertexShaderIndex = shaderId.depthVertexIndex;
		depthVertexShaderId = shaderId.depthVertexId;
		depthVertexShader = depthVertexShaderId.toString();
		depthFragmentShaderIndex = shaderId.depthFragmentIndex;
		depthFragmentShaderId = shaderId.depthFragmentId;
		depthFragmentShader = depthFragmentShaderId.toString();

		//primaryTargetTransparency = primaryTargetTransparency(collectorKey);

		// Important that these happen because otherwise material shaders will never be registered - they aren't part of render state.
		MaterialShaderIndexer.INSTANCE.register(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR);
		MaterialShaderIndexer.INSTANCE.register(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH);
		MaterialShaderIndexer.INSTANCE.register(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR_TERRAIN);
		MaterialShaderIndexer.INSTANCE.register(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH_TERRAIN);

		indexer = new ResourceCache<>(() -> renderState.texture.materialIndexProvider().getIndexer(this));

		//System.out.println("\n");
		//System.out.println("Material State");
		//System.out.println(this.toString());
		//System.out.println("Render State");
		//System.out.println(renderState.toString());
	}

	public MaterialIndexer materialIndexer() {
		return indexer.getOrLoad();
	}

	public void trackPerFrameAnimation(int spriteId) {
		if (!this.discardsTexture() && texture().isAtlas()) {
			// WIP: create and use sprite method on quad
			final int animationIndex = ((SpriteExt) texture().spriteIndex().fromIndex(spriteId)).canvas_animationIndex();

			if (animationIndex > 0) {
				((TextureAtlasExt) texture().textureAsAtlas()).canvas_trackFrameAnimation(animationIndex);
			}
		}
	}

	public int collectorIndex() {
		return collectorIndex;
	}

	public RenderState renderState() {
		return renderState;
	}

	public int vertexShaderIndex() {
		return vertexShaderIndex;
	}

	public int fragmentShaderIndex() {
		return fragmentShaderIndex;
	}

	public int shaderFlags() {
		return shaderFlags;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("stateIndex:   ").append(index).append("\n");
		sb.append("collectorIdx: ").append(collectorIndex).append("\n");
		sb.append("collectorKey: ").append(Strings.padStart(Long.toHexString(collectorKey), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(collectorKey), 64, '0')).append("\n");
		sb.append("renderIndex:  ").append(renderState.index).append("\n");
		sb.append("renderKey:    ").append(Strings.padStart(Long.toHexString(renderState.bits), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(renderState.bits), 64, '0')).append("\n");
		sb.append("renderLayerName: ").append(label).append("\n");
		sb.append("target: ").append(target()).append("\n");
		sb.append("texture: ").append(texture().id().toString()).append("\n");
		sb.append("blur: ").append(blur()).append("\n");
		sb.append("transparency: ").append(transparency()).append("\n");
		sb.append("depthTest: ").append(depthTest()).append("\n");
		sb.append("cull: ").append(cull()).append("\n");
		sb.append("writeMask: ").append(writeMask()).append("\n");
		sb.append("foilOverlay: ").append(foilOverlay()).append("\n");
		sb.append("decal: ").append(decal()).append("\n");
		sb.append("lines: ").append(lines()).append("\n");
		sb.append("fog: ").append(fog()).append("\n");

		sb.append("sorted: ").append(sorted()).append("\n");
		final BaseMaterialShader sid = (BaseMaterialShader) shader();
		sb.append("vertexShader: ").append(sid.vertexId.toString()).append(" (").append(sid.vertexIndex).append(")\n");
		sb.append("fragmentShader: ").append(sid.fragmentId.toString()).append(" (").append(sid.fragmentIndex).append(")\n");

		sb.append("conditionIndex: ").append(conditionIndex()).append("\n");

		sb.append("disableColorIndex: ").append(disableColorIndex()).append("\n");
		sb.append("emissive: ").append(emissive()).append("\n");
		sb.append("disableDiffuse: ").append(disableDiffuse()).append("\n");
		sb.append("disableAo: ").append(disableAo()).append("\n");
		sb.append("cutout: ").append(cutout()).append("\n");
		sb.append("unmipped: ").append(unmipped()).append("\n");
		sb.append("hurtOverlay: ").append(hurtOverlay()).append("\n");
		sb.append("flashoverlay: ").append(flashOverlay()).append("\n");

		sb.append("shaderFlags: ").append(Integer.toBinaryString(shaderFlags)).append("\n");
		sb.append("preset: ").append(preset()).append("\n");
		sb.append("drawPriority: ").append(renderState.drawPriority).append("\n");
		return sb.toString();
	}

	@Override
	public MaterialCondition condition() {
		return condition;
	}

	@Override
	public MaterialTexture texture() {
		return materialTexture;
	}

	@Override
	public MaterialShader shader() {
		return shaderId;
	}

	@Override
	public boolean isDefault() {
		return this == manager.defaultMaterial();
	}

	@Override
	public boolean isMissing() {
		return this == manager.missingMaterial();
	}
}
