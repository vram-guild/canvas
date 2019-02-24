package grondag.canvas.mixin;

import java.util.BitSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import grondag.canvas.mixinext.VisibilityDataExt;
import grondag.fermion.functions.PrimitiveFunctions.ObjToIntFunction;
import net.minecraft.class_852;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Mixin(class_852.class)
public class MixinVisibilityData implements VisibilityDataExt {
    @Shadow
    private static int method_3683(BlockPos blockPos_1) {
        return 0;
    };

    @Shadow
    private BitSet field_4478;
    @Shadow
    private static int[] field_4474;
    @Shadow
    private int field_4473 = 4096;

    @Shadow
    private void method_3684(int i, Set<Direction> set) {
    };

    @Shadow
    private int method_3685(int i, Direction face) {
        return 0;
    };

    @Override
    public ObjToIntFunction<BlockPos> indexFunction() {
        return b -> method_3683(b);
    }

    @Override
    public int[] exteriorIndices() {
        return field_4474;
    }

    @Override
    public BitSet bitSet() {
        return field_4478;
    }

    @Override
    public int getEmptyCount() {
        return field_4473;
    }

    @Override
    public void setEmptyCount(int count) {
        field_4473 = count;
    }

    @Override
    public void addExteriorToSet(int i, Set<Direction> set) {
        method_3684(i, set);
    }

    @Override
    public int getNeighborIndex(int i, Direction face) {
        return method_3685(i, face);
    }
}
