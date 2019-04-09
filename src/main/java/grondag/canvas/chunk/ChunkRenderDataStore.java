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

import net.minecraft.client.render.chunk.ChunkRenderData;

public class ChunkRenderDataStore {
    private static final ArrayBlockingQueue<ChunkRenderData> chunks = new ArrayBlockingQueue<>(4096);

    public static ChunkRenderData claim() {
        ChunkRenderData result = chunks.poll();
        if (result == null)
            result = new ChunkRenderData();

        return result;
    }

    public static void release(ChunkRenderData chunk) {
        ((ChunkRenderDataExt) chunk).canvas_clear();
        chunks.offer(chunk);
    }
}
