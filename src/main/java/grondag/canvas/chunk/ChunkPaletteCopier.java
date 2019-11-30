/*******************************************************************************
 * Copyright 2019 grondag
 *
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

package grondag.canvas.chunk;

import org.apache.commons.lang3.ObjectUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.PackedIntegerArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkPaletteCopier {

	@FunctionalInterface
	public interface PaletteCopy {
		BlockState apply(int index);

		default void release() {  }
	}

	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private static class PaletteCopyImpl implements PaletteCopy {
		private final PackedIntegerArray data;
		private final Palette<BlockState> palette;
		public final BlockState emptyVal;

		private PaletteCopyImpl(Palette<BlockState> palette, PackedIntegerArray data, BlockState emptyVal) {
			assert data != null;
			assert palette != null;
			this.palette = palette;
			this.data = ((PackedIntegerArrayExt)data).canvas_copy();
			this.emptyVal = emptyVal;
		}

		@Override
		public BlockState apply(int index) {
			return ObjectUtils.defaultIfNull(palette.getByIndex(data.get(index)), emptyVal);
		}

		@Override
		public void release() {
			PackedIntegerStorageHelper.releaseStorageCopy(data.getStorage());
		}
	}

	private static final PaletteCopy AIR_COPY = i -> AIR;

	public static PaletteCopy captureCopy(WorldChunk chunk, int sectionIndex) {
		if(chunk == null || sectionIndex < 0)
			return AIR_COPY;

		final ChunkSection[] sections = chunk.getSectionArray();

		if(sections == null || sectionIndex >= sections.length)
			return AIR_COPY;

		final ChunkSection sec = sections[sectionIndex];
		if(sec == null)
			return AIR_COPY;

		if(sec.isEmpty()) {
			final BlockState filler = sec.getBlockState(0, 0, 0);
			return filler == AIR ? AIR_COPY : i -> filler;
		}

		return ((PalettedContainerExt)sec.getContainer()).canvas_paletteCopy();
	}

	/**
	 * Callback from canvas_paletteCopy()
	 */
	public static PaletteCopy captureCopy(Palette<BlockState> palette, PackedIntegerArray data, BlockState emptyVal) {
		if(palette == null || data == null)
			return emptyVal == null ? AIR_COPY : i -> emptyVal;
			return new PaletteCopyImpl(palette, data, emptyVal);
	}
}
