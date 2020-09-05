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

package grondag.canvas.varia;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockPosHelper {

	private static final OffsetFunc[] FACE_OFFSETS = new OffsetFunc[6];

	static {
		FACE_OFFSETS[Direction.UP.ordinal()] = (t, s) -> {
			t.set(s.getX(), s.getY() + 1, s.getZ());
			return t;
		};

		FACE_OFFSETS[Direction.DOWN.ordinal()] = (t, s) -> {
			t.set(s.getX(), s.getY() - 1, s.getZ());
			return t;
		};

		FACE_OFFSETS[Direction.NORTH.ordinal()] = (t, s) -> {
			t.set(s.getX(), s.getY(), s.getZ() - 1);
			return t;
		};

		FACE_OFFSETS[Direction.SOUTH.ordinal()] = (t, s) -> {
			t.set(s.getX(), s.getY(), s.getZ() + 1);
			return t;
		};

		FACE_OFFSETS[Direction.EAST.ordinal()] = (t, s) -> {
			t.set(s.getX() + 1, s.getY(), s.getZ());
			return t;
		};

		FACE_OFFSETS[Direction.WEST.ordinal()] = (t, s) -> {
			t.set(s.getX() - 1, s.getY(), s.getZ());
			return t;
		};
	}

	public static BlockPos.Mutable fastFaceOffset(BlockPos.Mutable target, BlockPos start, int faceOrdinal) {
		return FACE_OFFSETS[faceOrdinal].offset(target, start);
	}

	@FunctionalInterface
	private interface OffsetFunc {
		BlockPos.Mutable offset(BlockPos.Mutable target, BlockPos start);
	}
}
