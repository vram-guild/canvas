package grondag.canvas.hooks;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import grondag.canvas.mixinext.ChunkVisibility;
import grondag.canvas.mixinext.VisibilityDataExt;
import grondag.fermion.functions.PrimitiveFunctions.ObjToIntFunction;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import grondag.frex.api.core.ModelHelper;
import net.minecraft.client.render.chunk.ChunkOcclusionGraph;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class VisibilityHooks {
    public static final ObjToIntFunction<BlockPos> INDEX_FUNCTION;
    public static final int[] EXTERIOR_INDICES;
    static
    {
        VisibilityDataExt visData = (VisibilityDataExt) new ChunkOcclusionGraphBuilder();
        INDEX_FUNCTION = visData.canvas_pack();
        EXTERIOR_INDICES = visData.canvas_edgePoints();
    }
    
    @SuppressWarnings("unchecked")
    public static Set<Direction> getVisibleFacingsExt(Object visData, BlockPos eyePos) {
        if (visData instanceof Set)
            return (Set<Direction>) visData;
        else {
            return ((VisibilityMap) visData).getFaceSet(INDEX_FUNCTION.apply(eyePos));
        }
    }

    public static ChunkOcclusionGraph computeVisiblityExt(ChunkOcclusionGraphBuilder visDataIn) {
        VisibilityDataExt visData = (VisibilityDataExt) visDataIn;
        ChunkOcclusionGraph setvisibility = new ChunkOcclusionGraph();

        if (4096 - visData.canvas_openCount() < 256) {
            setvisibility.fill(true); // set all visible
            ((ChunkVisibility) setvisibility).setVisibilityData(DirectionSet.ALL);
        } else if (visData.canvas_openCount() == 0) {
            setvisibility.fill(false);
            ((ChunkVisibility) setvisibility).setVisibilityData(DirectionSet.NONE);
        } else {
            final BitSet bitSet = visData.canvas_closed();
            VisibilityMap facingMap = VisibilityMap.claim();

            for (int i : EXTERIOR_INDICES) {
                if (!bitSet.get(i)) {
                    final Pair<Set<Direction>, IntArrayList> floodResult = floodFill(visData, i);
                    final Set<Direction> fillSet = floodResult.getLeft();
                    setvisibility.addOpenEdgeFaces(fillSet); // set multiple visible
                    byte setIndex = (byte) DirectionSet.sharedIndex(fillSet);
                    final IntArrayList list = floodResult.getRight();
                    final int limit = list.size();
                    for (int j = 0; j < limit; j++)
                        facingMap.setIndex(list.getInt(j), setIndex);
                }
            }
            ((ChunkVisibility) setvisibility).setVisibilityData(facingMap);
        }

        return setvisibility;
    }

    private static class Helpers {
        final EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        final IntArrayList list = new IntArrayList();
        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    }

    private static final ThreadLocal<Helpers> helpers = new ThreadLocal<Helpers>() {
        @Override
        protected Helpers initialValue() {
            return new Helpers();
        }
    };

    private static Pair<Set<Direction>, IntArrayList> floodFill(VisibilityDataExt visData, int pos) {
        final BitSet bitSet = visData.canvas_closed();
        final Helpers help = helpers.get();
        Set<Direction> set = help.faces;
        set.clear();

        final IntArrayList list = help.list;
        list.clear();

        final IntArrayFIFOQueue queue = help.queue;
        queue.clear();

        queue.enqueue(pos);
        list.add(pos);

        bitSet.set(pos, true);

        while (!queue.isEmpty()) {
            int i = queue.dequeueInt();
            visData.canvas_addEdgeFaces(i, set);

            for (int f = 0; f < 6; f++) {
                final Direction enumfacing = ModelHelper.faceFromIndex(f);

                int j = visData.canvas_offset(i, enumfacing);

                if (j >= 0 && !bitSet.get(j)) {
                    bitSet.set(j, true);
                    queue.enqueue(j);
                    list.add(j);
                }
            }
        }

        return Pair.of(DirectionSet.sharedInstance(set), list);
    }
}
