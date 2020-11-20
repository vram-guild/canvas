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

package grondag.canvas.terrain;

import java.util.function.IntUnaryOperator;

import net.minecraft.util.math.MathHelper;

/**
 * Fast, specialized modulo arithmetic for render region addressing.
 * About 50% faster than standard routines on Intel i7 w/ JVM 8.
 *
 * <p>Exploits fact that block X, Z coordinates are in range +/- 30000000
 * to avoid special casing for negative values.
 *
 * <p>See https://www.agner.org/optimize/optimizing_assembly.pdf for math.
 */
public class FastFloorMod {
	static final int MIN = -30000000;
	static final int MAX = 30000000;

	private static final int DIAMETER_4 = diameter(4);
	private static final int SHIFT_4 = shift(4);
	private static final long MUL_4 = multiplier(4);
	private static final int OFFSET_4 = offset(4);
	private static final long CORRECTION_4 = MUL_4 * correction(4);
	private static final int DIAMETER_5 = diameter(5);
	private static final int SHIFT_5 = shift(5);
	private static final long MUL_5 = multiplier(5);
	private static final int OFFSET_5 = offset(5);
	private static final long CORRECTION_5 = MUL_5 * correction(5);
	private static final int DIAMETER_6 = diameter(6);
	private static final int SHIFT_6 = shift(6);
	private static final long MUL_6 = multiplier(6);
	private static final int OFFSET_6 = offset(6);
	private static final long CORRECTION_6 = MUL_6 * correction(6);
	private static final int DIAMETER_7 = diameter(7);
	private static final int SHIFT_7 = shift(7);
	private static final long MUL_7 = multiplier(7);
	private static final int OFFSET_7 = offset(7);
	private static final long CORRECTION_7 = MUL_7 * correction(7);
	private static final int DIAMETER_8 = diameter(8);
	private static final int SHIFT_8 = shift(8);
	private static final long MUL_8 = multiplier(8);
	private static final int OFFSET_8 = offset(8);
	private static final long CORRECTION_8 = MUL_8 * correction(8);
	private static final int DIAMETER_9 = diameter(9);
	private static final int SHIFT_9 = shift(9);
	private static final long MUL_9 = multiplier(9);
	private static final int OFFSET_9 = offset(9);
	private static final long CORRECTION_9 = MUL_9 * correction(9);
	private static final int DIAMETER_10 = diameter(10);
	private static final int SHIFT_10 = shift(10);
	private static final long MUL_10 = multiplier(10);
	private static final int OFFSET_10 = offset(10);
	private static final long CORRECTION_10 = MUL_10 * correction(10);
	private static final int DIAMETER_11 = diameter(11);
	private static final int SHIFT_11 = shift(11);
	private static final long MUL_11 = multiplier(11);
	private static final int OFFSET_11 = offset(11);
	private static final long CORRECTION_11 = MUL_11 * correction(11);
	private static final int DIAMETER_12 = diameter(12);
	private static final int SHIFT_12 = shift(12);
	private static final long MUL_12 = multiplier(12);
	private static final int OFFSET_12 = offset(12);
	private static final long CORRECTION_12 = MUL_12 * correction(12);
	private static final int DIAMETER_13 = diameter(13);
	private static final int SHIFT_13 = shift(13);
	private static final long MUL_13 = multiplier(13);
	private static final int OFFSET_13 = offset(13);
	private static final long CORRECTION_13 = MUL_13 * correction(13);
	private static final int DIAMETER_14 = diameter(14);
	private static final int SHIFT_14 = shift(14);
	private static final long MUL_14 = multiplier(14);
	private static final int OFFSET_14 = offset(14);
	private static final long CORRECTION_14 = MUL_14 * correction(14);
	private static final int DIAMETER_15 = diameter(15);
	private static final int SHIFT_15 = shift(15);
	private static final long MUL_15 = multiplier(15);
	private static final int OFFSET_15 = offset(15);
	private static final long CORRECTION_15 = MUL_15 * correction(15);
	private static final int DIAMETER_16 = diameter(16);
	private static final int SHIFT_16 = shift(16);
	private static final long MUL_16 = multiplier(16);
	private static final int OFFSET_16 = offset(16);
	private static final long CORRECTION_16 = MUL_16 * correction(16);
	private static final int DIAMETER_17 = diameter(17);
	private static final int SHIFT_17 = shift(17);
	private static final long MUL_17 = multiplier(17);
	private static final int OFFSET_17 = offset(17);
	private static final long CORRECTION_17 = MUL_17 * correction(17);
	private static final int DIAMETER_18 = diameter(18);
	private static final int SHIFT_18 = shift(18);
	private static final long MUL_18 = multiplier(18);
	private static final int OFFSET_18 = offset(18);
	private static final long CORRECTION_18 = MUL_18 * correction(18);
	private static final int DIAMETER_19 = diameter(19);
	private static final int SHIFT_19 = shift(19);
	private static final long MUL_19 = multiplier(19);
	private static final int OFFSET_19 = offset(19);
	private static final long CORRECTION_19 = MUL_19 * correction(19);
	private static final int DIAMETER_20 = diameter(20);
	private static final int SHIFT_20 = shift(20);
	private static final long MUL_20 = multiplier(20);
	private static final int OFFSET_20 = offset(20);
	private static final long CORRECTION_20 = MUL_20 * correction(20);
	private static final int DIAMETER_21 = diameter(21);
	private static final int SHIFT_21 = shift(21);
	private static final long MUL_21 = multiplier(21);
	private static final int OFFSET_21 = offset(21);
	private static final long CORRECTION_21 = MUL_21 * correction(21);
	private static final int DIAMETER_22 = diameter(22);
	private static final int SHIFT_22 = shift(22);
	private static final long MUL_22 = multiplier(22);
	private static final int OFFSET_22 = offset(22);
	private static final long CORRECTION_22 = MUL_22 * correction(22);
	private static final int DIAMETER_23 = diameter(23);
	private static final int SHIFT_23 = shift(23);
	private static final long MUL_23 = multiplier(23);
	private static final int OFFSET_23 = offset(23);
	private static final long CORRECTION_23 = MUL_23 * correction(23);
	private static final int DIAMETER_24 = diameter(24);
	private static final int SHIFT_24 = shift(24);
	private static final long MUL_24 = multiplier(24);
	private static final int OFFSET_24 = offset(24);
	private static final long CORRECTION_24 = MUL_24 * correction(24);
	private static final int DIAMETER_25 = diameter(25);
	private static final int SHIFT_25 = shift(25);
	private static final long MUL_25 = multiplier(25);
	private static final int OFFSET_25 = offset(25);
	private static final long CORRECTION_25 = MUL_25 * correction(25);
	private static final int DIAMETER_26 = diameter(26);
	private static final int SHIFT_26 = shift(26);
	private static final long MUL_26 = multiplier(26);
	private static final int OFFSET_26 = offset(26);
	private static final long CORRECTION_26 = MUL_26 * correction(26);
	private static final int DIAMETER_27 = diameter(27);
	private static final int SHIFT_27 = shift(27);
	private static final long MUL_27 = multiplier(27);
	private static final int OFFSET_27 = offset(27);
	private static final long CORRECTION_27 = MUL_27 * correction(27);
	private static final int DIAMETER_28 = diameter(28);
	private static final int SHIFT_28 = shift(28);
	private static final long MUL_28 = multiplier(28);
	private static final int OFFSET_28 = offset(28);
	private static final long CORRECTION_28 = MUL_28 * correction(28);
	private static final int DIAMETER_29 = diameter(29);
	private static final int SHIFT_29 = shift(29);
	private static final long MUL_29 = multiplier(29);
	private static final int OFFSET_29 = offset(29);
	private static final long CORRECTION_29 = MUL_29 * correction(29);
	private static final int DIAMETER_30 = diameter(30);
	private static final int SHIFT_30 = shift(30);
	private static final long MUL_30 = multiplier(30);
	private static final int OFFSET_30 = offset(30);
	private static final long CORRECTION_30 = MUL_30 * correction(30);
	private static final int DIAMETER_31 = diameter(31);
	private static final int SHIFT_31 = shift(31);
	private static final long MUL_31 = multiplier(31);
	private static final int OFFSET_31 = offset(31);
	private static final long CORRECTION_31 = MUL_31 * correction(31);
	private static final int DIAMETER_32 = diameter(32);
	private static final int SHIFT_32 = shift(32);
	private static final long MUL_32 = multiplier(32);
	private static final int OFFSET_32 = offset(32);
	private static final long CORRECTION_32 = MUL_32 * correction(32);
	private static final IntUnaryOperator[] FUNCS = new IntUnaryOperator[32 - 4 + 1];
	private static final IntUnaryOperator RADIUS_4 = x -> {
		x += OFFSET_4;
		return x - (int) ((x * MUL_4 + CORRECTION_4) >> SHIFT_4) * DIAMETER_4;
	};
	private static final IntUnaryOperator RADIUS_5 = x -> {
		x += OFFSET_5;
		return x - (int) ((x * MUL_5 + CORRECTION_5) >> SHIFT_5) * DIAMETER_5;
	};
	private static final IntUnaryOperator RADIUS_6 = x -> {
		x += OFFSET_6;
		return x - (int) ((x * MUL_6 + CORRECTION_6) >> SHIFT_6) * DIAMETER_6;
	};
	private static final IntUnaryOperator RADIUS_7 = x -> {
		x += OFFSET_7;
		return x - (int) ((x * MUL_7 + CORRECTION_7) >> SHIFT_7) * DIAMETER_7;
	};
	private static final IntUnaryOperator RADIUS_8 = x -> {
		x += OFFSET_8;
		return x - (int) ((x * MUL_8 + CORRECTION_8) >> SHIFT_8) * DIAMETER_8;
	};
	private static final IntUnaryOperator RADIUS_9 = x -> {
		x += OFFSET_9;
		return x - (int) ((x * MUL_9 + CORRECTION_9) >> SHIFT_9) * DIAMETER_9;
	};
	private static final IntUnaryOperator RADIUS_10 = x -> {
		x += OFFSET_10;
		return x - (int) ((x * MUL_10 + CORRECTION_10) >> SHIFT_10) * DIAMETER_10;
	};
	private static final IntUnaryOperator RADIUS_11 = x -> {
		x += OFFSET_11;
		return x - (int) ((x * MUL_11 + CORRECTION_11) >> SHIFT_11) * DIAMETER_11;
	};
	private static final IntUnaryOperator RADIUS_12 = x -> {
		x += OFFSET_12;
		return x - (int) ((x * MUL_12 + CORRECTION_12) >> SHIFT_12) * DIAMETER_12;
	};
	private static final IntUnaryOperator RADIUS_13 = x -> {
		x += OFFSET_13;
		return x - (int) ((x * MUL_13 + CORRECTION_13) >> SHIFT_13) * DIAMETER_13;
	};
	private static final IntUnaryOperator RADIUS_14 = x -> {
		x += OFFSET_14;
		return x - (int) ((x * MUL_14 + CORRECTION_14) >> SHIFT_14) * DIAMETER_14;
	};
	private static final IntUnaryOperator RADIUS_15 = x -> {
		x += OFFSET_15;
		return x - (int) ((x * MUL_15 + CORRECTION_15) >> SHIFT_15) * DIAMETER_15;
	};
	private static final IntUnaryOperator RADIUS_16 = x -> {
		x += OFFSET_16;
		return x - (int) ((x * MUL_16 + CORRECTION_16) >> SHIFT_16) * DIAMETER_16;
	};
	private static final IntUnaryOperator RADIUS_17 = x -> {
		x += OFFSET_17;
		return x - (int) ((x * MUL_17 + CORRECTION_17) >> SHIFT_17) * DIAMETER_17;
	};
	private static final IntUnaryOperator RADIUS_18 = x -> {
		x += OFFSET_18;
		return x - (int) ((x * MUL_18 + CORRECTION_18) >> SHIFT_18) * DIAMETER_18;
	};
	private static final IntUnaryOperator RADIUS_19 = x -> {
		x += OFFSET_19;
		return x - (int) ((x * MUL_19 + CORRECTION_19) >> SHIFT_19) * DIAMETER_19;
	};
	private static final IntUnaryOperator RADIUS_20 = x -> {
		x += OFFSET_20;
		return x - (int) ((x * MUL_20 + CORRECTION_20) >> SHIFT_20) * DIAMETER_20;
	};
	private static final IntUnaryOperator RADIUS_21 = x -> {
		x += OFFSET_21;
		return x - (int) ((x * MUL_21 + CORRECTION_21) >> SHIFT_21) * DIAMETER_21;
	};
	private static final IntUnaryOperator RADIUS_22 = x -> {
		x += OFFSET_22;
		return x - (int) ((x * MUL_22 + CORRECTION_22) >> SHIFT_22) * DIAMETER_22;
	};
	private static final IntUnaryOperator RADIUS_23 = x -> {
		x += OFFSET_23;
		return x - (int) ((x * MUL_23 + CORRECTION_23) >> SHIFT_23) * DIAMETER_23;
	};
	private static final IntUnaryOperator RADIUS_24 = x -> {
		x += OFFSET_24;
		return x - (int) ((x * MUL_24 + CORRECTION_24) >> SHIFT_24) * DIAMETER_24;
	};
	private static final IntUnaryOperator RADIUS_25 = x -> {
		x += OFFSET_25;
		return x - (int) ((x * MUL_25 + CORRECTION_25) >> SHIFT_25) * DIAMETER_25;
	};
	private static final IntUnaryOperator RADIUS_26 = x -> {
		x += OFFSET_26;
		return x - (int) ((x * MUL_26 + CORRECTION_26) >> SHIFT_26) * DIAMETER_26;
	};
	private static final IntUnaryOperator RADIUS_27 = x -> {
		x += OFFSET_27;
		return x - (int) ((x * MUL_27 + CORRECTION_27) >> SHIFT_27) * DIAMETER_27;
	};
	private static final IntUnaryOperator RADIUS_28 = x -> {
		x += OFFSET_28;
		return x - (int) ((x * MUL_28 + CORRECTION_28) >> SHIFT_28) * DIAMETER_28;
	};
	private static final IntUnaryOperator RADIUS_29 = x -> {
		x += OFFSET_29;
		return x - (int) ((x * MUL_29 + CORRECTION_29) >> SHIFT_29) * DIAMETER_29;
	};
	private static final IntUnaryOperator RADIUS_30 = x -> {
		x += OFFSET_30;
		return x - (int) ((x * MUL_30 + CORRECTION_30) >> SHIFT_30) * DIAMETER_30;
	};
	private static final IntUnaryOperator RADIUS_31 = x -> {
		x += OFFSET_31;
		return x - (int) ((x * MUL_31 + CORRECTION_31) >> SHIFT_31) * DIAMETER_31;
	};
	private static final IntUnaryOperator RADIUS_32 = x -> {
		x += OFFSET_32;
		return x - (int) ((x * MUL_32 + CORRECTION_32) >> SHIFT_32) * DIAMETER_32;
	};

	static {
		FUNCS[0] = RADIUS_4;
		FUNCS[1] = RADIUS_5;
		FUNCS[2] = RADIUS_6;
		FUNCS[3] = RADIUS_7;
		FUNCS[4] = RADIUS_8;
		FUNCS[5] = RADIUS_9;
		FUNCS[6] = RADIUS_10;
		FUNCS[7] = RADIUS_11;
		FUNCS[8] = RADIUS_12;
		FUNCS[9] = RADIUS_13;
		FUNCS[10] = RADIUS_14;
		FUNCS[11] = RADIUS_15;
		FUNCS[12] = RADIUS_16;
		FUNCS[13] = RADIUS_17;
		FUNCS[14] = RADIUS_18;
		FUNCS[15] = RADIUS_19;
		FUNCS[16] = RADIUS_20;
		FUNCS[17] = RADIUS_21;
		FUNCS[18] = RADIUS_22;
		FUNCS[19] = RADIUS_23;
		FUNCS[20] = RADIUS_24;
		FUNCS[21] = RADIUS_25;
		FUNCS[22] = RADIUS_26;
		FUNCS[23] = RADIUS_27;
		FUNCS[24] = RADIUS_28;
		FUNCS[25] = RADIUS_29;
		FUNCS[26] = RADIUS_30;
		FUNCS[27] = RADIUS_31;
		FUNCS[28] = RADIUS_32;
	}

	public static IntUnaryOperator get(int radius) {
		assert radius >= 4;
		assert radius <= 32;
		return FUNCS[radius - 4];
	}

	private static int diameter(int radius) {
		return radius * 2 + 1;
	}

	private static int offset(int radius) {
		final int d = diameter(radius);
		return (-MIN / d + 1) * d;
	}

	private static int shift(int radius) {
		final int d = diameter(radius);
		return 32 + Integer.bitCount(MathHelper.smallestEncompassingPowerOfTwo(d) - 1) - 1;
	}

	private static long uncorrectedMultiplier(int radius) {
		return (1L << shift(radius)) / diameter(radius);
	}

	private static long multiplier(int radius) {
		final int shift = shift(radius);
		final int fractionBits = shift - 32;
		final long fractionMask = (1L << fractionBits) - 1;
		final long c = uncorrectedMultiplier(radius);
		final long f = c & fractionMask;

		if (f < (1L << (fractionBits - 1))) {
			// round down when fractional part is less than half
			return c & ~fractionMask;
		} else {
			//round up otherwise
			return (c & ~fractionMask) + (1L << fractionBits);
		}
	}

	private static long correction(int radius) {
		final int shift = shift(radius);
		final int fractionBits = shift - 32;
		final long fractionMask = (1L << fractionBits) - 1;
		final long c = uncorrectedMultiplier(radius);
		final long f = c & fractionMask;

		if (f < (1L << (fractionBits - 1))) {
			// correct at run time when fractional part is less than half
			return 1;
		} else {
			// no correction otherwise
			return 0;
		}
	}
}
