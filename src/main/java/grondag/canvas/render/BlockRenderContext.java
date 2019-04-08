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

/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grondag.canvas.render;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import grondag.canvas.RenderMaterialImpl;
import grondag.canvas.helper.AoCalculator;
import grondag.canvas.buffering.VertexCollector;
import grondag.canvas.core.CanvasBufferBuilder;
import grondag.frex.api.core.FabricBakedModel;
import grondag.frex.api.core.Mesh;
import grondag.frex.api.core.QuadEmitter;
import grondag.frex.api.core.RenderContext;
import grondag.frex.api.core.TerrainBlockView;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractRenderContext implements RenderContext {
    private final BlockRenderInfo blockInfo = new BlockRenderInfo();
    private final AoCalculator aoCalc = new AoCalculator(blockInfo, this::brightness, this::aoLevel);
    private final MeshConsumer meshConsumer = new MeshConsumer(blockInfo, this::brightness, this::getCollector, aoCalc,
            this::transform);
    private final Random random = new Random();
    private CanvasBufferBuilder canvasBuilder;
    private BlockModelRenderer vanillaRenderer;
    private long seed;
    private boolean isCallingVanilla = false;
    private boolean didOutput = false;

    public boolean isCallingVanilla() {
        return isCallingVanilla;
    }

    private int brightness(BlockState blockState, BlockPos pos) {
        if (blockInfo.blockView == null) {
            return 15 << 20 | 15 << 4;
        }
        return blockState.getBlockBrightness(blockInfo.blockView, pos);
    }

    private float aoLevel(BlockPos pos) {
        final ExtendedBlockView blockView = blockInfo.blockView;
        if (blockView == null) {
            return 1f;
        }
        return blockView.getBlockState(pos).getAmbientOcclusionLightLevel(blockView, pos);
    }

    private VertexCollector getCollector(RenderMaterialImpl.Value mat) {
        didOutput = true;
        return canvasBuilder.vcList.get(mat);
    }

    public boolean tesselate(BlockModelRenderer vanillaRenderer, TerrainBlockView blockView, BakedModel model,
            BlockState state, BlockPos pos, BufferBuilder buffer, long seed) {
        this.vanillaRenderer = vanillaRenderer;
        this.canvasBuilder = (CanvasBufferBuilder) buffer;
        this.seed = seed;
        this.didOutput = false;
        aoCalc.clear();
        blockInfo.setBlockView(blockView);
        blockInfo.prepareForBlock(state, pos, model.useAmbientOcclusion());

        ((FabricBakedModel) model).emitBlockQuads(blockView, state, pos, blockInfo.randomSupplier, this);

        this.vanillaRenderer = null;
        blockInfo.release();
        this.canvasBuilder = null;
        return didOutput;
    }

    // TODO: implement fallback consumer
    protected void acceptVanillaModel(BakedModel model) {
        isCallingVanilla = true;
        didOutput = didOutput && vanillaRenderer.tesselate(blockInfo.blockView, model, blockInfo.blockState,
                blockInfo.blockPos, canvasBuilder, false, random, seed);
        isCallingVanilla = false;
    }

//    private void setupOffsets() {
//        final AccessBufferBuilder buffer = (AccessBufferBuilder) canvasBuilder;
//        final BlockPos pos = blockInfo.blockPos;
//        offsetX = buffer.fabric_offsetX() + pos.getX();
//        offsetY = buffer.fabric_offsetY() + pos.getY();
//        offsetZ = buffer.fabric_offsetZ() + pos.getZ();
//    }

    private class MeshConsumer extends AbstractMeshConsumer {
        MeshConsumer(BlockRenderInfo blockInfo, ToIntBiFunction<BlockState, BlockPos> brightnessFunc,
                Function<RenderMaterialImpl.Value, VertexCollector> collectorFunc, AoCalculator aoCalc, QuadTransform transform) {
            super(blockInfo, brightnessFunc, collectorFunc, aoCalc, transform);
        }

        @Override
        protected void applyOffsets() {
            // NOOP: Nothing to do in block render context - offsets handled in CanvasBuilder / VertexCollectorList
        }
    }

    @Override
    public Consumer<Mesh> meshConsumer() {
        return meshConsumer;
    }

    @Override
    public Consumer<BakedModel> fallbackConsumer() {
        return this::acceptVanillaModel;
    }

    @Override
    public QuadEmitter getEmitter() {
        return meshConsumer.getEmitter();
    }
}
