package grondag.acuity.hooks;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public interface IMutableAxisAlignedBB
{
    IMutableAxisAlignedBB set(AxisAlignedBB box);

    IMutableAxisAlignedBB growMutable(double value);

    IMutableAxisAlignedBB growMutable(double x, double y, double z);
    
    AxisAlignedBB toImmutable();

    IMutableAxisAlignedBB offsetMutable(BlockPos pos);

    AxisAlignedBB cast();

    IMutableAxisAlignedBB expandMutable(double x, double y, double z);

}
