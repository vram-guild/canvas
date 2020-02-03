package grondag.canvas.chunk.occlusion;

import java.util.EnumSet;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

// FIXME: always hides interior space, which will be a problem in enclosed spaces inside a single chunk

@Environment(EnvType.CLIENT)
public class FastChunkOcclusionDataBuilder {
	private final EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
	private final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();


	private final long[] bits = new long[192];
	private int openCount = 4096;

	public void prepare() {
		System.arraycopy(EMPTY, 0, bits, 0, 192);
		openCount = 4096;
		queue.clear();
	}

	public void setVisibility(int xPos, int yPos, int zPos, boolean isOpaque, boolean isRenderable) {
		setVisibility(packedXYZ4(xPos, yPos, zPos), isOpaque, isRenderable);
	}

	public void setVisibility(int xyz4, boolean isOpaque, boolean isRenderable) {
		final long mask = (1L << (xyz4 & 63));
		final int baseIndex = xyz4 >> 6;

		if (isOpaque) {
			--openCount;
			bits[baseIndex] |= mask;
		}

		if(isRenderable) {
			bits[baseIndex + RENDERABLE_OFFSET] |= mask;
		}
	}

	public boolean isClosed(int xPos, int yPos, int zPos) {
		final int xyz4 = packedXYZ4(xPos, yPos, zPos);
		return (bits[(xyz4 >> 6)] & (1L << (xyz4 & 63))) != 0;
	}

	public boolean shouldRender(int xPos, int yPos, int zPos) {
		return shouldRender(packedXYZ4(xPos, yPos, zPos));
	}

	public boolean shouldRender(int xyz4) {
		return (bits[(xyz4 >> 6) + RENDERABLE_OFFSET] & (1L << (xyz4 & 63))) != 0;
	}

	private void setVisited(int xyz4) {
		bits[(xyz4 >> 6) + VISIBLE_OFFSET] |= (1L << (xyz4 & 63));
	}

	// not opaque and not already visited
	private boolean canVisit(int xyz4) {
		final int baseIndex = xyz4 >> 6;
		final long mask = 1L << (xyz4 & 63);

		if((bits[baseIndex + VISIBLE_OFFSET] & mask) == 0) {
			if ((bits[baseIndex] & mask) == 0) {
				return true;
			} else {
				// if closed then mark and report as visited - only want to do this for checked positions
				bits[baseIndex + VISIBLE_OFFSET] |= mask;
				return false;
			}
		} else {
			return false;
		}
	}

	public ChunkOcclusionData build() {
		// if corner is open, adjacent faces are visible to each other

		//PERF: any way to avoid allocation?


		if (4096 - openCount < 256) {
			// set all visible, renderable is unmasked
			return ALL_OPEN;
		} else if (openCount == 0) {
			// no thru-visibility, only exterior blocks are renderable
			for(int i = 0; i < 64; i++) {
				bits[i + RENDERABLE_OFFSET] &= EXTERIOR_MASK[i];
			}

			return ALL_CLOSED;

		} else {
			final ChunkOcclusionData result = new ChunkOcclusionData();

			// determine which blocks are visible
			for (final int xyz4 : EXTERIOR_INDEX) {
				if (canVisit(xyz4)) {
					result.addOpenEdgeFaces(getVistedFaces(xyz4));
				}
			}

			// TODO: disable in spectator mode
			// mask renderable to visible
			for(int i = 0; i < 64; i++) {
				bits[i + RENDERABLE_OFFSET] &= bits[i + VISIBLE_OFFSET];
			}

			return result;
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

	///////// STATIC MEMBERS FOLLOW /////////

	private static final long[] EMPTY = new long[192];
	private static final int VISIBLE_OFFSET = 64;
	private static final int RENDERABLE_OFFSET = VISIBLE_OFFSET + 64;
	private static final long[] EXTERIOR_MASK = new long[64];

	public static final ChunkOcclusionData ALL_OPEN;
	public static final ChunkOcclusionData ALL_CLOSED;
	/**
	 * Indexes to face voxels
	 */
	static final int[] EXTERIOR_INDEX = new int[1352];

	static {
		ALL_OPEN = new ChunkOcclusionData();
		ALL_OPEN.fill(true);

		ALL_CLOSED = new ChunkOcclusionData();
		ALL_CLOSED.fill(false);

		int exteriorIndex = 0;

		for (int i = 0; i < 4096; i++) {
			final int x = i & 15;
			final int y = (i >> 4) & 15;
			final int z = (i >> 8) & 15;

			if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
				EXTERIOR_INDEX[exteriorIndex++] = i;
			}
		}

		assert exteriorIndex == 1352;

		for(final int xyz4 : EXTERIOR_INDEX) {
			EXTERIOR_MASK[(xyz4 >> 6)] |= (1L << (xyz4 & 63));
		}
	}


	/**
	 * Packed 4-bit Cartesian coordinates
	 */
	static int packedXYZ4(int x, int y, int z) {
		return x | (y << 4) | (z << 8);
	}
}
