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

package grondag.canvas.material.state;

import static grondag.canvas.material.state.MaterialStateEncoder.COLLECTOR_AND_STATE_MASK;
import static grondag.canvas.material.state.MaterialStateEncoder.FLAG_SHIFT;
import static grondag.canvas.material.state.MaterialStateEncoder.primaryTargetTransparency;

import com.google.common.base.Strings;

import net.minecraft.resources.ResourceLocation;

import io.vram.frex.api.material.MaterialCondition;
import io.vram.frex.api.material.MaterialShader;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.texture.MaterialTexture;
import io.vram.frex.base.renderer.material.BaseMaterialView;
import io.vram.frex.base.renderer.material.BaseRenderMaterial;
import io.vram.frex.base.renderer.util.ResourceCache;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.mixinterface.TextureAtlasExt;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.MaterialShaderImpl;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.canvas.shader.ProgramType;
import grondag.canvas.texture.MaterialIndexer;

public class CanvasRenderMaterial extends BaseRenderMaterial implements RenderMaterial {
	protected final ResourceCache<MaterialIndexer> indexer;
	protected final long collectorKey;
	protected final int collectorIndex;
	protected final int shaderFlags;
	protected final RenderState renderState;
	protected final long canvasBits;

	protected final MaterialTexture materialTexture;
	protected final TextureMaterialState texture;

	protected final MaterialShaderId shaderId;

	protected final int vertexShaderIndex;
	protected final ResourceLocation vertexShaderId;
	protected final int fragmentShaderIndex;
	protected final ResourceLocation fragmentShaderId;
	protected final MaterialShaderImpl shader;
	protected final MaterialShaderImpl guiShader;
	protected final MaterialShaderImpl terrainShader;

	protected final int depthVertexShaderIndex;
	protected final ResourceLocation depthVertexShaderId;
	protected final String depthVertexShader;
	protected final int depthFragmentShaderIndex;
	protected final ResourceLocation depthFragmentShaderId;
	protected final String depthFragmentShader;
	protected final MaterialShaderImpl depthShader;
	protected final MaterialShaderImpl terrainDepthShader;
	protected final boolean primaryTargetTransparency;

	/**
	 * Will be always visible condition in vertex-controlled render state.
	 * This is ensured by the state mask.
	 */
	protected final MaterialConditionImpl condition;

	// PERF: use discardsTexture() to avoid overhead of animated textures
	public CanvasRenderMaterial(BaseMaterialView finder, int index) {
		super(index, finder);

		materialTexture = MaterialTexture.fromIndex(textureIndex);
		texture = TextureMaterialState.fromId(materialTexture.id());
		condition = (MaterialConditionImpl) super.condition();

		// NB: must prefix bit packers here because FREX packers have same names
		long canvasBits = 0;
		canvasBits = MaterialStateEncoder.BLUR.setValue(blur(), canvasBits);
		canvasBits = MaterialStateEncoder.DISABLE_SHADOWS.setValue(!castShadows(), canvasBits);
		canvasBits = MaterialStateEncoder.CONDITION.setValue(conditionIndex(), canvasBits);
		canvasBits = MaterialStateEncoder.CULL.setValue(cull(), canvasBits);
		canvasBits = MaterialStateEncoder.CUTOUT.setValue(cutout(), canvasBits);
		canvasBits = MaterialStateEncoder.DECAL.setValue(decal(), canvasBits);
		canvasBits = MaterialStateEncoder.DEPTH_TEST.setValue(depthTest(), canvasBits);
		canvasBits = MaterialStateEncoder.DISABLE_AO.setValue(disableAo(), canvasBits);
		canvasBits = MaterialStateEncoder.DISABLE_COLOR_INDEX.setValue(disableColorIndex(), canvasBits);
		canvasBits = MaterialStateEncoder.DISABLE_DIFFUSE.setValue(disableDiffuse(), canvasBits);
		canvasBits = MaterialStateEncoder.DISCARDS_TEXTURE.setValue(discardsTexture(), canvasBits);
		canvasBits = MaterialStateEncoder.EMISSIVE.setValue(emissive(), canvasBits);
		canvasBits = MaterialStateEncoder.FLASH_OVERLAY.setValue(flashOverlay(), canvasBits);
		canvasBits = MaterialStateEncoder.FOG.setValue(fog(), canvasBits);
		canvasBits = MaterialStateEncoder.ENABLE_GLINT.setValue(foilOverlay(), canvasBits);
		canvasBits = MaterialStateEncoder.HURT_OVERLAY.setValue(hurtOverlay(), canvasBits);
		canvasBits = MaterialStateEncoder.LINES.setValue(lines(), canvasBits);
		canvasBits = MaterialStateEncoder.PRESET.setValue(preset(), canvasBits);
		canvasBits = MaterialStateEncoder.SHADER_ID.setValue(shaderIndex(), canvasBits);
		canvasBits = MaterialStateEncoder.SORTED.setValue(sorted(), canvasBits);
		canvasBits = MaterialStateEncoder.TARGET.setValue(target(), canvasBits);
		canvasBits = MaterialStateEncoder.TEXTURE.setValue(texture.index, canvasBits);
		canvasBits = MaterialStateEncoder.TRANSPARENCY.setValue(transparency(), canvasBits);
		canvasBits = MaterialStateEncoder.UNMIPPED.setValue(unmipped(), canvasBits);
		canvasBits = MaterialStateEncoder.WRITE_MASK.setValue(writeMask(), canvasBits);
		this.canvasBits = canvasBits;

		// Main purpose of persisting this stuff is run-time debug
		// May also avoid a few pointer chases.
		collectorKey = canvasBits & COLLECTOR_AND_STATE_MASK;
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey);
		shaderFlags = (int) (canvasBits >>> FLAG_SHIFT) & 0xFFFF;
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		shaderId = (MaterialShaderId) super.shader();
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

		primaryTargetTransparency = primaryTargetTransparency(canvasBits);

		// Important that these happen because otherwise material shaders will never be registered - they aren't part of render state.
		shader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR);
		guiShader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR);
		depthShader = MaterialShaderManager.INSTANCE.find(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH);
		terrainShader = MaterialShaderManager.INSTANCE.find(vertexShaderIndex, fragmentShaderIndex, ProgramType.MATERIAL_COLOR_TERRAIN);
		terrainDepthShader = MaterialShaderManager.INSTANCE.find(depthVertexShaderIndex, depthFragmentShaderIndex, ProgramType.MATERIAL_DEPTH_TERRAIN);

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
		sb.append("stateKey      ").append(Strings.padStart(Long.toHexString(canvasBits), 16, '0')).append("  ").append(Strings.padStart(Long.toBinaryString(canvasBits), 64, '0')).append("\n");
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
		final MaterialShaderId sid = (MaterialShaderId) shader();
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
		return this == CanvasMaterialManager.INSTANCE.defaultMaterial();
	}

	@Override
	public boolean isMissing() {
		return this == CanvasMaterialManager.INSTANCE.missingMaterial();
	}
}
