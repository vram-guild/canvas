package grondag.canvas.chunk;

import static grondag.canvas.chunk.RenderRegionAddressHelper.INTERIOR_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.TOTAL_CACHE_SIZE;
import static grondag.canvas.chunk.RenderRegionAddressHelper.TOTAL_CACHE_WORDS;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localCornerIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localXfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localYfaceIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZEdgeIndex;
import static grondag.canvas.chunk.RenderRegionAddressHelper.localZfaceIndex;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;

public abstract class OcclusionRegion {
	final long[] bits = new long[WORLD_COUNT];
	int openCount;

	void prepare() {
		System.arraycopy(EMPTY_BITS, 0, bits, 0, WORLD_COUNT);
		openCount = TOTAL_CACHE_SIZE;
		captureFaces();
		captureEdges();
		captureCorners();
		captureInterior();
	}

	protected abstract BlockState blockStateAtIndex(int index);
	protected abstract boolean closedAtRelativePos(BlockState blockState, int x, int y, int z);

	public boolean isClosed(int index) {
		return (bits[(index >> 6)] & (1L << (index & 63))) != 0;
	}

	public boolean shouldRender(int index) {
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

	private void captureVisbility(int index, int x, int y, int z) {
		final BlockState blockState = blockStateAtIndex(index);

		if(blockState.getRenderType() != BlockRenderType.INVISIBLE || !blockState.getFluidState().isEmpty()) {
			setVisibility(index, true, closedAtRelativePos(blockState, x, y, z));
		}
	}

	private void captureInterior() {
		for(int i = 0; i <= INTERIOR_CACHE_SIZE; i++) {
			captureVisbility(i, i & 0xF, (i >> 4) & 0xF, (i >> 8) & 0xF);
		}
	}

	private void captureFaces() {
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				captureVisbility(localXfaceIndex(false, i, j), -1, i, j);
				captureVisbility(localXfaceIndex(true, i, j), 16, i, j);

				captureVisbility(localZfaceIndex(i, j, false), i, j, -1);
				captureVisbility(localZfaceIndex(i, j, true), i, j, 16);

				captureVisbility(localYfaceIndex(i, false, j), i, -1, j);
				captureVisbility(localYfaceIndex(i, true, j), i, 16, j);
			}
		}
	}

	private void captureEdges() {
		for(int i = 0; i < 16; i++) {
			captureVisbility(localZEdgeIndex(false, false, i), -1, -1, i);
			captureVisbility(localZEdgeIndex(false, true, i), -1, 16, i);
			captureVisbility(localZEdgeIndex(true, false, i), 16, -1, i);
			captureVisbility(localZEdgeIndex(true, true, i), 16, 16, i);

			captureVisbility(localYEdgeIndex(false, i, false), -1, i, -1);
			captureVisbility(localYEdgeIndex(false, i, true), -1, i, 16);
			captureVisbility(localYEdgeIndex(true, i, false), 16, i, -1);
			captureVisbility(localYEdgeIndex(true, i, true), 16, i, 16);

			captureVisbility(localXEdgeIndex(i, false, false), i, -1, -1);
			captureVisbility(localXEdgeIndex(i, false, true), i, -1, 16);
			captureVisbility(localXEdgeIndex(i, true, false), i, 16, -1);
			captureVisbility(localXEdgeIndex(i, true, true), i, 16, 16);
		}
	}

	private void captureCorners() {
		captureVisbility(localCornerIndex(false, false, false), -1, -1, -1);
		captureVisbility(localCornerIndex(false, false, true), -1, -1, 16);
		captureVisbility(localCornerIndex(false, true, false), -1, 16, -1);
		captureVisbility(localCornerIndex(false, true, true), -1, 16, 16);

		captureVisbility(localCornerIndex(true, false, false), 16, -1, -1);
		captureVisbility(localCornerIndex(true, false, true), 16, -1, 16);
		captureVisbility(localCornerIndex(true, true, false), 16, 16, -1);
		captureVisbility(localCornerIndex(true, true, true), 16, 16, 16);
	}

	static final int RENDERABLE_OFFSET = TOTAL_CACHE_WORDS;
	static final int EXTERIOR_VISIBLE_OFFSET = RENDERABLE_OFFSET + TOTAL_CACHE_WORDS;
	static final int INTERIOR_VISIBLE_OFFSET = EXTERIOR_VISIBLE_OFFSET + TOTAL_CACHE_WORDS;
	static final int WORLD_COUNT = INTERIOR_VISIBLE_OFFSET + TOTAL_CACHE_WORDS;

	static final long[] EMPTY_BITS = new long[WORLD_COUNT];
}
