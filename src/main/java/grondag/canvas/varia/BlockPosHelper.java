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

package grondag.canvas.varia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import io.vram.frex.api.model.ModelHelper;

public class BlockPosHelper {
	private static final OffsetFunc[] FACE_OFFSETS = new OffsetFunc[6];
	private static final int[] FACE_INDEX_OPPOSITES = new int[6];

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

		FACE_INDEX_OPPOSITES[ModelHelper.toFaceIndex(Direction.DOWN)] = ModelHelper.toFaceIndex(Direction.UP);
		FACE_INDEX_OPPOSITES[ModelHelper.toFaceIndex(Direction.UP)] = ModelHelper.toFaceIndex(Direction.DOWN);
		FACE_INDEX_OPPOSITES[ModelHelper.toFaceIndex(Direction.EAST)] = ModelHelper.toFaceIndex(Direction.WEST);
		FACE_INDEX_OPPOSITES[ModelHelper.toFaceIndex(Direction.WEST)] = ModelHelper.toFaceIndex(Direction.EAST);
		FACE_INDEX_OPPOSITES[ModelHelper.toFaceIndex(Direction.NORTH)] = ModelHelper.toFaceIndex(Direction.SOUTH);
		FACE_INDEX_OPPOSITES[ModelHelper.toFaceIndex(Direction.SOUTH)] = ModelHelper.toFaceIndex(Direction.NORTH);
	}

	public static BlockPos.MutableBlockPos fastFaceOffset(BlockPos.MutableBlockPos target, BlockPos start, int faceOrdinal) {
		return FACE_OFFSETS[faceOrdinal].offset(target, start);
	}

	@FunctionalInterface
	private interface OffsetFunc {
		BlockPos.MutableBlockPos offset(BlockPos.MutableBlockPos target, BlockPos start);
	}

	public static int oppositeFaceIndex(int faceIndex) {
		return FACE_INDEX_OPPOSITES[faceIndex];
	}
}
