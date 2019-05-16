package grondag.canvas.chunk;

import java.util.Arrays;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.PackedIntegerArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkHack {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    
    public static class PaletteCopy {
        private PackedIntegerArray data;
        private final Palette<BlockState> palette;
        public final BlockState emptyVal;
        public final boolean isEmpty;
        
        public PaletteCopy(Palette<BlockState> palette, PackedIntegerArray data, BlockState emptyVal) {
            this.palette = palette;
            this.data = data == null ? null : ((PackedIntegerArrayExt)data).canvas_copy();
            this.isEmpty = palette == null || this.data == null;
            this.emptyVal = emptyVal;
        }

        public BlockState blockState(int index) {
            final BlockState result = palette.getByIndex(data.get(index));
            return result == null ? emptyVal : result;
        }
    }

    public static void captureSection(BlockState[] blockStates, PaletteCopy paletteCopy) {
        if(paletteCopy.isEmpty) {
            Arrays.fill(blockStates, paletteCopy.emptyVal);
        } else {
            for(int i = 0; i < 4096; i++) {
                blockStates[i] = paletteCopy.blockState(i);
            }
        }
    }
    
    private static final PaletteCopy AIR_COPY = new PaletteCopy(null, null, AIR);

    public static PaletteCopy captureCopy(WorldChunk chunk, int sectionIndex) {
        if(chunk == null || sectionIndex < 0) {
            return AIR_COPY;
        }
        
        final ChunkSection[] sections = chunk.getSectionArray();
        
        if(sections == null || sectionIndex >= sections.length) {
            return AIR_COPY;
        }
        
        ChunkSection sec = sections[sectionIndex];
        if(sec == null) {
            return AIR_COPY;
        }
        
        if(sec.isEmpty()) {
            BlockState filler = sec.getBlockState(0, 0, 0);
            return new PaletteCopy(null, null, filler);
        } 
        
        return ((PalettedContainerExt)sec.getContainer()).canvas_paletteCopy();
    }
}
