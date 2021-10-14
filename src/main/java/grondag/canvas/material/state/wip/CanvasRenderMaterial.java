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

package grondag.canvas.material.state.wip;

import io.vram.frex.base.renderer.material.BaseMaterialView;
import io.vram.frex.base.renderer.material.BaseRenderMaterial;
import io.vram.frex.base.renderer.util.ResourceCache;

import grondag.canvas.material.property.TextureMaterialState;
import grondag.canvas.material.state.AbstractRenderStateView;
import grondag.canvas.material.state.CollectorIndexMap;
import grondag.canvas.material.state.MaterialImpl;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.mixinterface.TextureAtlasExt;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.texture.MaterialIndexer;

public class CanvasRenderMaterial extends BaseRenderMaterial {
	protected final MaterialImpl oldMat;
	protected final ResourceCache<MaterialIndexer> indexer;
	protected final int vertexShaderIndex;
	protected final int fragmentShaderIndex;
	protected final int collectorIndex;
	protected final int shaderFlags;
	protected final RenderState renderState;

	public CanvasRenderMaterial(BaseMaterialView finder, int index) {
		super(index, finder);

		long stateBits = 0;
		stateBits = AbstractRenderStateView.BLUR.setValue(blur(), stateBits);
		stateBits = AbstractRenderStateView.DISABLE_SHADOWS.setValue(!castShadows(), stateBits);
		stateBits = AbstractRenderStateView.CONDITION.setValue(conditionIndex(), stateBits);
		stateBits = AbstractRenderStateView.CULL.setValue(cull(), stateBits);
		stateBits = AbstractRenderStateView.CUTOUT.setValue(cutout(), stateBits);
		stateBits = AbstractRenderStateView.DECAL.setValue(decal(), stateBits);
		stateBits = AbstractRenderStateView.DEPTH_TEST.setValue(depthTest(), stateBits);
		stateBits = AbstractRenderStateView.DISABLE_AO.setValue(disableAo(), stateBits);
		stateBits = AbstractRenderStateView.DISABLE_COLOR_INDEX.setValue(disableColorIndex(), stateBits);
		stateBits = AbstractRenderStateView.DISABLE_DIFFUSE.setValue(disableDiffuse(), stateBits);
		stateBits = AbstractRenderStateView.DISCARDS_TEXTURE.setValue(discardsTexture(), stateBits);
		stateBits = AbstractRenderStateView.EMISSIVE.setValue(emissive(), stateBits);
		stateBits = AbstractRenderStateView.FLASH_OVERLAY.setValue(flashOverlay(), stateBits);
		stateBits = AbstractRenderStateView.FOG.setValue(fog(), stateBits);
		stateBits = AbstractRenderStateView.ENABLE_GLINT.setValue(foilOverlay(), stateBits);
		stateBits = AbstractRenderStateView.HURT_OVERLAY.setValue(hurtOverlay(), stateBits);
		stateBits = AbstractRenderStateView.LINES.setValue(lines(), stateBits);
		stateBits = AbstractRenderStateView.PRESET.setValue(preset(), stateBits);
		stateBits = AbstractRenderStateView.SHADER_ID.setValue(shaderIndex(), stateBits);
		stateBits = AbstractRenderStateView.SORTED.setValue(sorted(), stateBits);
		stateBits = AbstractRenderStateView.TARGET.setValue(target(), stateBits);
		stateBits = AbstractRenderStateView.TEXTURE.setValue(TextureMaterialState.fromId(texture().id()).index, stateBits);
		stateBits = AbstractRenderStateView.TRANSPARENCY.setValue(transparency(), stateBits);
		stateBits = AbstractRenderStateView.UNMIPPED.setValue(unmipped(), stateBits);
		stateBits = AbstractRenderStateView.WRITE_MASK.setValue(writeMask(), stateBits);

		oldMat = MaterialImpl.finder()
			.blur(this.blur())
			.castShadows(this.castShadows())
			.conditionIndex(this.conditionIndex())
			.cull(this.cull())
			.cutout(this.cutout())
			.decal(this.decal())
			.depthTest(this.depthTest())
			.disableAo(this.disableAo())
			.disableColorIndex(this.disableColorIndex())
			.disableDiffuse(this.disableDiffuse())
			.discardsTexture(this.discardsTexture())
			.emissive(this.emissive())
			.flashOverlay(this.flashOverlay())
			.fog(this.fog())
			.foilOverlay(this.foilOverlay())
			.hurtOverlay(this.hurtOverlay())
			.label(this.label())
			.lines(this.lines())
			.preset(this.preset())
			.shaderIndex(this.shaderIndex())
			.sorted(this.sorted())
			.target(this.target())
			.textureIndex(TextureMaterialState.fromId(this.texture().id()).index)
			.transparency(this.transparency())
			.unmipped(this.unmipped())

			.writeMask(this.writeMask())
			.find();

		final long collectorKey = stateBits & AbstractRenderStateView.COLLECTOR_AND_STATE_MASK;
		collectorIndex = CollectorIndexMap.indexFromKey(collectorKey);
		shaderFlags = (int) (stateBits >>> AbstractRenderStateView.FLAG_SHIFT) & 0xFFFF;
		renderState = CollectorIndexMap.renderStateForIndex(collectorIndex);
		final var shaderId = (MaterialShaderId) this.shader();
		vertexShaderIndex = shaderId.vertexIndex;
		fragmentShaderIndex = shaderId.fragmentIndex;
		indexer = new ResourceCache<>(() -> oldMat.texture.materialIndexProvider().getIndexer(this));

		assert stateBits == oldMat.bits();
		assert collectorKey == oldMat.collectorKey();
		assert collectorIndex == oldMat.collectorIndex;
		assert shaderFlags == oldMat.shaderFlags;
		assert renderState == oldMat.renderState;
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
}
