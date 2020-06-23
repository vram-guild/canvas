package grondag.canvas.terrain;

import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

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
