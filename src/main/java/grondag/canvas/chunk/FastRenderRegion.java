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

package grondag.canvas.chunk;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.IntFunction;

import grondag.canvas.chunk.ChunkHack.PaletteCopy;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class FastRenderRegion {
  private static final ArrayBlockingQueue<FastRenderRegion> POOL = new ArrayBlockingQueue<>(256); 

  public static FastRenderRegion claim() {
      final FastRenderRegion result = POOL.poll();
      return result == null ? new FastRenderRegion() : result;
  }

  private static void release(FastRenderRegion region) {
      POOL.offer(region);
  }
  
  private World world;
  
  private int secBaseX;
  private int secBaseY;
  private int secBaseZ;
  
  @SuppressWarnings("unchecked")
  // larger than it needs to be to speed up indexing
  public final IntFunction<BlockState>[] sectionCopies = new IntFunction[64];
  
  public FastRenderRegion prepare(World world, WorldChunk[][] chunks, BlockPos posFrom) {
      this.world = world;
      secBaseX = posFrom.getX() >> 4;
      secBaseY = posFrom.getY() >> 4;
      secBaseZ = posFrom.getZ() >> 4;

      
      for(int x = 0; x < 3; x++) {
          for(int z = 0; z < 3; z++) {
              for(int y = 0; y < 3; y++) {
                  sectionCopies[x | (y << 2) | (z << 4)] = ChunkHack.captureCopy(chunks[x][z], y + secBaseY);
              }
          }
      }
      
      return this;
  }
  
  public BlockState getBlockState(int x, int y, int z) {
      return sectionCopies[secIndex(x, y, z)].apply(blockIndex(x, y, z));
  }

  private int blockIndex(int x, int y, int z) {
      return (x & 0xF) | ((y & 0xF) << 8) | ((z & 0xF) << 4);
  }

  private int secIndex(int x, int y, int z) {
      int bx = (x >> 4) - secBaseX;
      int by = (y >> 4) - secBaseY;
      int bz = (z >> 4) - secBaseZ;
      return bx | (by << 2) | (bz << 4);
  }
  
  public void release() {
      for(Object o : sectionCopies) {
          if(o instanceof PaletteCopy) {
              ((PaletteCopy)o).release();
          }
      }
      release(this);
  }
}
