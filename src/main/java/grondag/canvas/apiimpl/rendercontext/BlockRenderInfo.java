/*******************************************************************************
 * Copyright 2019, 2020 grondag
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

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

/**
 * Holds, manages and provides access to the block/world related state
 * needed by fallback and mesh consumers.
 *
 * <p>Exception: per-block position offsets are tracked in {@link ChunkRenderInfo}
 * so they can be applied together with chunk offsets.
 */
public class BlockRenderInfo {
	private final BlockColors blockColorMap = MinecraftClient.getInstance().getBlockColorMap();
	private boolean needsRandomRefresh = true;
	private int lastColorIndex = -1;
	private int blockColor = -1;

	public final Random random = new Random();
	public BlockRenderView blockView;
	public BlockPos blockPos;
	public BlockState blockState;
	public long seed;
	boolean defaultAo;
	RenderLayer defaultLayer;

	public final Supplier<Random> randomSupplier = () -> {
		final Random result = random;

		if(needsRandomRefresh) {
			needsRandomRefresh = false;
			long seed = this.seed;

			if (seed == -1L) {
				seed = blockState.getRenderingSeed(blockPos);
				this.seed = seed;
			}

			result.setSeed(seed);
		}

		return result;
	};

	public void setBlockView(BlockRenderView blockView) {
		this.blockView = blockView;
	}

	/**
	 *
	 * @param blockState
	 * @param blockPos
	 * @param modelAO
	 * @param seed pass -1 for default behavior
	 */
	public void prepareForBlock(BlockState blockState, BlockPos blockPos, boolean modelAO, long seed) {
		this.blockPos = blockPos;
		this.blockState = blockState;
		lastColorIndex = -1;
		needsRandomRefresh = true;
		this.seed = seed;
		defaultAo = modelAO && MinecraftClient.isAmbientOcclusionEnabled() && blockState.getLuminance() == 0;
		defaultLayer = RenderLayers.getBlockLayer(blockState);
	}

	public void release() {
		blockPos = null;
		blockState = null;
	}

	int blockColor(int colorIndex) {
		if(colorIndex == -1) {
			return -1;
		} else if(lastColorIndex == colorIndex) {
			return blockColor;
		} else {
			lastColorIndex = colorIndex;
			final int result = 0xFF000000 | blockColorMap.getColor(blockState, blockView, blockPos, colorIndex);
			blockColor = result;
			return result;
		}
	}

	boolean shouldDrawFace(Direction face) {
		return true;
	}

	RenderLayer effectiveRenderLayer(BlendMode blendMode) {
		return blendMode == BlendMode.DEFAULT ? defaultLayer : blendMode.blockRenderLayer;
	}
}
