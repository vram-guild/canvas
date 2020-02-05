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
package grondag.canvas.light;

import net.minecraft.util.math.MathHelper;

import grondag.fermion.bits.BitPacker64;
import grondag.fermion.bits.BitPacker64.BooleanElement;
import grondag.fermion.bits.BitPacker64.IntElement;

@SuppressWarnings("rawtypes")
public final class LightKey {
	private static final BitPacker64<Void> PACKER = new BitPacker64<>(null, null);

	private static final IntElement CENTER = PACKER.createIntElement(-1, 60);

	private static final IntElement TOP = PACKER.createIntElement(-1, 60);
	private static final IntElement LEFT = PACKER.createIntElement(-1, 60);
	private static final IntElement RIGHT = PACKER.createIntElement(-1, 60);
	private static final IntElement BOTTOM = PACKER.createIntElement(-1, 60);

	private static final IntElement TOP_LEFT = PACKER.createIntElement(-1, 60);
	private static final IntElement TOP_RIGHT = PACKER.createIntElement(-1, 60);
	private static final IntElement BOTTOM_LEFT = PACKER.createIntElement(-1, 60);
	private static final IntElement BOTTOM_RIGHT = PACKER.createIntElement(-1, 60);

	private static final BooleanElement IS_AO = PACKER.createBooleanElement();

	static long toLightmapKey(
			int top,
			int left,
			int right,
			int bottom,
			int topLeft,
			int topRight,
			int bottomLeft,
			int bottomRight,
			int center)
	{
		long result = CENTER.setValue(clamp240(center), 0);

		result = TOP.setValue(clamp240(top), result);
		result = LEFT.setValue(clamp240(left), result);
		result = RIGHT.setValue(clamp240(right), result);
		result = BOTTOM.setValue(clamp240(bottom), result);

		result = TOP_LEFT.setValue(clamp240(topLeft), result);
		result = TOP_RIGHT.setValue(clamp240(topRight), result);
		result = BOTTOM_LEFT.setValue(clamp240(bottomLeft), result);
		result = BOTTOM_RIGHT.setValue(clamp240(bottomRight), result);

		result = IS_AO.setValue(false, result);

		return result;
	}

	static long toAoKey(
			int topLeft,
			int topRight,
			int bottomLeft,
			int bottomRight)
	{
		long result = IS_AO.setValue(true, 0);

		result = TOP_LEFT.setValue(MathHelper.clamp(topLeft + 2, 0, 255) / 5, result);
		result = TOP_RIGHT.setValue(MathHelper.clamp(topRight + 2, 0, 255) / 5, result);
		result = BOTTOM_LEFT.setValue(MathHelper.clamp(bottomLeft + 2, 0, 255) / 5, result);
		result = BOTTOM_RIGHT.setValue(MathHelper.clamp(bottomRight + 2, 0, 255) / 5, result);

		return result;
	}

	private static int clamp240(int val) {
		if(val < 0 || val == 0xFF) {
			return -1;
		} else if(val > 236) {
			return 30;
		}
		return (val + 4) >> 3; // 0-30
	}

	private static int unclamp240(int val) {
		return val == -1 ? AoFaceData.OPAQUE : val << 3;
	}

	public static int center(long key) {
		return unclamp240(CENTER.getValue(key));
	}

	public static int top(long key) {
		return unclamp240(TOP.getValue(key));
	}

	public static int left(long key) {
		return unclamp240(LEFT.getValue(key));
	}

	public static int right(long key) {
		return unclamp240(RIGHT.getValue(key));
	}

	public static int bottom(long key) {
		return unclamp240(BOTTOM.getValue(key));
	}

	public static int topLeft(long key) {
		return unclamp240(TOP_LEFT.getValue(key));
	}

	public static int topRight(long key) {
		return unclamp240(TOP_RIGHT.getValue(key));
	}

	public static int bottomLeft(long key) {
		return unclamp240(BOTTOM_LEFT.getValue(key));
	}

	public static int bottomRight(long key) {
		return unclamp240(BOTTOM_RIGHT.getValue(key));
	}

	public static int topLeftAo(long key) {
		return TOP_LEFT.getValue(key) * 5;
	}

	public static int topRightAo(long key) {
		return TOP_RIGHT.getValue(key) * 5;
	}

	public static int bottomLeftAo(long key) {
		return BOTTOM_LEFT.getValue(key) * 5;
	}

	public static int bottomRightAo(long key) {
		return BOTTOM_RIGHT.getValue(key) * 5;
	}

	public static boolean isAo(long key) {
		return IS_AO.getValue(key);
	}
}
