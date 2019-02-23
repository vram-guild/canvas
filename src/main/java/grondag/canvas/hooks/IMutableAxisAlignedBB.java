package grondag.canvas.hooks;

import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.BlockPos;

public interface IMutableAxisAlignedBB
{
    IMutableAxisAlignedBB set(BoundingBox box);

    IMutableAxisAlignedBB growMutable(double value);

    IMutableAxisAlignedBB growMutable(double x, double y, double z);
    
    BoundingBox toImmutable();

    IMutableAxisAlignedBB offsetMutable(BlockPos pos);

    BoundingBox cast();

    IMutableAxisAlignedBB expandMutable(double x, double y, double z);

}
