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
import grondag.canvas.shader.data.IntData;
import grondag.frex.api.material.MaterialCondition;

public class MaterialConditionImpl implements MaterialCondition {
	public final BooleanSupplier supplier;
	public final boolean affectItems;
	public final boolean affectBlocks;
	public final int index;
	private final int arrayIndex;
	private final int testMask;

	MaterialConditionImpl(BooleanSupplier supplier, boolean affectBlocks, boolean affectItems) {
		this.supplier = supplier;
		this.affectBlocks = affectBlocks;
		this.affectItems = affectItems;

		synchronized (CONDITIONS) {
			index = nextIndex++;
			CONDITIONS[index] = this;
		}

		arrayIndex = index >> 5;
		testMask = (1 << (index & 31));
	}

	@Override
	public boolean compute() {
		return (CONDITION_FLAGS[arrayIndex] & testMask) != 0;
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

	private static final int[] CONDITION_FLAGS = new int[CONDITION_FLAG_ARRAY_LENGTH];

	public static void update() {
		for (int i = 0; i < CONDITION_FLAG_ARRAY_LENGTH; ++i) {
			CONDITION_FLAGS[i] = 0;
		}

		for (int i = 0; i < nextIndex; ++i) {
			if (CONDITIONS[i].supplier.getAsBoolean()) {
				CONDITION_FLAGS[i >> 5] |= (1 << (i & 31));
			}
		}

		for (int i = 0; i < CONDITION_FLAG_ARRAY_LENGTH; ++i) {
			IntData.INT_DATA.put(IntData.CONDITION_DATA_START + i, CONDITION_FLAGS[i]);
		}
	}
}
