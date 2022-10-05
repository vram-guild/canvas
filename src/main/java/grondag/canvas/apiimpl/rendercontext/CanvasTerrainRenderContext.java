/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.apiimpl.rendercontext;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.math.PackedSectionPos;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.api.world.RenderRegionBakeListener.BlockStateRenderer;
import io.vram.frex.api.world.RenderRegionBakeListener.RenderRegionContext;
import io.vram.frex.base.renderer.ao.AoCalculator;
import io.vram.frex.base.renderer.context.input.BaseBlockInputContext;
import io.vram.frex.base.renderer.context.render.BlockRenderContext;

import grondag.canvas.apiimpl.rendercontext.encoder.TerrainQuadEncoder;
import grondag.canvas.config.Configurator;
import grondag.canvas.light.LightSmoother;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;

public class CanvasTerrainRenderContext extends BlockRenderContext<BlockAndTintGetter> implements BlockStateRenderer {
	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();

	public final InputRegion region;
	public final MatrixStack matrixStack = MatrixStack.create();
	protected final RenderRegionContext<BlockAndTintGetter> bakedListenerContext;

	public final TerrainQuadEncoder encoder;

	@SuppressWarnings("unchecked")
	public CanvasTerrainRenderContext() {
		super();
		region = new InputRegion(this);
		inputContext.prepareForWorld(region, true, matrixStack);
		encoder = new TerrainQuadEncoder(emitter, inputContext);
		bakedListenerContext = (RenderRegionContext<BlockAndTintGetter>) inputContext;
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

		@Override
		protected int cacheIndexFromSectionIndex(int packedSectorIndex) {
			return RenderRegionStateIndexer.packedSectionPosToRegionIndex(packedSectorIndex);
		}
	};

	protected class InputContext extends BaseBlockInputContext<BlockAndTintGetter> implements RenderRegionContext<BlockAndTintGetter> {
		@Override
		protected int fastBrightness(BlockPos pos) {
			return region.cachedBrightness(pos);
		}

		@Override
		public @Nullable Object blockEntityRenderData(BlockPos pos) {
			return region.getBlockEntityRenderAttachment(pos);
		}

		@Override
		public Biome getBiome(BlockPos pos) {
			return region.getBiome(pos);
		}

		@Override
		public boolean hasBiomeAccess() {
			return true;
		}

		@Override
		public BlockPos origin() {
			return searchPos.set(region.originX(), region.originY(), region.originZ());
		}

		@Override
		public MutableBlockPos originOffset(int x, int y, int z) {
			return searchPos.set(region.originX() + x, region.originY() + y, region.originZ() + z);
		}
	}

	@Override
	protected InputContext createInputContext() {
		return new InputContext();
	}

	public CanvasTerrainRenderContext prepareForRegion(PackedInputRegion protoRegion) {
		nonCullBlockEntities.clear();
		addedBlockEntities.clear();
		removedBlockEntities.clear();
		region.prepare(protoRegion);
		encoder.animationBits.clear();

		if (Configurator.lightSmoothing) {
			LightSmoother.computeSmoothedBrightness(region);
		}

		return this;
	}

	public void renderBakeListeners() {
		final var bakeListeners = region.bakeListeners;

		if (!bakeListeners.isEmpty()) {
			final int limit = bakeListeners.size();

			for (int n = 0; n < limit; ++n) {
				final var listener = bakeListeners.get(n);
				inputContext.setWorld(listener.blockViewOverride(region));
				listener.bake(bakedListenerContext, this);
			}

			inputContext.setWorld(region);
		}
	}

	public void renderFluid(BlockState blockState, BlockPos blockPos, final BlockModel model) {
		aoCalc.prepare(PackedSectionPos.packWithSectionMask(blockPos));
		prepareForFluid(blockState, blockPos);
		renderInner(model);
	}

	public void renderBlock(BlockState blockState, BlockPos blockPos, final BakedModel model) {
		aoCalc.prepare(PackedSectionPos.packWithSectionMask(blockPos));
		prepareForBlock(model, blockState, blockPos);
		renderInner((BlockModel) model);
	}

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
		emitter.colorize(inputContext);

		// needs to happen before offsets are applied
		if (!emitter.material().disableAo() && Minecraft.useAmbientOcclusion()) {
			aoCalc.compute(emitter);
		} else if (Configurator.semiFlatLighting) {
			aoCalc.computeFlat(emitter);
		} else {
			emitter.applyFlatLighting(inputContext.flatBrightness(emitter));
		}
	}

	@Override
	protected void encodeQuad() {
		encoder.encode();
	}

	@Override
	public void bake(BlockPos pos, BlockState state) {
		matrixStack.push();
		matrixStack.translate(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
		renderBlock(state, pos, BlockModel.get(state));
		matrixStack.pop();
	}
}
