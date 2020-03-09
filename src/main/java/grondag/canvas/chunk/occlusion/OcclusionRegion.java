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
package grondag.canvas.chunk.occlusion;

import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_WORDS;
import static grondag.canvas.chunk.RenderRegionAddressHelper.TOTAL_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.TOTAL_CACHE_WORDS;
import static grondag.canvas.chunk.RenderRegionAddressHelper.fastRelativeCacheIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.interiorIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localCornerIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZfaceIndex;
import static grondag.canvas.chunk.occlusion.RegionOcclusionData.X0;
import static grondag.canvas.chunk.occlusion.RegionOcclusionData.X1;
import static grondag.canvas.chunk.occlusion.RegionOcclusionData.Y0;
import static grondag.canvas.chunk.occlusion.RegionOcclusionData.Y1;
import static grondag.canvas.chunk.occlusion.RegionOcclusionData.Z0;
import static grondag.canvas.chunk.occlusion.RegionOcclusionData.Z1;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;

public abstract class OcclusionRegion {
	private final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
	private final long[] bits = new long[WORD_COUNT];
	private int openCount;

	public void prepare() {
		System.arraycopy(EMPTY_BITS, 0, bits, 0, WORD_COUNT);
		captureFaces();
		captureEdges();
		captureCorners();

		openCount = TOTAL_CACHE_SIZE;
		captureInterior();
	}

	protected abstract BlockState blockStateAtIndex(int index);
	protected abstract boolean closedAtRelativePos(BlockState blockState, int x, int y, int z);

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

	private void captureInteriorVisbility(int index, int x, int y, int z) {
		final BlockState blockState = blockStateAtIndex(index);

		if(blockState.getRenderType() != BlockRenderType.INVISIBLE || !blockState.getFluidState().isEmpty()) {
			setVisibility(index, true, closedAtRelativePos(blockState, x, y, z));
		}
	}

	private void captureExteriorVisbility(int index, int x, int y, int z) {
		final BlockState blockState = blockStateAtIndex(index);

		if((blockState.getRenderType() != BlockRenderType.INVISIBLE || !blockState.getFluidState().isEmpty()) && closedAtRelativePos(blockState, x, y, z)) {
			setVisibility(index, false, true);
		}
	}

	private void captureInterior() {
		for(int i = 0; i <= INTERIOR_CACHE_SIZE; i++) {
			captureInteriorVisbility(i, i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xF);
		}
	}

	private void captureFaces() {
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				captureExteriorVisbility(localXfaceIndex(false, i, j), -1, i, j);
				captureExteriorVisbility(localXfaceIndex(true, i, j), 16, i, j);

				captureExteriorVisbility(localZfaceIndex(i, j, false), i, j, -1);
				captureExteriorVisbility(localZfaceIndex(i, j, true), i, j, 16);

				captureExteriorVisbility(localYfaceIndex(i, false, j), i, -1, j);
				captureExteriorVisbility(localYfaceIndex(i, true, j), i, 16, j);
			}
		}
	}

	private void captureEdges() {
		for(int i = 0; i < 16; i++) {
			captureExteriorVisbility(localZEdgeIndex(false, false, i), -1, -1, i);
			captureExteriorVisbility(localZEdgeIndex(false, true, i), -1, 16, i);
			captureExteriorVisbility(localZEdgeIndex(true, false, i), 16, -1, i);
			captureExteriorVisbility(localZEdgeIndex(true, true, i), 16, 16, i);

			captureExteriorVisbility(localYEdgeIndex(false, i, false), -1, i, -1);
			captureExteriorVisbility(localYEdgeIndex(false, i, true), -1, i, 16);
			captureExteriorVisbility(localYEdgeIndex(true, i, false), 16, i, -1);
			captureExteriorVisbility(localYEdgeIndex(true, i, true), 16, i, 16);

			captureExteriorVisbility(localXEdgeIndex(i, false, false), i, -1, -1);
			captureExteriorVisbility(localXEdgeIndex(i, false, true), i, -1, 16);
			captureExteriorVisbility(localXEdgeIndex(i, true, false), i, 16, -1);
			captureExteriorVisbility(localXEdgeIndex(i, true, true), i, 16, 16);
		}
	}

	private void captureCorners() {
		captureExteriorVisbility(localCornerIndex(false, false, false), -1, -1, -1);
		captureExteriorVisbility(localCornerIndex(false, false, true), -1, -1, 16);
		captureExteriorVisbility(localCornerIndex(false, true, false), -1, 16, -1);
		captureExteriorVisbility(localCornerIndex(false, true, true), -1, 16, 16);

		captureExteriorVisbility(localCornerIndex(true, false, false), 16, -1, -1);
		captureExteriorVisbility(localCornerIndex(true, false, true), 16, -1, 16);
		captureExteriorVisbility(localCornerIndex(true, true, false), 16, 16, -1);
		captureExteriorVisbility(localCornerIndex(true, true, true), 16, 16, 16);
	}

	private void setVisited(int index) {
		bits[(index >> 6) + EXTERIOR_VISIBLE_OFFSET] |= (1L << (index & 63));
	}

	// interior, not opaque and not already visited
	private boolean canVisit(int index) {
		// can't visit outside central chunk
		if (index >= INTERIOR_CACHE_SIZE) {
			return false;
		}

		final int baseIndex = index >> 6;
		final long mask = 1L << (index & 63);

		if((bits[baseIndex + EXTERIOR_VISIBLE_OFFSET] & mask) == 0) {
			if ((bits[baseIndex] & mask) == 0) {
				return true;
			} else {
				// if closed then mark and report as visited - only want to do this for checked positions
				bits[baseIndex + EXTERIOR_VISIBLE_OFFSET] |= mask;
				return false;
			}
		} else {
			return false;
		}
	}

	private void clearInteriorRenderable(int x, int y, int z) {
		final int index = interiorIndex(x, y, z);
		bits[(index >> 6) + RENDERABLE_OFFSET] &= ~(1L << (index & 63));
	}

	private void adjustSurfaceVisbility() {
		// mask renderable to surface only
		for(int i = 0; i < 64; i++) {
			bits[i + RENDERABLE_OFFSET] &= EXTERIOR_MASK[i];
		}

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				if (isClosed(localXfaceIndex(false, i, j))) clearInteriorRenderable(0, i, j);
				if (isClosed(localXfaceIndex(true, i, j))) clearInteriorRenderable(15, i, j);

				if (isClosed(localZfaceIndex(i, j, false))) clearInteriorRenderable(i, j, 0);
				if (isClosed(localZfaceIndex(i, j, true))) clearInteriorRenderable(i, j, 15);

				if (isClosed(localYfaceIndex(i, false, j))) clearInteriorRenderable(i, 0, j);
				if (isClosed(localYfaceIndex(i, true, j))) clearInteriorRenderable(i, 15, j);
			}
		}
	}

	/**
	 * Removes renderable flag if position has no open neighbors.
	 * Probably less expensive than a flood fill when small number of closed position
	 * and used in that scenario.
	 */
	private void hideClosedPositions() {
		for (int i = 0; i < INTERIOR_CACHE_SIZE; i++) {
			final long mask = (1L << (i & 63));
			final int wordIndex = (i >> 6) + RENDERABLE_OFFSET;

			if ((bits[wordIndex] & mask) != 0) {
				final int x = i & 0xF;
				final int y = (i >> 4) & 0xF;
				final int z = (i >> 8) & 0xF;

				if(isClosed(fastRelativeCacheIndex(x - 1, y, z)) && isClosed(fastRelativeCacheIndex(x + 1, y, z))
						&& isClosed(fastRelativeCacheIndex(x, y - 1, z)) && isClosed(fastRelativeCacheIndex(x, y + 1, z))
						&& isClosed(fastRelativeCacheIndex(x, y, z - 1)) && isClosed(fastRelativeCacheIndex(x, y, z + 1))) {

					bits[wordIndex] &=  ~mask;
				}
			}
		}
	}

	private int minRenderableX;
	private int minRenderableY;
	private int minRenderableZ;
	private int maxRenderableX;
	private int maxRenderableY;
	private int maxRenderableZ;

	/**
	 * Removes renderable flag if position has no open neighbors and is not visible from exterior.
	 */
	private void hideInteriorClosedPositions() {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;

		for (int i = 0; i < INTERIOR_CACHE_SIZE; i++) {
			final long mask = (1L << (i & 63));
			final int wordIndex = (i >> 6);

			if ((bits[wordIndex + RENDERABLE_OFFSET] & mask) != 0) {

				final int x = i & 0xF;
				final int y = (i >> 4) & 0xF;
				final int z = (i >> 8) & 0xF;

				if((bits[wordIndex + EXTERIOR_VISIBLE_OFFSET] & mask) == 0 && isClosed(fastRelativeCacheIndex(x - 1, y, z)) && isClosed(fastRelativeCacheIndex(x + 1, y, z))
						&& isClosed(fastRelativeCacheIndex(x, y - 1, z)) && isClosed(fastRelativeCacheIndex(x, y + 1, z))
						&& isClosed(fastRelativeCacheIndex(x, y, z - 1)) && isClosed(fastRelativeCacheIndex(x, y, z + 1))) {

					bits[wordIndex + RENDERABLE_OFFSET] &= ~mask;
				} else {
					if (x < minX) {
						minX = x;
					} else if (x > maxX) {
						maxX = x;
					}

					if (y < minY) {
						minY = y;
					} else if (y > maxY) {
						maxY = y;
					}

					if (z < minZ) {
						minZ = z;
					} else if (z > maxZ) {
						maxZ = z;
					}
				}
			}
		}

		minRenderableX = minX;
		minRenderableY = minY;
		minRenderableZ = minZ;
		maxRenderableX = maxX;
		maxRenderableY = maxY;
		maxRenderableZ = maxZ;
	}

	private void visitSurfaceIfPossible(int x, int y, int z) {
		final int index = interiorIndex(x, y, z);

		if (canVisit(index)) {
			fill(index);
		}
	}

	private int[] computeOcclusion() {
		//		final RegionOcclusionData result = new RegionOcclusionData(null);
		//
		//		// determine which blocks are visible
		boolean xPos = true;
		boolean xNeg = true;
		boolean yPos = true;
		boolean yNeg = true;
		boolean zPos = true;
		boolean zNeg = true;

		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				if (!isClosed(localXfaceIndex(false, i, j))) {
					visitSurfaceIfPossible(0, i, j);
					xNeg = false;
				}
				if (!isClosed(localXfaceIndex(true, i, j))) {
					visitSurfaceIfPossible(15, i, j);
					xPos = false;
				}

				if (!isClosed(localZfaceIndex(i, j, false))) {
					visitSurfaceIfPossible(i, j, 0);
					yNeg = false;
				}

				if (!isClosed(localZfaceIndex(i, j, true))) {
					visitSurfaceIfPossible(i, j, 15);
					yPos = false;
				}

				if (!isClosed(localYfaceIndex(i, false, j))) {
					visitSurfaceIfPossible(i, 0, j);
					zNeg = false;
				}

				if (!isClosed(localYfaceIndex(i, true, j))) {
					visitSurfaceIfPossible(i, 15, j);
					zPos = false;
				}
			}
		}

		hideInteriorClosedPositions();

		final int[] result = new int[7];

		if (minRenderableX == Integer.MAX_VALUE) {
			RegionOcclusionData.isEmptyChunk(result, true);

		} else {
			result[X0] = minRenderableX;
			result[Y0] = minRenderableY;
			result[Z0] = minRenderableZ;
			result[X1] = maxRenderableX + 1;
			result[Y1] = maxRenderableY + 1;
			result[Z1] = maxRenderableZ + 1;

			if ((minRenderableX | minRenderableY | minRenderableZ) == 0 && (maxRenderableX & maxRenderableY & maxRenderableZ) == 15) {
				RegionOcclusionData.isFullChunkVisible(result, true);

				if(xPos && xNeg && yPos && yNeg && zPos && zNeg) {
					RegionOcclusionData.sameAsVisible(result, true);
				}
			}
		}

		return result;
	}

	public int[] build() {
		final int closedCount = 4096 - openCount;

		if (closedCount < 256) {

			if(closedCount > 7) {
				// hide unexposed blocks
				hideClosedPositions();
			}

			// all chunk faces thru-visible
			//return ALL_OPEN;
		} else if (openCount == 0) {
			// only surface blocks are visible, and only if not covered
			adjustSurfaceVisbility();

			// all interior blocks are closed, no thru-visibility
			//return ALL_CLOSED;
		}

		return computeOcclusion();
	}

	private  void fill(int xyz4) {
		final int faceBits = 0;
		setVisited(xyz4);
		visit(xyz4, faceBits);

		while (!queue.isEmpty()) {
			final int nextXyz4 = queue.dequeueInt();
			visit(nextXyz4, faceBits);
		}
	}

	private void visit(int xyz4, int faceBits) {
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

	private void enqueIfUnvisited(int xyz4) {
		if (canVisit(xyz4)) {
			setVisited(xyz4);
			queue.enqueue(xyz4);
		}
	}

	static final int RENDERABLE_OFFSET = TOTAL_CACHE_WORDS;
	static final int EXTERIOR_VISIBLE_OFFSET = RENDERABLE_OFFSET + TOTAL_CACHE_WORDS;
	static final int WORD_COUNT = EXTERIOR_VISIBLE_OFFSET + TOTAL_CACHE_WORDS;

	static final long[] EMPTY_BITS = new long[WORD_COUNT];
	static final long[] EXTERIOR_MASK = new long[INTERIOR_CACHE_WORDS];

	//	public static final RegionOcclusionData ALL_OPEN;
	//	public static final RegionOcclusionData ALL_CLOSED;

	static {
		//		final int[] open = {0, 0, 0, 16, 16, 16, 0};
		//		ALL_OPEN = new RegionOcclusionData(open);
		//		RegionOcclusionData.isVisibleFullChunk(open, true);
		//		ALL_OPEN.fill(true);
		//
		//		final int[] closed = {0, 0, 0, 16, 16, 16, 0};
		//		RegionOcclusionData.isVisibleFullChunk(open, true);
		//		RegionOcclusionData.sameAsVisible(open, true);
		//		ALL_CLOSED = new RegionOcclusionData(closed);
		//		ALL_CLOSED.fill(false);

		for (int i = 0; i < 4096; i++) {
			final int x = i & 15;
			final int y = (i >> 4) & 15;
			final int z = (i >> 8) & 15;

			if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
				EXTERIOR_MASK[(i >> 6)] |= (1L << (i & 63));
			}
		}
	}
}
