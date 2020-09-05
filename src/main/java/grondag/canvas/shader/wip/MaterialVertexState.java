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

package grondag.canvas.shader.wip;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.fermion.bits.BitPacker32;
import grondag.fermion.bits.BitPacker32.BooleanElement;
import grondag.fermion.bits.BitPacker32.IntElement;

/**
 * Encapsulates material state conveyed via vertex attributes
 */
@SuppressWarnings("rawtypes")
public class MaterialVertexState {
	private static final BitPacker32 PACKER = new BitPacker32<>(null, null);
	public static final int STATE_COUNT = 1 << PACKER.bitLength();
	// these 8 correspond to shader flag bits
	private static final BooleanElement EMISSIVE = PACKER.createBooleanElement();
	private static final BooleanElement DISABLE_DIFFUSE = PACKER.createBooleanElement();
	private static final BooleanElement DISABLE_AO = PACKER.createBooleanElement();
	private static final BooleanElement CUTOUT = PACKER.createBooleanElement();
	private static final BooleanElement UNMIPPED = PACKER.createBooleanElement();

	// WIP: flat? (equivalent of shadeModel - need to look at how it is used)
	// WIP: cutout threshold: 10% or 50%
	@SuppressWarnings("unused")
	private static final BooleanElement RESERVED_5 = PACKER.createBooleanElement();
	@SuppressWarnings("unused")
	private static final BooleanElement RESERVED_6 = PACKER.createBooleanElement();
	@SuppressWarnings("unused")
	private static final BooleanElement RESERVED_7 = PACKER.createBooleanElement();
	private static final IntElement CONDITION = PACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);
	private static final MaterialVertexState[] STATES = new MaterialVertexState[1 << PACKER.bitLength()];

	static {
		assert MaterialConditionImpl.ALWAYS.index == 0;

		for (int i = 0; i < STATE_COUNT; ++i) {
			STATES[i] = new MaterialVertexState(i);
		}
	}

	public final boolean emissive;
	public final boolean disableDiffuse;
	public final boolean disableAo;
	public final boolean cutout;
	public final boolean unmipped;
	public final MaterialConditionImpl condition;
	public final int bits;

	private MaterialVertexState(int bits) {
		this.bits = bits;
		emissive = EMISSIVE.getValue(bits);
		disableDiffuse = DISABLE_DIFFUSE.getValue(bits);
		disableAo = DISABLE_AO.getValue(bits);
		cutout = CUTOUT.getValue(bits);
		unmipped = UNMIPPED.getValue(bits);
		condition = MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
	}

	public static MaterialVertexState fromBits(int bits) {
		return STATES[bits];
	}

	public static class Finder {
		private int bits = 0;

		public Finder() {
			reset();
		}

		public Finder reset() {
			bits = 0;
			return this;
		}

		public Finder emissive(boolean emissive) {
			bits = EMISSIVE.setValue(emissive, bits);
			return this;
		}

		public Finder disableDiffuse(boolean disableDiffuse) {
			bits = DISABLE_DIFFUSE.setValue(disableDiffuse, bits);
			return this;
		}

		public Finder disableAo(boolean disableAo) {
			bits = DISABLE_AO.setValue(disableAo, bits);
			return this;
		}

		public Finder cutout(boolean cutout) {
			bits = CUTOUT.setValue(cutout, bits);
			return this;
		}

		public Finder unmipped(boolean unmipped) {
			bits = UNMIPPED.setValue(unmipped, bits);
			return this;
		}

		public Finder condition(MaterialConditionImpl condition) {
			bits = CONDITION.setValue(condition.index, bits);
			return this;
		}

		public int findBits() {
			return bits;
		}

		public MaterialVertexState find() {
			return STATES[bits];
		}
	}
}
