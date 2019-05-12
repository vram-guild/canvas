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

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Consumer;

import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.util.AoCalculator;
import grondag.canvas.buffer.packing.CanvasBufferBuilder;
import grondag.canvas.buffer.packing.VertexCollector;
import grondag.canvas.draw.TessellatorExt;
import grondag.canvas.material.ShaderContext;
import grondag.canvas.material.ShaderProps;
import grondag.frex.api.model.DynamicBakedModel;
import grondag.frex.api.mesh.Mesh;
import grondag.frex.api.mesh.QuadEmitter;
import grondag.frex.api.render.RenderContext;
import grondag.frex.api.render.TerrainBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractRenderContext implements RenderContext {
    private static ShaderContext contextFunc(RenderMaterialImpl.Value mat) {
        return ShaderContext.BLOCK_SOLID;
    }
    
    private final BlockRenderInfo blockInfo = new BlockRenderInfo();
    private final AoCalculator aoCalc = new AoCalculator(blockInfo, this::brightness, this::aoLevel);
    private final MeshConsumer meshConsumer = new MeshConsumer(blockInfo, this::brightness, this::getCollector, aoCalc,
            this::transform, QuadRenderer.NO_OFFSET, BlockRenderContext::contextFunc);
    private final FallbackConsumer fallbackConsumer = new FallbackConsumer(blockInfo, this::brightness, this::getCollector, aoCalc,
            this::transform, QuadRenderer.NO_OFFSET, BlockRenderContext::contextFunc);
    private final TessellatorExt tesselatorExt = (TessellatorExt) Tessellator.getInstance();
    private CanvasBufferBuilder canvasBuilder;
    private boolean didOutput = false;

    private int brightness(BlockPos pos) {
        final ExtendedBlockView blockView = blockInfo.blockView;
        if (blockView == null) {
            return 15 << 20 | 15 << 4;
        }
        return blockView.getBlockState(pos).getBlockBrightness(blockView, pos);
    }

    private float aoLevel(BlockPos pos) {
        final ExtendedBlockView blockView = blockInfo.blockView;
        if (blockView == null) {
            return 1f;
        }
        return blockView.getBlockState(pos).getAmbientOcclusionLightLevel(blockView, pos);
    }

    private VertexCollector getCollector(RenderMaterialImpl.Value mat, QuadViewImpl quad) {
        didOutput = true;
        int props = ShaderProps.classify(mat, quad, ShaderContext.BLOCK_SOLID);
        return canvasBuilder.vcList.get(mat, props);
    }

    public boolean tesselate(BlockModelRenderer vanillaRenderer, TerrainBlockView blockView, BakedModel model,
            BlockState state, BlockPos pos, BufferBuilder buffer, long seed) {
        this.canvasBuilder = (CanvasBufferBuilder) buffer;
        this.didOutput = false;
        aoCalc.clear();
        blockInfo.setBlockView(blockView);
        blockInfo.prepareForBlock(state, pos, model.useAmbientOcclusion());
        tesselatorExt.canvas_context(ShaderContext.BLOCK_SOLID);
        ((DynamicBakedModel) model).emitBlockQuads(blockView, state, pos, blockInfo.randomSupplier, this);

        blockInfo.release();
        this.canvasBuilder = null;
        return didOutput;
    }

//    private void setupOffsets() {
//        final AccessBufferBuilder buffer = (AccessBufferBuilder) canvasBuilder;
//        final BlockPos pos = blockInfo.blockPos;
//        offsetX = buffer.fabric_offsetX() + pos.getX();
//        offsetY = buffer.fabric_offsetY() + pos.getY();
//        offsetZ = buffer.fabric_offsetZ() + pos.getZ();
//    }

    @Override
    public Consumer<Mesh> meshConsumer() {
        return meshConsumer;
    }

    @Override
    public Consumer<BakedModel> fallbackConsumer() {
        return fallbackConsumer;
    }

    @Override
    public QuadEmitter getEmitter() {
        return meshConsumer.getEmitter();
    }
}
