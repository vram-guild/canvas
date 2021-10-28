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

import static io.vram.frex.base.renderer.util.EncoderUtil.colorizeQuad;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.base.renderer.context.BaseBlockContext;
import io.vram.frex.base.renderer.util.EncoderUtil;

import grondag.canvas.apiimpl.rendercontext.base.AbstractBlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.encoder.TerrainQuadEncoder;
import grondag.canvas.config.Configurator;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.light.LightSmoother;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;

/**
 * Context for non-terrain block rendering.
 */
public class CanvasTerrainRenderContext extends AbstractBlockRenderContext<InputRegion, TerrainQuadEncoder> {
	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();

	public final InputRegion region;
	public final MatrixStack matrixStack = MatrixStack.cast(new PoseStack());

	public CanvasTerrainRenderContext() {
		super();
		region = new InputRegion(this);
		inputContext.prepareForWorld(region, true, matrixStack);
	}

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

	@Override
	protected BaseBlockContext<InputRegion> createInputContext() {
		return new BaseBlockContext<>() {
			@Override
			protected int fastBrightness(BlockPos pos) {
				return region.cachedBrightness(pos);
			}

			@Override
			public @Nullable Object blockEntityRenderData(BlockPos pos) {
				return region.getBlockEntityRenderAttachment(pos);
			}
		};
	}

	public CanvasTerrainRenderContext prepareForRegion(PackedInputRegion protoRegion) {
		nonCullBlockEntities.clear();
		addedBlockEntities.clear();
		removedBlockEntities.clear();
		region.prepare(protoRegion);
		encoder.animationBits.clear();

		if (Configurator.lightSmoothing) {
			//            final long start = counter.startRun();
			LightSmoother.computeSmoothedBrightness(region);
		}

		return this;
	}

	public void renderFluid(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model) {
		aoCalc.prepare(RenderRegionStateIndexer.interiorIndex(blockPos));
		prepareForFluid(blockState, blockPos, defaultAo);
		renderInner(model);
	}

	public void renderBlock(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BakedModel model) {
		aoCalc.prepare(RenderRegionStateIndexer.interiorIndex(blockPos));
		prepareForBlock(model, blockState, blockPos, defaultAo);
		renderInner((BlockModel) model);
	}

	// PERF: don't pass in matrixStack each time, just change model matrix directly
	private void renderInner(final BlockModel model) {
		try {
			model.renderAsBlock(this.inputContext, emitter());
		} catch (final Throwable var9) {
			final CrashReport crashReport_1 = CrashReport.forThrowable(var9, "Tesselating block in world - Canvas Renderer");
			final CrashReportCategory crashReportElement_1 = crashReport_1.addCategory("Block being tesselated");
			CrashReportCategory.populateBlockDetails(crashReportElement_1, region, inputContext.pos(), inputContext.blockState());
			throw new ReportedException(crashReport_1);
		}
	}

	@Override
	protected void shadeQuad() {
		// needs to happen before offsets are applied
		if (!emitter.material().disableAo() && Minecraft.useAmbientOcclusion()) {
			aoCalc.compute(emitter);
		} else if (Configurator.semiFlatLighting) {
			aoCalc.computeFlat(emitter);
		} else {
			EncoderUtil.applyFlatLighting(emitter, inputContext.flatBrightness(emitter));
		}

		colorizeQuad(emitter, this.inputContext);
	}

	@Override
	protected TerrainQuadEncoder createEncoder() {
		return new TerrainQuadEncoder(emitter, inputContext);
	}

	@Override
	protected void encodeQuad() {
		encoder.encode();
	}
}
