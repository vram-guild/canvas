/*
 * Copyright © Original Authors
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

package grondag.canvas.terrain.util;

import net.minecraft.core.Direction;

import io.vram.frex.api.model.util.FaceUtil;

public class ChunkInfoEncoder {
	private static final int FACE_SHIFT = 22;
	private static final int FLAGS_SHIFT = 22 + 3;
	private static final int INDEX_MASK = (1 << 22) - 1;

	public static int encodeChunkInfo(int chunkIndex, Direction entryFace, int backtrackFlags) {
		return chunkIndex | (FaceUtil.toFaceIndex(entryFace) << FACE_SHIFT) | (backtrackFlags << FLAGS_SHIFT);
	}

	public static int decodeChunkIndex(int bits) {
		return bits & INDEX_MASK;
	}

	public static Direction decodeEntryFaceOrdinal(int bits) {
		return FaceUtil.faceFromIndex((bits >> FACE_SHIFT) & 7);
	}

	public static int decodeBacktrackFlags(int bits) {
		return (bits >> FLAGS_SHIFT) & 0xFF;
	}

	/**
	 * True if the direction is the opposite of a back-facing direction.
	 */
	public static boolean isBacktrack(int bits, Direction opposite) {
		final int backtrackFlags = decodeBacktrackFlags(bits);
		return (backtrackFlags & 1 << opposite.ordinal()) > 0;
	}
}
