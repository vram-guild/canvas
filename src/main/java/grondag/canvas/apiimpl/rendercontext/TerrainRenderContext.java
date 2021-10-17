/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.apiimpl.rendercontext;

import static grondag.canvas.buffer.format.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.format.EncoderUtils.colorizeQuad;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.model.BlockModel;
import io.vram.frex.api.model.util.FaceUtil;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.config.Configurator;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.light.LightSmoother;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;

/**
 * Implementation of RenderContext used during terrain rendering.
 * Dispatches calls from models during chunk rebuild to the appropriate consumer,
 * and holds/manages all of the state needed by them.
 */
public class TerrainRenderContext extends AbstractBlockRenderContext<InputRegion> {
	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();

	private final AoCalculator aoCalc = new AoCalculator() {
		@Override
		protected int ao(int cacheIndex) {
			return region.cachedAoLevel(cacheIndex);
		}

		@Override
		protected int brightness(int cacheIndex) {
			return region.cachedBrightness(cacheIndex);
		}

		@Override
		protected boolean isOpaque(int cacheIndex) {
			return region.isClosed(cacheIndex);
		}
	};

	private int cullCompletionFlags;
	private int cullResultFlags;

	public TerrainRenderContext() {
		super("TerrainRenderContext");
		region = new InputRegion(this);
		collectors = new VertexCollectorList(true);
	}

	public TerrainRenderContext prepareForRegion(PackedInputRegion protoRegion) {
		nonCullBlockEntities.clear();
		addedBlockEntities.clear();
		removedBlockEntities.clear();
		region.prepare(protoRegion);
		animationBits.clear();

		if (Configurator.lightSmoothing) {
			//            final long start = counter.startRun();
			LightSmoother.computeSmoothedBrightness(region);
		}

		return this;
	}

	public void renderFluid(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model, PoseStack matrixStack) {
		isFluidModel = true;
		renderInner(blockState, blockPos, defaultAo, model, matrixStack);
	}

	public void renderBlock(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model, PoseStack matrixStack) {
		isFluidModel = false;
		renderInner(blockState, blockPos, defaultAo, model, matrixStack);
	}

	// PERF: don't pass in matrixStack each time, just change model matrix directly
	private void renderInner(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model, PoseStack matrixStack) {
		encodingContext.prepare(matrixStack);

		try {
			aoCalc.prepare(RenderRegionStateIndexer.interiorIndex(blockPos));
			prepareForBlock(blockState, blockPos, defaultAo, -1);
			cullCompletionFlags = 0;
			cullResultFlags = 0;
			model.renderAsBlock(this, emitter());
		} catch (final Throwable var9) {
			final CrashReport crashReport_1 = CrashReport.forThrowable(var9, "Tesselating block in world - Canvas Renderer");
			final CrashReportCategory crashReportElement_1 = crashReport_1.addCategory("Block being tesselated");
			CrashReportCategory.populateBlockDetails(crashReportElement_1, region, blockPos, blockState);
			throw new ReportedException(crashReport_1);
		}
	}

	@Override
	public int brightness() {
		return 0;
	}

	@Override
	public void computeAo(BaseQuadEmitter quad) {
		aoCalc.compute(quad);
	}

	@Override
	public void computeFlat(BaseQuadEmitter quad) {
		if (Configurator.semiFlatLighting) {
			aoCalc.computeFlat(quad);
		} else if (Configurator.hdLightmaps()) {
			// FEAT: per-vertex light maps will be ignored unless we bake a custom HD map
			// or retain vertex light maps in buffer format and logic in shader to take max
			aoCalc.computeFlatHd(quad, flatBrightness(quad));
		} else {
			computeFlatSimple(quad);
		}
	}

	@Override
	protected int fastBrightness(BlockState blockState, BlockPos pos) {
		return region.cachedBrightness(pos);
	}

	@Override
	public boolean cullTest(int faceIndex) {
		if (faceIndex == FaceUtil.UNASSIGNED_INDEX) {
			return true;
		}

		final int mask = 1 << faceIndex;

		if ((cullCompletionFlags & mask) == 0) {
			cullCompletionFlags |= mask;
			final Direction face = FaceUtil.faceFromIndex(faceIndex);

			if (Block.shouldRenderFace(blockState, region, blockPos, face, internalSearchPos.setWithOffset(blockPos, face))) {
				cullResultFlags |= mask;
				return true;
			} else {
				return false;
			}
		} else {
			return (cullResultFlags & mask) != 0;
		}
	}

	@Override
	protected void encodeQuad(BaseQuadEmitter quad) {
		// needs to happen before offsets are applied
		applyBlockLighting(quad, this);
		colorizeQuad(quad, this);
		TerrainFormat.TERRAIN_ENCODER.encode(quad, encodingContext, collectors.get((CanvasRenderMaterial) quad.material()));
	}

	@Override
	public @Nullable Object blockEntityRenderData(BlockPos pos) {
		return region.getBlockEntityRenderAttachment(pos);
	}
}
