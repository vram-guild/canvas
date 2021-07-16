/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.apiimpl.rendercontext;

import static grondag.canvas.buffer.format.EncoderUtils.applyBlockLighting;
import static grondag.canvas.buffer.format.EncoderUtils.colorizeQuad;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.config.Configurator;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.light.LightSmoother;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;

/**
 * Implementation of {@link RenderContext} used during terrain rendering.
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

	private int packedRelativeBlockPos;

	@Override
	public int packedRelativeBlockPos() {
		return packedRelativeBlockPos;
	}

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

		if (Configurator.lightSmoothing) {
			//            final long start = counter.startRun();
			LightSmoother.computeSmoothedBrightness(region);
		}

		return this;
	}

	public void renderFluid(BlockState blockState, BlockPos blockPos, boolean defaultAo, final FabricBakedModel model, MatrixStack matrixStack) {
		isFluidModel = true;
		renderInner(blockState, blockPos, defaultAo, model, matrixStack);
	}

	public void renderBlock(BlockState blockState, BlockPos blockPos, boolean defaultAo, final FabricBakedModel model, MatrixStack matrixStack) {
		isFluidModel = false;
		renderInner(blockState, blockPos, defaultAo, model, matrixStack);
	}

	// PERF: don't pass in matrixStack each time, just change model matrix directly
	private void renderInner(BlockState blockState, BlockPos blockPos, boolean defaultAo, final FabricBakedModel model, MatrixStack matrixStack) {
		matrix = matrixStack.peek().getModel();

		// PERF: can probably grab this at prepare
		normalMatrix = (Matrix3fExt) (Object) matrixStack.peek().getNormal();

		try {
			aoCalc.prepare(RenderRegionStateIndexer.interiorIndex(blockPos));
			prepareForBlock(blockState, blockPos, defaultAo, -1);

			if (!Configurator.terrainRenderConfig.shouldApplyBlockPosTranslation) {
				packedRelativeBlockPos = (blockPos.getX() & 0xF) | ((blockPos.getY() & 0xF) << 4) | ((blockPos.getZ() & 0xF) << 8);
			}

			cullCompletionFlags = 0;
			cullResultFlags = 0;
			model.emitBlockQuads(region, blockState, blockPos, randomSupplier, this);
		} catch (final Throwable var9) {
			final CrashReport crashReport_1 = CrashReport.create(var9, "Tesselating block in world - Canvas Renderer");
			final CrashReportSection crashReportElement_1 = crashReport_1.addElement("Block being tesselated");
			CrashReportSection.addBlockInfo(crashReportElement_1, region, blockPos, blockState);
			throw new CrashException(crashReport_1);
		}
	}

	@Override
	public int brightness() {
		return 0;
	}

	@Override
	public void computeAo(MutableQuadViewImpl quad) {
		aoCalc.compute(quad);
	}

	@Override
	public void computeFlat(MutableQuadViewImpl quad) {
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
	protected boolean cullTest(int faceIndex) {
		if (faceIndex == ModelHelper.NULL_FACE_ID) {
			return true;
		}

		final int mask = 1 << faceIndex;

		if ((cullCompletionFlags & mask) == 0) {
			cullCompletionFlags |= mask;
			final Direction face = ModelHelper.faceFromIndex(faceIndex);

			if (Block.shouldDrawSide(blockState, region, blockPos, face, internalSearchPos.set(blockPos, face))) {
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
	protected void encodeQuad(MutableQuadViewImpl quad) {
		// needs to happen before offsets are applied
		applyBlockLighting(quad, this);
		colorizeQuad(quad, this);
		Configurator.terrainRenderConfig.transcoder.encode(quad, this, collectors.get(quad.material()));
	}
}
