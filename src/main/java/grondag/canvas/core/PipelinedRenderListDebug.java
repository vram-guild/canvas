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

package grondag.canvas.core;

import grondag.canvas.Canvas;
import grondag.canvas.buffering.DrawableChunk;
import grondag.canvas.mixinext.ChunkRendererExt;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderer;

public class PipelinedRenderListDebug extends AbstractPipelinedRenderList {
    public PipelinedRenderListDebug() {
        super();
    }

    protected long startNanos;
    protected long totalNanos;
    protected int frameCounter;
    protected int chunkCounter;
    protected int drawCounter;
    protected int quadCounter;
    protected long minNanos = Long.MAX_VALUE;
    protected long maxNanos = 0;

    @Override
    public final void addChunkRenderer(ChunkRenderer renderChunkIn, BlockRenderLayer layer) {
        chunkCounter++;
        DrawableChunk vertexbuffer = layer == BlockRenderLayer.SOLID ? ((ChunkRendererExt) renderChunkIn).getSolidDrawable()
                : ((ChunkRendererExt) renderChunkIn).getTranslucentDrawable();
        if (vertexbuffer == null)
            return;
        drawCounter += vertexbuffer.drawCount();
        quadCounter += vertexbuffer.quadCount();
        super.addChunkRenderer(renderChunkIn, layer);
    }

    @Override
    public final void renderChunkLayer(BlockRenderLayer layer) {
        startNanos = System.nanoTime();
        // assumes will always be a solid layer - probably true enough for us
        if (layer == BlockRenderLayer.SOLID)
            frameCounter++;

        super.renderChunkLayer(layer);

        long duration = (System.nanoTime() - startNanos);
        minNanos = Math.min(minNanos, duration);
        maxNanos = Math.max(maxNanos, duration);
        totalNanos += duration;
        if (frameCounter >= 600) {
            final double ms = totalNanos / 1000000.0;
            Canvas.INSTANCE.getLog()
                    .info(String.format("renderChunkLayer %d frames / %d chunks / %d draws / %d quads (Canvas Renderer)",
                            frameCounter, chunkCounter, drawCounter, quadCounter));
            Canvas.INSTANCE.getLog().info(String.format("renderChunkLayer %f ms / %f ms / %f ms / %f ns",
                    ms / frameCounter, ms / chunkCounter, ms / drawCounter, (double) totalNanos / quadCounter));
            Canvas.INSTANCE.getLog().info(String.format("renderChunkLayer min = %f ms, max = %f ms",
                    minNanos / 1000000.0, maxNanos / 1000000.0));

            totalNanos = 0;
            frameCounter = 0;
            chunkCounter = 0;
            drawCounter = 0;
            quadCounter = 0;
            minNanos = Long.MAX_VALUE;
            maxNanos = 0;
        }
    }
}
