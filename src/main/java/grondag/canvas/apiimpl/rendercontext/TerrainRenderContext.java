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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.chunk.BuiltRenderRegion;
import grondag.canvas.chunk.FastRenderRegion;
import grondag.canvas.chunk.ProtoRenderRegion;
import grondag.canvas.chunk.RenderRegionAddressHelper;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.mixinterface.Matrix3fExt;

/**
 * Implementation of {@link RenderContext} used during terrain rendering.
 * Dispatches calls from models during chunk rebuild to the appropriate consumer,
 * and holds/manages all of the state needed by them.
 */
public class TerrainRenderContext extends AbstractBlockRenderContext<FastRenderRegion> {
	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();

	private int cullCompletionFlags;
	private int cullResultFlags;

	/**
	 * See {@link BuiltRenderRegion#backfaceCullFlags}
	 */
	private int backfaceCullFlags;

	public int backfaceCullFlags() {
		return backfaceCullFlags;
	}

	public TerrainRenderContext() {
		region = new FastRenderRegion(this);
	}

	private final AoCalculator aoCalc = new AoCalculator() {
		@Override
		protected float ao(int cacheIndex) {
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

	public TerrainRenderContext prepareRegion(ProtoRenderRegion protoRegion) {
		nonCullBlockEntities.clear();
		addedBlockEntities.clear();
		removedBlockEntities.clear();
		region.prepare(protoRegion);
		backfaceCullFlags = protoRegion.backfaceCullFlags;

		// TODO: renable smooth lighting
		if(Configurator.lightSmoothing) {
			//            final long start = counter.startRun();
			//LightSmoother.computeSmoothedBrightness(null, null, null); // chunkOrigin, blockView, blockView.brightnessCache);
		}

		return this;
	}

	/** Called from chunk renderer hook. */
	public void tesselateBlock(BlockState blockState, BlockPos blockPos, final BakedModel model, MatrixStack matrixStack) {
		matrix = matrixStack.peek().getModel();
		normalMatrix = (Matrix3fExt)(Object) matrixStack.peek().getNormal();

		try {
			aoCalc.prepare(RenderRegionAddressHelper.interiorIndex(blockPos));
			prepareForBlock(blockState, blockPos, model.useAmbientOcclusion(), -1);
			cullCompletionFlags = backfaceCullFlags;
			cullResultFlags = 0;
			((FabricBakedModel) model).emitBlockQuads(region, blockState, blockPos, randomSupplier, this);
		} catch (final Throwable var9) {
			final CrashReport crashReport_1 = CrashReport.create(var9, "Tesselating block in world - Indigo Renderer");
			final CrashReportSection crashReportElement_1 = crashReport_1.addElement("Block being tesselated");
			CrashReportSection.addBlockInfo(crashReportElement_1, blockPos, blockState);
			throw new CrashException(crashReport_1);
		}
	}

	@Override
	public MaterialContext materialContext() {
		return MaterialContext.TERRAIN;
	}

	@Override
	public VertexConsumer consumer(MutableQuadViewImpl quad) {
		final RenderLayer layer = effectiveRenderLayer(quad.material().blendMode(0));
		return collectors.get(MaterialContext.TERRAIN, layer);
	}

	@Override
	public int brightness() {
		return 0;
	}

	@Override
	public AoCalculator aoCalc() {
		return aoCalc;
	}

	@Override
	protected int fastBrightness(BlockState blockState, BlockPos pos) {
		return region.cachedBrightness(pos);
	}

	@Override
	public boolean cullTest(Direction face) {
		if (face == null) {
			return true;
		}

		final int mask = 1 << face.getId();

		if ((cullCompletionFlags & mask) == 0) {
			cullCompletionFlags |= mask;

			if (Block.shouldDrawSide(blockState, region, blockPos, face)) {
				cullResultFlags |= mask;
				return true;
			} else {
				return false;
			}
		} else {
			return (cullResultFlags & mask) != 0;
		}
	}
}
