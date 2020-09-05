package grondag.canvas.terrain;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.lang3.ObjectUtils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.WorldChunk;

import grondag.canvas.mixinterface.PalettedContainerExt;

public class ChunkPaletteCopier {

	@FunctionalInterface
	public interface PaletteCopy {
		BlockState apply(int index);

		default void release() {  }
	}

	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private static class PaletteCopyImpl implements PaletteCopy {
		private final IntArrayList data;
		private final Palette<BlockState> palette;
		public final BlockState emptyVal;

		private PaletteCopyImpl(Palette<BlockState> palette, PackedIntegerArray data, BlockState emptyVal) {
			assert data != null;
			assert palette != null;
			this.palette = palette;
			this.data = PackedIntegerStorageHelper.claim(data);
			this.emptyVal = emptyVal;
		}

		@Override
		public BlockState apply(int index) {
			return ObjectUtils.defaultIfNull(palette.getByIndex(data.getInt(index)), emptyVal);
		}

		@Override
		public void release() {
			PackedIntegerStorageHelper.release(data);
		}
	}

	public static final PaletteCopy AIR_COPY = i -> AIR;

	public static PaletteCopy captureCopy(WorldChunk chunk, int sectionIndex) {
		if(chunk == null || sectionIndex < 0) {
			return AIR_COPY;
		}

		final ChunkSection[] sections = chunk.getSectionArray();

		if(sections == null || sectionIndex >= sections.length) {
			return AIR_COPY;
		}

		final ChunkSection sec = sections[sectionIndex];
		if(sec == null) {
			return AIR_COPY;
		}

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
		if(palette == null || data == null) {
			return emptyVal == null ? AIR_COPY : i -> emptyVal;
		}

		return new PaletteCopyImpl(palette, data, emptyVal);
	}
}
