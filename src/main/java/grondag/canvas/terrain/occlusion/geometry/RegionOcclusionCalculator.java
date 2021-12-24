/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.terrain.occlusion.geometry;

import static grondag.canvas.terrain.util.RenderRegionStateIndexer.INTERIOR_CACHE_WORDS;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.INTERIOR_STATE_COUNT;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.TOTAL_CACHE_WORDS;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.interiorIndex;
import static grondag.canvas.terrain.util.RenderRegionStateIndexer.regionIndex;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.model.util.FaceUtil;

import grondag.bitraster.PackedBox;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.Pipeline;

public abstract class RegionOcclusionCalculator {
	public static final int OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX = 0;
	public static final int OCCLUSION_RESULT_FIRST_BOX_INDEX = 1;
	public static final int[] EMPTY_OCCLUSION_DATA = {PackedBox.EMPTY_BOX};
	public static final OcclusionResult EMPTY_OCCLUSION_RESULT = new OcclusionResult(EMPTY_OCCLUSION_DATA, -1L);

	private static final int RENDERABLE_OFFSET = TOTAL_CACHE_WORDS;
	private static final int EXTERIOR_VISIBLE_OFFSET = RENDERABLE_OFFSET + TOTAL_CACHE_WORDS;
	private static final int WORD_COUNT = EXTERIOR_VISIBLE_OFFSET + TOTAL_CACHE_WORDS;
	static final long[] EMPTY_BITS = new long[WORD_COUNT];
	private static final long[] EXTERIOR_MASK = new long[INTERIOR_CACHE_WORDS];

	static {
		for (int i = 0; i < 4096; i++) {
			final int x = i & 15;
			final int y = (i >> 4) & 15;
			final int z = (i >> 8) & 15;

			if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
				EXTERIOR_MASK[(i >> 6)] |= (1L << (i & 63));
			}
		}
	}

	public final BoxFinder boxFinder = new BoxFinder(new AreaFinder());
	private final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
	private final long[] bits = new long[WORD_COUNT];
	private int openCount;
	private int minRenderableX;
	private int minRenderableY;
	private int minRenderableZ;
	private int maxRenderableX;
	private int maxRenderableY;
	private int maxRenderableZ;

	/**
	 * Accumulates exterior faces visited during fill to track which exterior
	 * faces are connected. Used for occlusion.
	 */
	private int visitedFacesMask;

	public void prepare() {
		System.arraycopy(EMPTY_BITS, 0, bits, 0, WORD_COUNT);
		captureExterior();
		openCount = INTERIOR_STATE_COUNT;
		captureInterior();
	}

	protected abstract BlockState blockStateAtIndex(int regionIndex);

	protected abstract boolean closedAtRelativePos(BlockState blockState, int regionIndex);

	public boolean isClosed(int index) {
		return (bits[(index >> 6)] & (1L << (index & 63))) != 0;
	}

	public boolean shouldRender(int interiorIndex) {
		return (bits[(interiorIndex >> 6) + RENDERABLE_OFFSET] & (1L << (interiorIndex & 63))) != 0;
	}

	protected void setVisibility(int index, boolean isRenderable, boolean isClosed) {
		final long mask = (1L << (index & 63));
		final int baseIndex = index >> 6;

		if (isClosed) {
			--openCount;
			bits[baseIndex] |= mask;
		}

		if (isRenderable) {
			bits[baseIndex + RENDERABLE_OFFSET] |= mask;
		}
	}

	private void captureInteriorVisibility(int regionIndex) {
		final BlockState blockState = blockStateAtIndex(regionIndex);

		if (blockState.getRenderShape() != RenderShape.INVISIBLE || !blockState.getFluidState().isEmpty()) {
			final boolean closed = closedAtRelativePos(blockState, regionIndex) || (Configurator.renderWhiteGlassAsOccluder && blockState.getBlock() == Blocks.WHITE_STAINED_GLASS);
			setVisibility(regionIndex, true, closed);
		}
	}

	private void captureInterior() {
		for (int i = 0; i < INTERIOR_STATE_COUNT; i++) {
			captureInteriorVisibility(i);
		}
	}

	/**
	 * Count of positions from adjacent regions that can potentially obscure this region.
	 * Is also the count of positions in the interior region that can be obscured.
	 * Six 16x16 faces. Does not include diagonally adjacent corners and edges
	 * because those can't occlude the interior region.
	 */
	private static final int COVERING_INDEX_COUNT = 16 * 16 * 6;

	/**
	 * Positions from adjacent regions that can potentially obscure this region.
	 */
	private static final int[] COVERING_INDEXES = new int[COVERING_INDEX_COUNT];

	/**
	 * Surface positions in the interior region that can potentially be obscured by adjacent regions.
	 */
	private static final int[] COVERED_INDEXES = new int[COVERING_INDEX_COUNT];

	/**
	 * Count of corner and edge positions from adjacent regions that aren't used for
	 * exterior occlusion but still need to be present to support for AO calcs.
	 * These are the exterior positions not part of COVERING_INDEXES.
	 */
	private static final int EDGE_INDEX_COUNT = 16 * 12 + 8;

	/**
	 * Exterior edge (and corner) positions from adjacent regions needed for AO calc.
	 */
	private static final int[] EDGE_INDEXES = new int[EDGE_INDEX_COUNT];

	static {
		int exteriorIndex = 0;

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				COVERING_INDEXES[exteriorIndex] = regionIndex(-1, i, j);
				COVERED_INDEXES[exteriorIndex++] = regionIndex(0, i, j);
			}
		}

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				COVERING_INDEXES[exteriorIndex] = regionIndex(16, i, j);
				COVERED_INDEXES[exteriorIndex++] = regionIndex(15, i, j);
			}
		}

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				COVERING_INDEXES[exteriorIndex] = regionIndex(i, j, -1);
				COVERED_INDEXES[exteriorIndex++] = regionIndex(i, j, 0);
			}
		}

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				COVERING_INDEXES[exteriorIndex] = regionIndex(i, j, 16);
				COVERED_INDEXES[exteriorIndex++] = regionIndex(i, j, 15);
			}
		}

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				COVERING_INDEXES[exteriorIndex] = regionIndex(i, -1, j);
				COVERED_INDEXES[exteriorIndex++] = regionIndex(i, 0, j);
			}
		}

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				COVERING_INDEXES[exteriorIndex] = regionIndex(i, 16, j);
				COVERED_INDEXES[exteriorIndex++] = regionIndex(i, 15, j);
			}
		}

		assert exteriorIndex == COVERING_INDEX_COUNT;

		exteriorIndex = 0;

		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, -1, i);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, 16, i);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(16, -1, i);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(16, 16, i);

		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, i, -1);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, i, 16);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(16, i, -1);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(16, i, 16);

		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(i, -1, -1);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(i, -1, 16);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(i, 16, -1);
		for (int i = 0; i < 16; i++) EDGE_INDEXES[exteriorIndex++] = regionIndex(i, 16, 16);

		EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, -1, -1);
		EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, -1, 16);
		EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, 16, -1);
		EDGE_INDEXES[exteriorIndex++] = regionIndex(-1, 16, 16);
		EDGE_INDEXES[exteriorIndex++] = regionIndex(16, -1, -1);
		EDGE_INDEXES[exteriorIndex++] = regionIndex(16, -1, 16);
		EDGE_INDEXES[exteriorIndex++] = regionIndex(16, 16, -1);
		EDGE_INDEXES[exteriorIndex++] = regionIndex(16, 16, 16);

		assert exteriorIndex == EDGE_INDEX_COUNT;
	}

	private void captureExteriorVisibility(int regionIndex) {
		final BlockState blockState = blockStateAtIndex(regionIndex);

		if ((blockState.getRenderShape() != RenderShape.INVISIBLE || !blockState.getFluidState().isEmpty()) && closedAtRelativePos(blockState, regionIndex)) {
			setVisibility(regionIndex, false, true);
		}
	}

	private void captureExterior() {
		for (int i = 0; i < COVERING_INDEX_COUNT; i++) {
			captureExteriorVisibility(COVERING_INDEXES[i]);
		}

		for (int i = 0; i < EDGE_INDEX_COUNT; i++) {
			captureExteriorVisibility(EDGE_INDEXES[i]);
		}
	}

	/**
	 * Checks if the position is interior and not already visited.
	 * If the position is interior and not already visited, marks it visited
	 * and returns a boolean indicating opacity.
	 *
	 * @param index
	 * @return True if position met the conditions for visiting AND was not opaque.
	 */
	private boolean setVisited(int index) {
		// interior only
		if (index >= INTERIOR_STATE_COUNT) {
			return false;
		}

		final int wordIndex = index >> 6;
		final long mask = 1L << (index & 63);

		if ((bits[wordIndex + EXTERIOR_VISIBLE_OFFSET] & mask) == 0) {
			// not already visited

			// mark visited
			bits[wordIndex + EXTERIOR_VISIBLE_OFFSET] |= mask;

			// return opacity result
			if ((bits[wordIndex] & mask) == 0) {
				if (!Pipeline.advancedTerrainCulling()) {
					trackVistedFaces(index);
				}

				return true;
			} else {
				return false;
			}
		} else {
			// already visited
			return false;
		}
	}

	private void clearInteriorRenderable(int interiorIndex) {
		bits[(interiorIndex >> 6) + RENDERABLE_OFFSET] &= ~(1L << (interiorIndex & 63));
	}

	// Pre-compute the position indices for exterior visibility tests to reduce computation overhead

	// 14x14 blocks per face (not counting edges and corners), six faces, one target and one test position each.
	private static final int FACE_VISIBILITY_COUNT = 14 * 14 * 2 * 6;
	private static final int[] FACE_VISIBILITY_TESTS = new int[FACE_VISIBILITY_COUNT];

	// 14 blocks per edge (not counting corners), 12 edges, one position and two test positions per edge block
	private static final int EDGE_VISIBILITY_COUNT = 14 * 12 * 3;
	private static final int[] EDGE_VISIBILITY_TESTS = new int[EDGE_VISIBILITY_COUNT];

	// 8 corners, one target position and three test positions per corner
	private static final int CORNER_VISIBILITY_COUNT = 8 * 4;
	private static final int[] CORNER_VISIBILITY_TESTS = new int[CORNER_VISIBILITY_COUNT];

	static {
		int n = 0;

		for (int i = 1; i < 15; i++) {
			for (int j = 1; j < 15; j++) {
				FACE_VISIBILITY_TESTS[n++] = regionIndex(-1, i, j);
				FACE_VISIBILITY_TESTS[n++] = interiorIndex(0, i, j);

				FACE_VISIBILITY_TESTS[n++] = regionIndex(16, i, j);
				FACE_VISIBILITY_TESTS[n++] = interiorIndex(15, i, j);

				FACE_VISIBILITY_TESTS[n++] = regionIndex(i, j, -1);
				FACE_VISIBILITY_TESTS[n++] = interiorIndex(i, j, 0);

				FACE_VISIBILITY_TESTS[n++] = regionIndex(i, j, 16);
				FACE_VISIBILITY_TESTS[n++] = interiorIndex(i, j, 15);

				FACE_VISIBILITY_TESTS[n++] = regionIndex(i, -1, j);
				FACE_VISIBILITY_TESTS[n++] = interiorIndex(i, 0, j);

				FACE_VISIBILITY_TESTS[n++] = regionIndex(i, 16, j);
				FACE_VISIBILITY_TESTS[n++] = interiorIndex(i, 15, j);
			}
		}

		assert n == FACE_VISIBILITY_COUNT;

		n = 0;

		for (int i = 1; i < 15; i++) {
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(-1, 0, i);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(0, -1, i);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(0, 0, i);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(16, 0, i);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(15, -1, i);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(15, 0, i);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(-1, 15, i);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(0, 16, i);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(0, 15, i);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(16, 15, i);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(15, 16, i);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(15, 15, i);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, 0, -1);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, -1, 0);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(i, 0, 0);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, 0, 16);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, -1, 15);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(i, 0, 15);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, 15, -1);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, 16, 0);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(i, 15, 0);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, 15, 16);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(i, 16, 15);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(i, 15, 15);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(-1, i, 0);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(0, i, -1);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(0, i, 0);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(16, i, 0);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(15, i, -1);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(15, i, 0);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(-1, i, 15);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(0, i, 16);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(0, i, 15);

			EDGE_VISIBILITY_TESTS[n++] = regionIndex(16, i, 15);
			EDGE_VISIBILITY_TESTS[n++] = regionIndex(15, i, 16);
			EDGE_VISIBILITY_TESTS[n++] = interiorIndex(15, i, 15);
		}

		assert n == EDGE_VISIBILITY_COUNT;

		n = 0;

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(-1, 0, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, -1, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, 0, -1);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(0, 0, 0);

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(16, 0, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, -1, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, 0, -1);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(15, 0, 0);

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(-1, 15, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, 16, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, 15, -1);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(0, 15, 0);

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(16, 15, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, 16, 0);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, 15, -1);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(15, 15, 0);

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(-1, 0, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, -1, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, 0, 16);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(0, 0, 15);

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(16, 0, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, -1, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, 0, 16);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(15, 0, 15);

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(-1, 15, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, 16, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(0, 15, 16);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(0, 15, 15);

		CORNER_VISIBILITY_TESTS[n++] = regionIndex(16, 15, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, 16, 15);
		CORNER_VISIBILITY_TESTS[n++] = regionIndex(15, 15, 16);
		CORNER_VISIBILITY_TESTS[n++] = interiorIndex(15, 15, 15);

		assert n == CORNER_VISIBILITY_COUNT;
	}

	private void adjustSurfaceVisibility() {
		// mask renderable to surface only
		for (int i = 0; i < 64; i++) {
			bits[i + RENDERABLE_OFFSET] &= EXTERIOR_MASK[i];
		}

		// don't render face blocks obscured by neighboring chunks
		for (int n = 0; n < FACE_VISIBILITY_COUNT; n += 2) {
			if (isClosed(FACE_VISIBILITY_TESTS[n])) {
				clearInteriorRenderable(FACE_VISIBILITY_TESTS[n + 1]);
			}
		}

		// don't render edge blocks obscured by neighboring chunks
		for (int n = 0; n < EDGE_VISIBILITY_COUNT; n += 3) {
			if (isClosed(EDGE_VISIBILITY_TESTS[n]) && isClosed(EDGE_VISIBILITY_TESTS[n +1 ])) {
				clearInteriorRenderable(EDGE_VISIBILITY_TESTS[n + 2]);
			}
		}

		// don't render corner blocks obscured by neighboring chunks
		for (int n = 0; n < CORNER_VISIBILITY_COUNT; n += 4) {
			if (isClosed(CORNER_VISIBILITY_TESTS[n]) && isClosed(CORNER_VISIBILITY_TESTS[n + 1]) && isClosed(CORNER_VISIBILITY_TESTS[n + 2])) {
				clearInteriorRenderable(CORNER_VISIBILITY_TESTS[n + 3]);
			}
		}
	}

	/**
	 * Removes renderable flag and marks closed if position has no open neighbors and is not visible from exterior.
	 * Should not be called if camera may be inside the chunk!
	 */
	private void hideInteriorClosedPositions() {
		for (int i = 0; i < INTERIOR_STATE_COUNT; i++) {
			final long mask = (1L << (i & 63));
			final int wordIndex = (i >> 6);
			final int x = i & 0xF;
			final int y = (i >> 4) & 0xF;
			final int z = (i >> 8) & 0xF;

			if ((bits[wordIndex + EXTERIOR_VISIBLE_OFFSET] & mask) == 0 && x != 0 && y != 0 && z != 0 && x != 15 && y != 15 && z != 15) {
				bits[wordIndex + RENDERABLE_OFFSET] &= ~mask;
				// mark it opaque
				bits[wordIndex] |= mask;
			}
		}
	}

	private void computeRenderableBounds() {
		int worldIndex = RENDERABLE_OFFSET;

		long combined0 = 0;
		long combined1 = 0;
		long combined2 = 0;
		long combined3 = 0;

		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;

		for (int z = 0; z < 16; ++z) {
			final long w0 = bits[worldIndex++];
			final long w1 = bits[worldIndex++];
			final long w2 = bits[worldIndex++];
			final long w3 = bits[worldIndex++];

			if ((w0 | w1 | w2 | w3) != 0) {
				combined0 |= w0;
				combined1 |= w1;
				combined2 |= w2;
				combined3 |= w3;

				if (z < minZ) {
					minZ = z;
				}

				if (z > maxZ) {
					maxZ = z;
				}
			}
		}

		int yBits = 0;

		if ((combined0 & 0x000000000000FFFFL) != 0) yBits |= 0x0001;
		if ((combined0 & 0x00000000FFFF0000L) != 0) yBits |= 0x0002;
		if ((combined0 & 0x0000FFFF00000000L) != 0) yBits |= 0x0004;
		if ((combined0 & 0xFFFF000000000000L) != 0) yBits |= 0x0008;

		if ((combined1 & 0x000000000000FFFFL) != 0) yBits |= 0x0010;
		if ((combined1 & 0x00000000FFFF0000L) != 0) yBits |= 0x0020;
		if ((combined1 & 0x0000FFFF00000000L) != 0) yBits |= 0x0040;
		if ((combined1 & 0xFFFF000000000000L) != 0) yBits |= 0x0080;

		if ((combined2 & 0x000000000000FFFFL) != 0) yBits |= 0x0100;
		if ((combined2 & 0x00000000FFFF0000L) != 0) yBits |= 0x0200;
		if ((combined2 & 0x0000FFFF00000000L) != 0) yBits |= 0x0400;
		if ((combined2 & 0xFFFF000000000000L) != 0) yBits |= 0x0800;

		if ((combined3 & 0x000000000000FFFFL) != 0) yBits |= 0x1000;
		if ((combined3 & 0x00000000FFFF0000L) != 0) yBits |= 0x2000;
		if ((combined3 & 0x0000FFFF00000000L) != 0) yBits |= 0x4000;
		if ((combined3 & 0xFFFF000000000000L) != 0) yBits |= 0x8000;

		long xBits = combined0 | combined1 | combined2 | combined3;

		xBits |= (xBits >> 32);
		xBits |= (xBits >> 16);

		final int ixBits = (int) (xBits & 0xFFFFL);

		minRenderableX = Integer.numberOfTrailingZeros(ixBits);
		minRenderableY = Integer.numberOfTrailingZeros(yBits);
		minRenderableZ = minZ;
		maxRenderableX = 31 - Integer.numberOfLeadingZeros(ixBits);
		maxRenderableY = 31 - Integer.numberOfLeadingZeros(yBits);
		maxRenderableZ = maxZ < minZ ? minZ : maxZ;
	}

	private void visitSurfaceIfPossible(int index) {
		if (setVisited(index)) {
			fill(index);
		}
	}

	private OcclusionResult computeOcclusion(boolean isNear) {
		// Determine which blocks are visible by visiting exterior blocks
		// that aren't occluded by neighboring regions and doing a fill from there.
		long mutualFaceMask = 0;

		if (Pipeline.advancedTerrainCulling()) {
			for (int i = 0; i < COVERING_INDEX_COUNT; ++i) {
				if (!isClosed(COVERING_INDEXES[i])) {
					visitSurfaceIfPossible(COVERED_INDEXES[i]);
				}
			}
		} else {
			for (int i = 0; i < COVERING_INDEX_COUNT; ++i) {
				// face indices are six groups of 256, one for each face.
				// We want to reset visibility search on each new face and
				// exploit this fact to know when we progress to the next face
				if ((i & 0xFF) == 0) {
					if (visitedFacesMask != 0) {
						mutualFaceMask |= OcclusionResult.buildMutualFaceMask(visitedFacesMask);
						visitedFacesMask = 0;
					}
				}

				if (!isClosed(COVERING_INDEXES[i])) {
					visitSurfaceIfPossible(COVERED_INDEXES[i]);
				}
			}

			if (visitedFacesMask != 0) {
				mutualFaceMask |= OcclusionResult.buildMutualFaceMask(visitedFacesMask);
				visitedFacesMask = 0;
			}
		}

		if (Pipeline.advancedTerrainCulling()) {
			// don't hide inside position if we may be inside the chunk!
			if (!isNear) {
				hideInteriorClosedPositions();
			}

			computeRenderableBounds();

			final BoxFinder boxFinder = this.boxFinder;
			final IntArrayList boxes = boxFinder.boxes;

			boxFinder.findBoxes(bits, 0);

			final int boxCount = boxes.size();

			final int[] result = new int[boxCount + 1];

			int n = RegionOcclusionCalculator.OCCLUSION_RESULT_FIRST_BOX_INDEX;

			if (boxCount > 0) {
				for (int i = 0; i < boxCount; i++) {
					result[n++] = boxes.getInt(i);
				}
			}

			if (minRenderableX == Integer.MAX_VALUE) {
				result[OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX] = PackedBox.EMPTY_BOX;
			} else {
				if ((minRenderableX | minRenderableY | minRenderableZ) == 0 && (maxRenderableX & maxRenderableY & maxRenderableZ) == 15) {
					result[OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX] = PackedBox.FULL_BOX;
				} else {
					result[OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX] = PackedBox.pack(minRenderableX, minRenderableY, minRenderableZ,
							maxRenderableX + 1, maxRenderableY + 1, maxRenderableZ + 1, PackedBox.RANGE_EXTREME);
				}
			}

			return new OcclusionResult(result, 0L);
		} else {
			return new OcclusionResult(null, mutualFaceMask);
		}
	}

	public OcclusionResult build(boolean isNear) {
		if (openCount == 0) {
			// If there are no open interior positions then only surface blocks can be visible,
			// and only if they not covered by positions in adjacent sections.

			// PERF: should do this after hiding interior closed positions?
			// PERF: should still compute render box instead of assuming it is full
			//       because the only visible/renderable area could be quite small because of
			//       adjacent regions occluding most of this region
			adjustSurfaceVisibility();

			final int[] result = new int[2];
			result[OCCLUSION_RESULT_RENDERABLE_BOUNDS_INDEX] = PackedBox.FULL_BOX;

			// entire region acts as an occluder
			result[OCCLUSION_RESULT_FIRST_BOX_INDEX] = PackedBox.FULL_BOX;
			return new OcclusionResult(result, 0L);
		} else {
			return computeOcclusion(isNear);
		}
	}

	private void fill(int xyz4) {
		visit(xyz4);

		while (!queue.isEmpty()) {
			final int nextXyz4 = queue.dequeueInt();
			visit(nextXyz4);
		}
	}

	private void visit(int xyz4) {
		final int x = xyz4 & 0xF;

		if (x == 0) {
			enqueIfUnvisited(xyz4 + 1);
		} else if (x == 15) {
			enqueIfUnvisited(xyz4 - 1);
		} else {
			enqueIfUnvisited(xyz4 - 1);
			enqueIfUnvisited(xyz4 + 1);
		}

		final int y = xyz4 & 0xF0;

		if (y == 0) {
			enqueIfUnvisited(xyz4 + 0x10);
		} else if (y == 0xF0) {
			enqueIfUnvisited(xyz4 - 0x10);
		} else {
			enqueIfUnvisited(xyz4 - 0x10);
			enqueIfUnvisited(xyz4 + 0x10);
		}

		final int z = xyz4 & 0xF00;

		if (z == 0) {
			enqueIfUnvisited(xyz4 + 0x100);
		} else if (z == 0xF00) {
			enqueIfUnvisited(xyz4 - 0x100);
		} else {
			enqueIfUnvisited(xyz4 - 0x100);
			enqueIfUnvisited(xyz4 + 0x100);
		}
	}

	private void trackVistedFaces(int xyz4) {
		final int x = xyz4 & 0xF;

		if (x == 0) {
			visitedFacesMask |= FaceUtil.WEST_FLAG;
			return;
		} else if (x == 15) {
			visitedFacesMask |= FaceUtil.EAST_FLAG;
			return;
		}

		final int y = xyz4 & 0xF0;

		if (y == 0) {
			visitedFacesMask |= FaceUtil.DOWN_FLAG;
			return;
		} else if (y == 0xF0) {
			visitedFacesMask |= FaceUtil.UP_FLAG;
			return;
		}

		final int z = xyz4 & 0xF00;

		if (z == 0) {
			visitedFacesMask |= FaceUtil.NORTH_FLAG;
		} else if (z == 0xF00) {
			visitedFacesMask |= FaceUtil.SOUTH_FLAG;
		}
	}

	private void enqueIfUnvisited(int xyz4) {
		if (setVisited(xyz4)) {
			queue.enqueue(xyz4);
		}
	}
}
