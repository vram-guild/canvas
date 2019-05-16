package grondag.canvas.chunk;

import java.util.Arrays;

import grondag.canvas.mixin.MixinPalettedContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkHack {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState[] EMPTY_SECTION = new BlockState[4096];
    
    static {
        Arrays.fill(EMPTY_SECTION, AIR);
    }
    
    //TODO: remove
    public static void doHack(ChunkRendererRegion region, int cxOff, int czOff, WorldChunk[][] chunks, BlockPos posFrom, BlockPos posTo, 
            BlockState[] blockStates, FluidState[] fluidStates) {
        
        final ChunkSection[] sections = chunks[1][1].getSectionArray();
        
        if(sections == null) {
            return;
        }
        
        //TODO: handle out of range chunks
        int secIndex = (posFrom.getY() + 2) >> 4;

        BlockState[] testStates = new BlockState[4096];
        
        if(secIndex < 0 || secIndex > sections.length) {
            Arrays.fill(testStates, AIR);
        } else {
            ChunkSection sec = sections[secIndex];
            if(sec == null) {
                Arrays.fill(testStates, AIR);
            } else if(sec.isEmpty()) {
                BlockState filler = sec.getBlockState(0, 0, 0);
                Arrays.fill(testStates, filler);
            } else {
                MixinPalettedContainer container = (MixinPalettedContainer) sec.getContainer();
                for(int i = 0; i < 4096; i++) {
                    testStates[i] = container.invokeGet(i);
                }
            }
        }
        
        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        int baseX = posFrom.getX() + 2;
        int baseY = posFrom.getY() + 2;
        int baseZ = posFrom.getZ() + 2;
        
        for(int x = 0; x < 16; x++) {
            for(int y = 0; y < 16; y++) {
                for(int z = 0; z < 16; z++) {
                    BlockState original = region.getBlockState(pos.set(baseX + x, baseY + y, baseZ + z));
                    BlockState copy = testStates[x + (y << 8) + (z << 4)];
                    if(copy != original) {
                        System.out.print(false);
                    }
                }
            }
        }
    }
    
    public static void captureSection(BlockState[] section, WorldChunk chunk, int sectionIndex) {
        if(chunk == null || sectionIndex < 0) {
            System.arraycopy(EMPTY_SECTION, 0, section, 0, 4096);
            return;
        }
        
        final ChunkSection[] sections = chunk.getSectionArray();
        
        if(sections == null || sectionIndex >= sections.length) {
            System.arraycopy(EMPTY_SECTION, 0, section, 0, 4096);
            return;
        }
        
        ChunkSection sec = sections[sectionIndex];
        if(sec == null) {
            System.arraycopy(EMPTY_SECTION, 0, section, 0, 4096);
            return;
        }
        
        if(sec.isEmpty()) {
            BlockState filler = sec.getBlockState(0, 0, 0);
            Arrays.fill(section, filler);
            return;
        } 
        
        MixinPalettedContainer container = (MixinPalettedContainer) sec.getContainer();
        for(int i = 0; i < 4096; i++) {
            section[i] = container.invokeGet(i);
        }
    }
}
