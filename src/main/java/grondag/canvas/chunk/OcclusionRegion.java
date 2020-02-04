package grondag.canvas.chunk;

import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_WORDS;
import static grondag.canvas.chunk.RenderRegionAddressHelper.TOTAL_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.TOTAL_CACHE_WORDS;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localCornerIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.mainChunkLocalIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.relativeBlockIndex;

import java.util.EnumSet;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

public abstract class OcclusionRegion {
	private final EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
	private final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
	private final long[] bits = new long[WORLD_COUNT];
	private int openCount;

	void prepare() {
		System.arraycopy(EMPTY_BITS, 0, bits, 0, WORLD_COUNT);
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

	public boolean shouldRender(int x, int y, int z) {
		final int index = mainChunkLocalIndex(x, y, z);
		return (bits[(index >> 6) + RENDERABLE_OFFSET] & (1L << (index & 63))) != 0;
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
		final int index = mainChunkLocalIndex(x, y, z);
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

				if(isClosed(relativeBlockIndex(x - 1, y, z)) && isClosed(relativeBlockIndex(x + 1, y, z))
						&& isClosed(relativeBlockIndex(x, y - 1, z)) && isClosed(relativeBlockIndex(x, y + 1, z))
						&& isClosed(relativeBlockIndex(x, y, z - 1)) && isClosed(relativeBlockIndex(x, y, z + 1))) {

					bits[wordIndex] &=  ~mask;
				}
			}
		}
	}

	private void addOpenEdgeFacesIfCanVisit(ChunkOcclusionData result, int x, int y, int z) {
		final int index = mainChunkLocalIndex(x, y, z);

		if (canVisit(index)) {
			result.addOpenEdgeFaces(getVistedFaces(index));
		}
	}

	private ChunkOcclusionData computeOcclusion() {
		final ChunkOcclusionData result = new ChunkOcclusionData();

		// determine which blocks are visible
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				if (!isClosed(localXfaceIndex(false, i, j))) addOpenEdgeFacesIfCanVisit(result, 0, i, j);
				if (!isClosed(localXfaceIndex(true, i, j))) addOpenEdgeFacesIfCanVisit(result, 15, i, j);

				if (!isClosed(localZfaceIndex(i, j, false))) addOpenEdgeFacesIfCanVisit(result, i, j, 0);
				if (!isClosed(localZfaceIndex(i, j, true))) addOpenEdgeFacesIfCanVisit(result, i, j, 15);

				if (!isClosed(localYfaceIndex(i, false, j))) addOpenEdgeFacesIfCanVisit(result, i, 0, j);
				if (!isClosed(localYfaceIndex(i, true, j))) addOpenEdgeFacesIfCanVisit(result, i, 15, j);
			}
		}

		// TODO: disable in spectator mode
		// mask renderable to visible
		for(int i = 0; i < INTERIOR_CACHE_WORDS; i++) {
			bits[i + RENDERABLE_OFFSET] &= bits[i + EXTERIOR_VISIBLE_OFFSET];
		}

		return result;
	}

	public ChunkOcclusionData build() {
		final int closedCount = 4096 - openCount;

		if (closedCount < 256) {

			if(closedCount > 7) {
				// hide unexposed blocks
				hideClosedPositions();
			}

			// all chunk faces thru-visible
			return ALL_OPEN;
		} else if (openCount == 0) {
			// only surface blocks are visible, and only if not covered
			adjustSurfaceVisbility();

			// all interior blocks are closed, no thru-visibility
			return ALL_CLOSED;
		} else {
			return computeOcclusion();
		}
	}

	private  Set<Direction> getVistedFaces(int xyz4) {
		faces.clear();
		setVisited(xyz4);
		visit(xyz4);

		while (!queue.isEmpty()) {
			final int nextXyz4 = queue.dequeueInt();
			visit(nextXyz4);
		}

		return faces;
	}

	private void visit(int xyz4) {
		final int x = xyz4 & 0xF;

		if (x == 0) {
			faces.add(Direction.WEST);
			enqueIfUnvisited(xyz4 + 1);
		} else if (x == 15) {
			faces.add(Direction.EAST);
			enqueIfUnvisited(xyz4 - 1);
		} else {
			enqueIfUnvisited(xyz4 - 1);
			enqueIfUnvisited(xyz4 + 1);
		}

		final int y = xyz4 & 0xF0;

		if (y == 0) {
			faces.add(Direction.DOWN);
			enqueIfUnvisited(xyz4 + 0x10);
		} else if (y == 0xF0) {
			faces.add(Direction.UP);
			enqueIfUnvisited(xyz4 - 0x10);
		} else {
			enqueIfUnvisited(xyz4 - 0x10);
			enqueIfUnvisited(xyz4 + 0x10);
		}

		final int z = xyz4 & 0xF00;

		if (z == 0) {
			faces.add(Direction.NORTH);
			enqueIfUnvisited(xyz4 + 0x100);
		} else if (z == 0xF00) {
			faces.add(Direction.SOUTH);
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
	static final int INTERIOR_VISIBLE_OFFSET = EXTERIOR_VISIBLE_OFFSET + TOTAL_CACHE_WORDS;
	static final int WORLD_COUNT = INTERIOR_VISIBLE_OFFSET + TOTAL_CACHE_WORDS;

	static final long[] EMPTY_BITS = new long[WORLD_COUNT];
	static final long[] EXTERIOR_MASK = new long[INTERIOR_CACHE_WORDS];

	public static final ChunkOcclusionData ALL_OPEN;
	public static final ChunkOcclusionData ALL_CLOSED;

	static {
		ALL_OPEN = new ChunkOcclusionData();
		ALL_OPEN.fill(true);

		ALL_CLOSED = new ChunkOcclusionData();
		ALL_CLOSED.fill(false);

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
