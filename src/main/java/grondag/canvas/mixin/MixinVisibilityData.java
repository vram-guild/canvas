package grondag.canvas.mixin;

import java.util.BitSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.mixinext.VisibilityDataExt;
import grondag.fermion.functions.PrimitiveFunctions.ObjToIntFunction;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Mixin(ChunkOcclusionGraphBuilder.class)
public class MixinVisibilityData implements VisibilityDataExt {
    @Shadow
    private static int pack(BlockPos blockPos_1) {
        return 0;
    };

    @Shadow
    private BitSet closed;
    @Shadow
    private static int[] EDGE_POINTS;
    @Shadow
    private int openCount = 4096;

    @Shadow
    private void addEdgeFaces(int i, Set<Direction> set) {
    };

    @Shadow
    private int offset(int i, Direction face) {
        return 0;
    };

    @Override
    public ObjToIntFunction<BlockPos> canvas_pack() {
        return b -> pack(b);
    }

    @Override
    public int[] canvas_edgePoints() {
        return EDGE_POINTS;
    }

    @Override
    public BitSet canvas_closed() {
        return closed;
    }

    @Override
    public int canvas_openCount() {
        return openCount;
    }

    @Override
    public void canvas_openCount(int count) {
        openCount = count;
    }

    @Override
    public void canvas_addEdgeFaces(int i, Set<Direction> set) {
        addEdgeFaces(i, set);
    }

    @Override
    public int canvas_offset(int i, Direction face) {
        return offset(i, face);
    }
}
