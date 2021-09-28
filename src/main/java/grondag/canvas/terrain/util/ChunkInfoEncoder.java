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

package grondag.canvas.terrain.util;

import net.minecraft.core.Direction;

import io.vram.frex.api.model.ModelHelper;

public class ChunkInfoEncoder {
	private static final int FACE_SHIFT = 22;
	private static final int FLAGS_SHIFT = 22 + 3;
	private static final int INDEX_MASK = (1 << 22) - 1;

	public static int encodeChunkInfo(int chunkIndex, Direction entryFace, int backtrackFlags) {
		return chunkIndex | (ModelHelper.toFaceIndex(entryFace) << FACE_SHIFT) | (backtrackFlags << FLAGS_SHIFT);
	}

	public static int decodeChunkIndex(int bits) {
		return bits & INDEX_MASK;
	}

	public static Direction decodeEntryFaceOrdinal(int bits) {
		return ModelHelper.faceFromIndex((bits >> FACE_SHIFT) & 7);
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
