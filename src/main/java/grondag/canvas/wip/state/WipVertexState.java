/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.wip.state;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.mixin.AccessMultiPhaseParameters;
import grondag.canvas.mixin.AccessTexture;
import grondag.canvas.mixinterface.MultiPhaseExt;
import grondag.fermion.bits.BitPacker32;
import grondag.fermion.bits.BitPacker32.BooleanElement;
import grondag.fermion.bits.BitPacker32.IntElement;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;

/**
 * Encapsulates material state conveyed via vertex attributes.
 */
@SuppressWarnings("rawtypes")
public class WipVertexState {
	private WipVertexState() { }

	private static final BitPacker32 PACKER = new BitPacker32<>(null, null);
	public static final int STATE_COUNT = 1 << PACKER.bitLength();
	// first 16 bits correspond to shader flag bits
	private static final BooleanElement EMISSIVE = PACKER.createBooleanElement();
	private static final BooleanElement DISABLE_DIFFUSE = PACKER.createBooleanElement();
	private static final BooleanElement DISABLE_AO = PACKER.createBooleanElement();
	private static final BooleanElement CUTOUT = PACKER.createBooleanElement();
	private static final BooleanElement UNMIPPED = PACKER.createBooleanElement();

	// true = 10%, false = 50%
	private static final BooleanElement TRANSLUCENT_CUTOUT = PACKER.createBooleanElement();
	private static final BooleanElement HURT_OVERLAY = PACKER.createBooleanElement();
	private static final BooleanElement FLASH_OVERLAY = PACKER.createBooleanElement();

	public static final int HURT_OVERLAY_FLAG = HURT_OVERLAY.comparisonMask() << 24;
	public static final int FLASH_OVERLAY_FLAG = FLASH_OVERLAY.comparisonMask() << 24;

	// WIP: implement with indexed draw
	private static final IntElement CONDITION = PACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

	static {
		assert MaterialConditionImpl.ALWAYS.index == 0;
	}

	public static boolean emissive(int vertexState) {
		return EMISSIVE.getValue(vertexState);
	}

	public static boolean disableDiffuse(int vertexState) {
		return DISABLE_DIFFUSE.getValue(vertexState);
	}

	public static boolean disableAo(int vertexState) {
		return DISABLE_AO.getValue(vertexState);
	}

	public static boolean cutout(int vertexState) {
		return CUTOUT.getValue(vertexState);
	}

	public static boolean unmipped(int vertexState) {
		return UNMIPPED.getValue(vertexState);
	}

	public static boolean translucentCutout(int vertexState) {
		return TRANSLUCENT_CUTOUT.getValue(vertexState);
	}

	public static boolean hurtOverlay(int vertexState) {
		return HURT_OVERLAY.getValue(vertexState);
	}

	public static boolean flashOverlay(int vertexState) {
		return FLASH_OVERLAY.getValue(vertexState);
	}

	public static MaterialConditionImpl condition(int vertexState) {
		return MaterialConditionImpl.fromIndex(CONDITION.getValue(vertexState));
	}

	public static int conditionIndex(int vertexState) {
		return CONDITION.getValue(vertexState);
	}

	public static int shaderFlags(int vertexState) {
		return vertexState & 0xFF;
	}

	public static class Finder {
		private int vertexState = 0;

		public Finder() {
			reset();
		}

		public Finder reset() {
			vertexState = 0;
			return this;
		}

		/**
		 * Note sets Ao disabled by default - non-terrain layers can't/won't use it.
		 */
		public Finder copyFromLayer(RenderLayer layer) {
			final MultiPhaseExt ext = (MultiPhaseExt) layer;

			final String name = ext.canvas_name();
			final AccessMultiPhaseParameters params = ext.canvas_phases();
			final AccessTexture tex = (AccessTexture) params.getTexture();
			unmipped(!tex.getMipmap());
			disableDiffuse(params.getDiffuseLighting() == RenderPhase.DISABLE_DIFFUSE_LIGHTING);
			cutout(params.getAlpha() != RenderPhase.ZERO_ALPHA);
			translucentCutout(params.getAlpha() == RenderPhase.ONE_TENTH_ALPHA);
			disableAo(true);

			// WIP: put in proper material map hooks
			emissive(name.equals("eyes") || name.equals("beacon_beam"));
			return this;
		}

		public Finder emissive(boolean emissive) {
			vertexState = EMISSIVE.setValue(emissive, vertexState);
			return this;
		}

		public Finder disableDiffuse(boolean disableDiffuse) {
			vertexState = DISABLE_DIFFUSE.setValue(disableDiffuse, vertexState);
			return this;
		}

		public Finder disableAo(boolean disableAo) {
			vertexState = DISABLE_AO.setValue(disableAo, vertexState);
			return this;
		}

		public Finder cutout(boolean cutout) {
			vertexState = CUTOUT.setValue(cutout, vertexState);
			return this;
		}

		public Finder unmipped(boolean unmipped) {
			vertexState = UNMIPPED.setValue(unmipped, vertexState);
			return this;
		}

		/**
		 * Sets cutout threshold to low value vs default of 50%
		 */
		public Finder translucentCutout(boolean translucentCutout) {
			vertexState = TRANSLUCENT_CUTOUT.setValue(translucentCutout, vertexState);
			return this;
		}

		/**
		 * Used in lieu of overlay texture.  Displays red blended overlay color.
		 */
		public Finder hurtOverlay(boolean hurtOverlay) {
			vertexState = HURT_OVERLAY.setValue(hurtOverlay, vertexState);
			return this;
		}

		/**
		 * Used in lieu of overlay texture. Displays white blended overlay color.
		 */
		public Finder flashOverlay(boolean flashOverlay) {
			vertexState = FLASH_OVERLAY.setValue(flashOverlay, vertexState);
			return this;
		}

		public int find() {
			return vertexState;
		}

		public Finder condition(MaterialConditionImpl condition) {
			vertexState = CONDITION.setValue(condition.index, vertexState);
			return this;
		}
	}

	private static ThreadLocal<Finder> FINDER = ThreadLocal.withInitial(Finder::new);

	public static Finder finder() {
		final Finder result = FINDER.get();
		return result.reset();
	}
}
