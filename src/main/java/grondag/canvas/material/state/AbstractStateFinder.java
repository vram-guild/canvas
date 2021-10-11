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

import net.minecraft.resources.ResourceLocation;

import io.vram.frex.api.material.MaterialCondition;
import io.vram.frex.api.material.RenderMaterial;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.data.ShaderStrings;

@SuppressWarnings("unchecked")
public abstract class AbstractStateFinder<T extends AbstractStateFinder<T, V>, V extends AbstractRenderState> extends AbstractRenderStateView {
	protected AbstractStateFinder() {
		super(AbstractRenderStateView.DEFAULT_BITS);
	}

	public T clear() {
		bits = AbstractRenderStateView.DEFAULT_BITS;
		return (T) this;
	}

	//public T primitive(int primitive) {
	//	if (primitive != GL21.GL_QUADS) {
	//		throw new IllegalArgumentException("Invalid GL primitive.  Currently only quads are supported.");
	//	}
	//
	//	//assert primitive <= 7;
	//	//bits = PRIMITIVE.setValue(primitive, bits);
	//	return (T) this;
	//}

	public T textureIndex(int index) {
		bits = TEXTURE.setValue(index, bits);
		return (T) this;
	}

	public T blur(boolean blur) {
		bits = BLUR.setValue(blur, bits);
		return (T) this;
	}

	public T transparency(int transparency) {
		bits = TRANSPARENCY.setValue(transparency, bits);
		return (T) this;
	}

	public T depthTest(int depthTest) {
		bits = DEPTH_TEST.setValue(depthTest, bits);
		return (T) this;
	}

	public T cull(boolean cull) {
		bits = CULL.setValue(cull, bits);
		return (T) this;
	}

	public T writeMask(int writeMask) {
		bits = WRITE_MASK.setValue(writeMask, bits);
		return (T) this;
	}

	public T foilOverlay(boolean enableGlint) {
		bits = ENABLE_GLINT.setValue(enableGlint, bits);
		return (T) this;
	}

	public T decal(int decal) {
		bits = DECAL.setValue(decal, bits);
		return (T) this;
	}

	public T discardsTexture(boolean discardsTexture) {
		bits = DISCARDS_TEXTURE.setValue(discardsTexture, bits);
		return (T) this;
	}

	public T target(int target) {
		bits = TARGET.setValue(target, bits);
		return (T) this;
	}

	public T sorted(boolean sorted) {
		bits = SORTED.setValue(sorted, bits);
		return (T) this;
	}

	public T lines(boolean lines) {
		bits = LINES.setValue(lines, bits);
		return (T) this;
	}

	public T fog(boolean fog) {
		bits = FOG.setValue(fog, bits);
		return (T) this;
	}

	public T castShadows(boolean castShadows) {
		bits = DISABLE_SHADOWS.setValue(!castShadows, bits);
		return (T) this;
	}

	public T shader(ResourceLocation vertexSource, ResourceLocation fragmentSource, ResourceLocation depthVertexSouce, ResourceLocation depthFragmentSouce) {
		if (vertexSource == null) {
			vertexSource = ShaderStrings.DEFAULT_VERTEX_SOURCE;
		}

		if (fragmentSource == null) {
			fragmentSource = ShaderStrings.DEFAULT_FRAGMENT_SOURCE;
		}

		if (depthVertexSouce == null) {
			depthVertexSouce = ShaderStrings.DEFAULT_VERTEX_SOURCE;
		}

		if (depthFragmentSouce == null) {
			depthFragmentSouce = ShaderStrings.DEFAULT_FRAGMENT_SOURCE;
		}

		bits = SHADER_ID.setValue(MaterialShaderId.find(vertexSource, fragmentSource, depthVertexSouce, depthFragmentSouce).index, bits);
		return (T) this;
	}

	public T shader(ResourceLocation vertexSource, ResourceLocation fragmentSource) {
		return shader(vertexSource, fragmentSource, ShaderStrings.DEFAULT_VERTEX_SOURCE, ShaderStrings.DEFAULT_FRAGMENT_SOURCE);
	}

	public T emissive(boolean emissive) {
		bits = EMISSIVE.setValue(emissive, bits);
		return (T) this;
	}

	public T disableDiffuse(boolean disableDiffuse) {
		bits = DISABLE_DIFFUSE.setValue(disableDiffuse, bits);
		return (T) this;
	}

	public T disableAo(boolean disableAo) {
		bits = DISABLE_AO.setValue(disableAo, bits);
		return (T) this;
	}

	public T cutout(int cutout) {
		bits = CUTOUT.setValue(cutout, bits);
		return (T) this;
	}

	public T unmipped(boolean unmipped) {
		bits = UNMIPPED.setValue(unmipped, bits);
		return (T) this;
	}

	/**
	 * Used in lieu of overlay texture.  Displays red blended overlay color.
	 */
	public T hurtOverlay(boolean hurtOverlay) {
		bits = HURT_OVERLAY.setValue(hurtOverlay, bits);
		return (T) this;
	}

	/**
	 * Used in lieu of overlay texture. Displays white blended overlay color.
	 */
	public T flashOverlay(boolean flashOverlay) {
		bits = FLASH_OVERLAY.setValue(flashOverlay, bits);
		return (T) this;
	}

	public T preset(int preset) {
		bits = PRESET.setValue(preset, bits);
		return (T) this;
	}

	public T disableColorIndex(boolean disable) {
		bits = DISABLE_COLOR_INDEX.setValue(disable, bits);
		return (T) this;
	}

	public T condition(MaterialCondition condition) {
		bits = CONDITION.setValue(((MaterialConditionImpl) condition).index, bits);
		return (T) this;
	}

	public T copyFrom(RenderMaterial material) {
		return copyFrom((V) material);
	}

	public V find() {
		return findInner();
	}

	public V fromBits(long bits) {
		this.bits = bits;
		return findInner();
	}

	protected abstract V findInner();

	public T copyFrom(V template) {
		bits = template.bits;
		return (T) this;
	}
}
