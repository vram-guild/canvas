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

package grondag.canvas.terrain.region.input;

import static grondag.canvas.terrain.util.RenderRegionStateIndexer.regionIndex;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Tries to prevent InputRegion from being unreadably big. Fails.
 */
public abstract class AbstractInputRegion {
	// larger than needed to speed up indexing
	protected final LevelChunk[] chunks = new LevelChunk[16];
	protected int originX;
	protected int originY;
	protected int originZ;
	protected int chunkBaseX;
	/** Section index of region below this one, -1 if this is the bottom-most region in a chunk. */
	protected int baseSectionIndex;
	protected int chunkBaseZ;
	protected Level world;

	final boolean isInMainChunk(int x, int y, int z) {
		return originX == (x & 0xFFFFFFF0) && originY == (y & 0xFFFFFFF0) && originZ == (z & 0xFFFFFFF0);
	}

	final boolean isInMainChunk(BlockPos pos) {
		return isInMainChunk(pos.getX(), pos.getY(), pos.getZ());
	}

	final int blockIndex(int x, int y, int z) {
		return regionIndex(x - originX, y - originY, z - originZ);
	}

	protected LevelChunkSection getSection(int x, int y, int z) {
		final int index = y + baseSectionIndex;

		if (index < 0) {
			return null;
		}

		final LevelChunkSection[] sections = chunks[x | (z << 2)].getSections();
		return index >= sections.length ? null : sections[index];
	}

	protected LevelChunk getChunk(int cx, int cz) {
		final int chunkBaseX = this.chunkBaseX;
		final int chunkBaseZ = this.chunkBaseZ;

		if (cx < chunkBaseX || cx > (chunkBaseZ + 2) || cz < chunkBaseZ || cz > (chunkBaseZ + 2)) {
			return world.getChunk(cx, cz);
		} else {
			return chunks[(cx - chunkBaseX) | ((cz - chunkBaseZ) << 2)];
		}
	}
}
