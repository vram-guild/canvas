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

import static grondag.canvas.apiimpl.util.GeometryHelper.LIGHT_FACE_FLAG;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.util.ColorHelper;

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
	public boolean defaultAo;
	public RenderLayer defaultLayer;

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

	public int blockColor(int colorIndex) {
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

	public boolean shouldDrawFace(Direction face) {
		return true;
	}

	public RenderLayer effectiveRenderLayer(BlendMode blendMode) {
		return blendMode == BlendMode.DEFAULT ? defaultLayer : blendMode.blockRenderLayer;
	}

	public void applyBlockLighting(MutableQuadViewImpl quad) {
		final RenderMaterialImpl.Value mat = quad.material();

		// TODO: handle multiple
		final int textureIndex = 0;

		if (defaultAo && !mat.disableAo(textureIndex)) {
			if (mat.emissive(textureIndex)) {
				tesselateSmoothEmissive(quad);
			} else {
				tesselateSmooth(quad);
			}
		} else {
			if (mat.emissive(textureIndex)) {
				tesselateFlatEmissive(quad);
			} else {
				tesselateFlat(quad);
			}
		}
	}

	// routines below have a bit of copy-paste code reuse to avoid conditional execution inside a hot loop

	static final int FULL_BRIGHTNESS = 0xF000F0;

	/** for non-emissive mesh quads and all fallback quads with smooth lighting. */
	private void tesselateSmooth(MutableQuadViewImpl q) {
		for (int i = 0; i < 4; i++) {
			q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), q.ao[i]));
			q.lightmap(i, ColorHelper.maxBrightness(q.lightmap(i), q.light[i]));
		}
	}

	/** for emissive mesh quads with smooth lighting. */
	private void tesselateSmoothEmissive(MutableQuadViewImpl q) {
		for (int i = 0; i < 4; i++) {
			q.spriteColor(i, 0, ColorHelper.multiplyRGB(q.spriteColor(i, 0), q.ao[i]));
			q.lightmap(i, FULL_BRIGHTNESS);
		}
	}

	/** for non-emissive mesh quads and all fallback quads with flat lighting. */
	private void tesselateFlat(MutableQuadViewImpl quad) {
		final int brightness = flatBrightness(quad);

		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), brightness));
		}
	}

	/** for emissive mesh quads with flat lighting. */
	private void tesselateFlatEmissive(MutableQuadViewImpl quad) {
		for (int i = 0; i < 4; i++) {
			quad.lightmap(i, FULL_BRIGHTNESS);
		}
	}

	private final BlockPos.Mutable mpos = new BlockPos.Mutable();

	/**
	 * Handles geometry-based check for using self brightness or neighbor brightness.
	 * That logic only applies in flat lighting.
	 */
	private int flatBrightness(MutableQuadViewImpl quad) {
		final BlockState blockState = this.blockState;
		final BlockPos pos = blockPos;

		mpos.set(pos);

		if ((quad.geometryFlags() & LIGHT_FACE_FLAG) != 0 || Block.isShapeFullCube(blockState.getCollisionShape(blockView, pos))) {
			mpos.setOffset(quad.lightFace());
		}

		// Unfortunately cannot use brightness cache here unless we implement one specifically for flat lighting. See #329
		return WorldRenderer.getLightmapCoordinates(blockView, blockState, mpos);
	}
}
