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

import grondag.canvas.CanvasMod;
import grondag.canvas.shader.MaterialShaderManager;
import grondag.frex.api.material.MaterialCondition;

public class MaterialConditionImpl implements MaterialCondition {
	public final BooleanSupplier supplier;
	public final boolean affectItems;
	public final boolean affectBlocks;
	public final int index;
	private int frameIndex;
	private boolean result;

	MaterialConditionImpl(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
		this.supplier = supplier;
		this.affectBlocks = affectBlocks;
		this.affectItems = affectItems;

		synchronized (CONDITIONS) {
			index = nextIndex++;
			CONDITIONS[index] = this;
		}
	}

	@Override
	public boolean compute() {
		final int frameIndex = MaterialShaderManager.INSTANCE.frameIndex();

		if (frameIndex == this.frameIndex) {
			return result;
		} else {
			final boolean result = supplier.getAsBoolean();
			this.result = result;
			return result;
		}
	}

	public static final int CONDITION_FLAG_ARRAY_LENGTH = 2;
	public static final int MAX_CONDITIONS = CONDITION_FLAG_ARRAY_LENGTH * 32;
	private static MaterialConditionImpl[] CONDITIONS = new MaterialConditionImpl[MAX_CONDITIONS];
	private static int nextIndex = 0;
	public static final MaterialConditionImpl ALWAYS = new MaterialConditionImpl(() -> true, false, false);

	public static MaterialConditionImpl fromIndex(int index) {
		return CONDITIONS[index];
	}

	public static MaterialCondition create(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
		if (nextIndex >= MAX_CONDITIONS) {
			CanvasMod.LOG.error("Unable to create new render condition because max conditions have already been created.  Some renders may not work correctly.");
			return ALWAYS;
		} else {
			return new MaterialConditionImpl(supplier, affectBlocks, affectItems);
		}
	}

	public static final int[] CONDITION_FLAGS = new int[CONDITION_FLAG_ARRAY_LENGTH];
	private static final int[] CONDITION_FLAGS_BUILD = new int[CONDITION_FLAG_ARRAY_LENGTH];

	/**
	 * Returns true if any flag changed.
	 */
	public static boolean refreshFlags() {
		for (int i = 0; i < CONDITION_FLAG_ARRAY_LENGTH; ++i) {
			CONDITION_FLAGS_BUILD[i] = 0;
		}

		for (int i = 0; i < nextIndex; ++i) {
			if (CONDITIONS[i].compute()) {
				CONDITION_FLAGS_BUILD[i >> 5] |= 1 << (i & 31);
			}
		}

		for (int i = 0; i < CONDITION_FLAG_ARRAY_LENGTH; ++i) {
			if (CONDITION_FLAGS_BUILD[i] != CONDITION_FLAGS[i]) {
				System.arraycopy(CONDITION_FLAGS_BUILD, 0, CONDITION_FLAGS, 0, CONDITION_FLAG_ARRAY_LENGTH);
				return true;
			}
		}

		return false;
	}
}
