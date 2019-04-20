/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.mixin;

import java.util.BitSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import grondag.canvas.chunk.occlusion.ChunkOcclusionBuilderAccessHelper.ChunkOcclusionGraphBuilderExt;
import grondag.canvas.chunk.occlusion.ChunkOcclusionGraphExt;
import grondag.canvas.chunk.occlusion.ChunkOcclusionMap;
import grondag.canvas.chunk.occlusion.DirectionSet;
import grondag.canvas.chunk.occlusion.OcclusionHelper;
import grondag.fermion.functions.PrimitiveFunctions.ObjToIntFunction;
import grondag.frex.api.model.ModelHelper;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.render.chunk.ChunkOcclusionGraph;
import net.minecraft.client.render.chunk.ChunkOcclusionGraphBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@Mixin(ChunkOcclusionGraphBuilder.class)
public abstract class MixinChunkOcclusionGraphBuilder implements ChunkOcclusionGraphBuilderExt {
    @Shadow private static int[] EDGE_POINTS;
    @Shadow private BitSet closed;
    @Shadow private int openCount = 4096;
    
    @Shadow
    private static int pack(BlockPos blockPos_1) {
        return 0;
    };

    @Shadow
    private void addEdgeFaces(int i, Set<Direction> set) {
    };

    @Shadow
    private int offset(int i, Direction face) {
        return 0;
    };
    
    @Inject(method = "build", at = @At("HEAD"), cancellable = true, require = 1)
    public void buildFast(CallbackInfoReturnable<ChunkOcclusionGraph> ci) {
        ChunkOcclusionGraph result = new ChunkOcclusionGraph();

        if (4096 - openCount < 256) {
            result.fill(true); // set all visible
            ((ChunkOcclusionGraphExt) result).canvas_visibilityData(DirectionSet.ALL);
        } else if (openCount == 0) {
            result.fill(false);
            ((ChunkOcclusionGraphExt) result).canvas_visibilityData(DirectionSet.NONE);
        } else {
            final BitSet bitSet = closed;
            ChunkOcclusionMap facingMap = ChunkOcclusionMap.claim();

            for (int i : EDGE_POINTS) {
                if (!bitSet.get(i)) {
                    final Pair<Set<Direction>, IntArrayList> floodResult = getOpenFacesFast(i);
                    final Set<Direction> fillSet = floodResult.getLeft();
                    result.addOpenEdgeFaces(fillSet); // set multiple visible
                    byte setIndex = (byte) DirectionSet.sharedIndex(fillSet);
                    final IntArrayList list = floodResult.getRight();
                    final int limit = list.size();
                    for (int j = 0; j < limit; j++)
                        facingMap.setIndex(list.getInt(j), setIndex);
                }
            }
            ((ChunkOcclusionGraphExt) result).canvas_visibilityData(facingMap);
        }
        ci.setReturnValue(result);
    }
    
    private Pair<Set<Direction>, IntArrayList> getOpenFacesFast(int pos) {
        final BitSet closed = this.closed;
        final OcclusionHelper help = OcclusionHelper.POOL.get().clear();
        Set<Direction> set = help.faces;
        final IntArrayList list = help.list;
        final IntArrayFIFOQueue queue = help.queue;

        queue.enqueue(pos);
        list.add(pos);

        closed.set(pos, true);

        while (!queue.isEmpty()) {
            int i = queue.dequeueInt();
            addEdgeFaces(i, set);

            for (int f = 0; f < 6; f++) {
                final Direction enumfacing = ModelHelper.faceFromIndex(f);

                int j = offset(i, enumfacing);

                if (j >= 0 && !closed.get(j)) {
                    closed.set(j, true);
                    queue.enqueue(j);
                    list.add(j);
                }
            }
        }

        return Pair.of(DirectionSet.sharedInstance(set), list);
    }
    
    @Override
    public ObjToIntFunction<BlockPos> canvas_pack() {
        return b -> pack(b);
    }
    
    @Override
    public void canvas_clear() {
        closed.clear();
        openCount = 4096;
    }
}
