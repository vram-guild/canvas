package grondag.acuity.hooks;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class VisibilityHooks
{
    @SuppressWarnings("unchecked")
    public static Set<EnumFacing> getVisibleFacingsExt(Object visData, BlockPos eyePos)
    {
        if(visData instanceof Set)
            return (Set<EnumFacing>)visData;
        else
        {
            return ((VisibilityMap)visData).getFaceSet(VisGraph.getIndex(eyePos));
        }
    }
    
    public static SetVisibility computeVisiblityExt(VisGraph visgraph)
    {
        SetVisibility setvisibility = new SetVisibility();

        if (4096 - visgraph.empty < 256)
        {
            setvisibility.setAllVisible(true);
            ((ISetVisibility)setvisibility).setVisibilityData(EnumFacingSet.ALL);
        }
        else if (visgraph.empty == 0)
        {
            setvisibility.setAllVisible(false);
            ((ISetVisibility)setvisibility).setVisibilityData(EnumFacingSet.NONE);
        }
        else
        {
            final BitSet bitSet = visgraph.bitSet;
            VisibilityMap facingMap = VisibilityMap.claim();
            
            for (int i : VisGraph.INDEX_OF_EDGES)
            {
                if (!bitSet.get(i))
                {
                    final Pair<Set<EnumFacing>, IntArrayList> floodResult = floodFill(visgraph, i);
                    final Set<EnumFacing> fillSet = floodResult.getLeft();
                    setvisibility.setManyVisible(fillSet);
                    byte setIndex = (byte) EnumFacingSet.sharedIndex(fillSet);
                    final IntArrayList list = floodResult.getRight();
                    final int limit = list.size();
                    for(int j = 0; j < limit; j++)
                        facingMap.setIndex(list.getInt(j), setIndex);
                }
            }
            ((ISetVisibility)setvisibility).setVisibilityData(facingMap);
        }

        return setvisibility;
    }

    private static class Helpers
    {
        final EnumSet<EnumFacing> faces = EnumSet.noneOf(EnumFacing.class);
        final IntArrayList list = new IntArrayList();
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    }
    
    private static final ThreadLocal<Helpers> helpers = new ThreadLocal<Helpers>()
    {
        @Override
        protected Helpers initialValue()
        {
            return new Helpers();
        }
    };
    
    private static Pair<Set<EnumFacing>, IntArrayList> floodFill(VisGraph visgraph, int pos)
    {
        final Helpers help = helpers.get();
        Set<EnumFacing> set = help.faces;
        set.clear();
        
        final IntArrayList list = help.list;
        list.clear();
        
        final IntArrayFIFOQueue queue = help.queue;
        queue.clear();
        
        queue.enqueue(pos);
        list.add(pos);
        
        visgraph.bitSet.set(pos, true);

        while (!queue.isEmpty())
        {
            int i = queue.dequeueInt();
            visgraph.addEdges(i, set);

            for(int f = 0; f < 6; f++)
            {
                final EnumFacing enumfacing = EnumFacing.VALUES[f];
                
                int j = visgraph.getNeighborIndexAtFace(i, enumfacing);

                if (j >= 0 && !visgraph.bitSet.get(j))
                {
                    visgraph.bitSet.set(j, true);
                    queue.enqueue(j);
                    list.add(j);
                }
            }
        }

        return Pair.of(EnumFacingSet.sharedInstance(set), list);
    }
}
