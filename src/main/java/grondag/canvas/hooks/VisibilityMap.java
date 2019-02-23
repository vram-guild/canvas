package grondag.acuity.hooks;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap;
import net.minecraft.util.EnumFacing;

@SuppressWarnings("serial")
public class VisibilityMap extends Short2ByteOpenHashMap
{
    private static ArrayBlockingQueue<VisibilityMap> visibilityMaps = new ArrayBlockingQueue<>(4096);
    
    public static VisibilityMap claim()
    {
        VisibilityMap result = visibilityMaps.poll();
        if(result == null)
            result = new VisibilityMap();
        else
            result.clear();
        return result;
    }
    
    public static void release(VisibilityMap map)
    {
        visibilityMaps.offer(map);
    }

    public Set<EnumFacing> getFaceSet(int index)
    {
        return EnumFacingSet.sharedInstance(this.get((short)index));
    }
    
    public void setIndex(int positionIndex, int setIndex)
    {
        this.put((short)positionIndex, (byte)setIndex);
    }
}