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
import java.util.function.ToIntBiFunction;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.fabricmc.fabric.api.client.model.fabric.FabricBakedModel;
import net.fabricmc.fabric.api.client.model.fabric.Mesh;
import net.fabricmc.fabric.api.client.model.fabric.QuadEmitter;
import net.fabricmc.fabric.api.client.model.fabric.RenderContext;
import net.fabricmc.fabric.api.client.model.fabric.TerrainBlockView;
import grondag.canvas.accessor.AccessBufferBuilder;
import grondag.canvas.aocalc.AoCalculator;
import grondag.canvas.core.CompoundBufferBuilder;
import grondag.canvas.mesh.MutableQuadViewImpl;
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
    private final MeshConsumer meshConsumer = new MeshConsumer(blockInfo, this::brightness, this::outputBuffer, aoCalc,
            this::transform);
    private final Random random = new Random();
    private BlockModelRenderer vanillaRenderer;
    private CompoundBufferBuilder bufferBuilder;
    private long seed;
    private boolean isCallingVanilla = false;
    private boolean didOutput = false;

    private double offsetX;
    private double offsetY;
    private double offsetZ;

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

    private CompoundBufferBuilder outputBuffer(int renderLayer) {
        didOutput = true;
        return bufferBuilder;
    }

    public boolean tesselate(BlockModelRenderer vanillaRenderer, TerrainBlockView blockView, BakedModel model,
            BlockState state, BlockPos pos, BufferBuilder buffer, long seed) {
        this.vanillaRenderer = vanillaRenderer;
        this.bufferBuilder = (CompoundBufferBuilder) buffer;
        this.seed = seed;
        this.didOutput = false;
        aoCalc.clear();
        blockInfo.setBlockView(blockView);
        blockInfo.prepareForBlock(state, pos, model.useAmbientOcclusion());
        setupOffsets();

        ((FabricBakedModel) model).emitBlockQuads(blockView, state, pos, blockInfo.randomSupplier, this);

        this.vanillaRenderer = null;
        blockInfo.release();
        this.bufferBuilder = null;
        return didOutput;
    }

    protected void acceptVanillaModel(BakedModel model) {
        isCallingVanilla = true;
        didOutput = didOutput && vanillaRenderer.tesselate(blockInfo.blockView, model, blockInfo.blockState,
                blockInfo.blockPos, (BufferBuilder) bufferBuilder, false, random, seed);
        isCallingVanilla = false;
    }

    private void setupOffsets() {
        final AccessBufferBuilder buffer = (AccessBufferBuilder) bufferBuilder;
        final BlockPos pos = blockInfo.blockPos;
        offsetX = buffer.fabric_offsetX() + pos.getX();
        offsetY = buffer.fabric_offsetY() + pos.getY();
        offsetZ = buffer.fabric_offsetZ() + pos.getZ();
    }

    private class MeshConsumer extends AbstractMeshConsumer {
        MeshConsumer(BlockRenderInfo blockInfo, ToIntBiFunction<BlockState, BlockPos> brightnessFunc,
                Int2ObjectFunction<CompoundBufferBuilder> bufferFunc, AoCalculator aoCalc, QuadTransform transform) {
            super(blockInfo, brightnessFunc, bufferFunc, aoCalc, transform);
        }

        @Override
        protected void applyOffsets(MutableQuadViewImpl q) {
            final double x = offsetX;
            final double y = offsetY;
            final double z = offsetZ;
            for (int i = 0; i < 4; i++) {
                q.pos(i, (float) (q.x(i) + x), (float) (q.y(i) + y), (float) (q.z(i) + z));
            }
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
