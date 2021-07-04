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

package grondag.canvas.material.state;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL21;

import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.material.property.MaterialTextureState;
import grondag.canvas.shader.MaterialShaderId;
import grondag.canvas.shader.data.ShaderStrings;
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.RenderMaterial;

@SuppressWarnings("unchecked")
public abstract class AbstractStateFinder<T extends AbstractStateFinder<T, V>, V extends AbstractRenderState> extends AbstractRenderStateView {
	protected AbstractStateFinder() {
		super(AbstractRenderStateView.DEFAULT_BITS);
	}

	public T clear() {
		bits = AbstractRenderStateView.DEFAULT_BITS;
		return (T) this;
	}

	public T primitive(int primitive) {
		if (primitive != GL21.GL_QUADS) {
			throw new IllegalArgumentException("Invalid GL primitive.  Currently only quads are supported.");
		}

		//assert primitive <= 7;
		//bits = PRIMITIVE.setValue(primitive, bits);
		return (T) this;
	}

	public T texture(@Nullable Identifier id) {
		final int val = id == null ? MaterialTextureState.NO_TEXTURE.index : MaterialTextureState.fromId(id).index;
		bits = TEXTURE.setValue(val, bits);
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

	public T enableGlint(boolean enableGlint) {
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

	public T shader(Identifier vertexSource, Identifier fragmentSource, Identifier depthVertexSouce, Identifier depthFragmentSouce) {
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

	public T shader(Identifier vertexSource, Identifier fragmentSource) {
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

	public T blendMode(BlendMode blendMode) {
		bits = BLENDMODE.setValue(blendMode, bits);
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

	protected abstract V missing();

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
