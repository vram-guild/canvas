package grondag.acuity.hooks;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import net.minecraft.util.EnumFacing;
import static net.minecraft.util.EnumFacing.*;

public class EnumFacingSet
{
    @SuppressWarnings("unchecked")
    private static Set<EnumFacing>[] ALL_SETS = new Set[64];
    
    public static final Set<EnumFacing> ALL;
    public static final Set<EnumFacing> NONE;
  
    static
    {
        for(int i = 0; i < 64; i++)
        {
            EnumSet<EnumFacing> set = EnumSet.noneOf(EnumFacing.class);
            if((i & (1 << DOWN.ordinal())) != 0)  set.add(DOWN);
            if((i & (1 << UP.ordinal())) != 0)  set.add(UP);
            if((i & (1 << EAST.ordinal())) != 0)  set.add(EAST);
            if((i & (1 << WEST.ordinal())) != 0)  set.add(WEST);
            if((i & (1 << NORTH.ordinal())) != 0)  set.add(NORTH);
            if((i & (1 << SOUTH.ordinal())) != 0)  set.add(SOUTH);
            
            ALL_SETS[i] = Collections.unmodifiableSet(set);
        }
        ALL = ALL_SETS[63];
        NONE = ALL_SETS[0];
    }
    
    public static int addFaceToBit(int bits, EnumFacing face)
    {
        return bits | (1 << face.ordinal());
    }
    
    public static int sharedIndex(Set<EnumFacing> fromSet)
    {
        if(fromSet.isEmpty())
            return 0;
        else if(fromSet.size() == 6)
            return 63;
        else
        {
            int bits = 0;
            if(fromSet.contains(EnumFacing.DOWN)) bits |= (1 << DOWN.ordinal());
            if(fromSet.contains(EnumFacing.UP)) bits |= (1 << UP.ordinal());
            if(fromSet.contains(EnumFacing.EAST)) bits |= (1 << EAST.ordinal());
            if(fromSet.contains(EnumFacing.WEST)) bits |= (1 << WEST.ordinal());
            if(fromSet.contains(EnumFacing.NORTH)) bits |= (1 << NORTH.ordinal());
            if(fromSet.contains(EnumFacing.SOUTH)) bits |= (1 << SOUTH.ordinal());
            
            return bits;
        }    
    }
    
    public static Set<EnumFacing> sharedInstance(Set<EnumFacing> fromSet)
    {
        return sharedInstance(sharedIndex(fromSet));
    }
    
    public static Set<EnumFacing> sharedInstance(int fromIndex)
    {
        return ALL_SETS[fromIndex];
    }
    
}
