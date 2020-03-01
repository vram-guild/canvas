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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
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
public class TerrainRenderContext extends AbstractRenderContext implements RenderContext {
	private final TerrainBlockRenderInfo blockInfo = new TerrainBlockRenderInfo();
	public final FastRenderRegion region = new FastRenderRegion(this);

	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();

	public TerrainRenderContext() {
		blockInfo.setBlockView(region);
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

	/** for use by chunk builder - avoids another threadlocal */
	public final BlockPos.Mutable searchPos = new BlockPos.Mutable();

	public TerrainRenderContext prepareRegion(ProtoRenderRegion protoRegion) {
		nonCullBlockEntities.clear();
		addedBlockEntities.clear();
		removedBlockEntities.clear();
		region.prepare(protoRegion);
		return this;
	}

	public void release() {
		blockInfo.release();
	}

	/** Called from chunk renderer hook. */
	public void tesselateBlock(BlockState blockState, BlockPos blockPos, final BakedModel model, MatrixStack matrixStack) {
		matrix = matrixStack.peek().getModel();
		normalMatrix = (Matrix3fExt)(Object) matrixStack.peek().getNormal();

		try {
			aoCalc.prepare(RenderRegionAddressHelper.interiorIndex(blockPos));
			blockInfo.prepareForBlock(blockState, blockPos, model.useAmbientOcclusion(), -1);
			((FabricBakedModel) model).emitBlockQuads(blockInfo.blockView, blockInfo.blockState, blockInfo.blockPos, blockInfo.randomSupplier, this);
		} catch (final Throwable var9) {
			final CrashReport crashReport_1 = CrashReport.create(var9, "Tesselating block in world - Indigo Renderer");
			final CrashReportSection crashReportElement_1 = crashReport_1.addElement("Block being tesselated");
			CrashReportSection.addBlockInfo(crashReportElement_1, blockPos, blockState);
			throw new CrashException(crashReport_1);
		}
	}

	@Override
	protected boolean cullTest(Direction face) {
		return blockInfo.shouldDrawFace(face);
	}

	@Override
	public MaterialContext materialContext() {
		return MaterialContext.TERRAIN;
	}

	@Override
	public int overlay() {
		return overlay;
	}

	@Override
	public Matrix4f matrix() {
		return matrix;
	}

	@Override
	public Matrix3fExt normalMatrix() {
		return normalMatrix;
	}

	@Override
	public void computeLighting(MutableQuadViewImpl quad) {
		aoCalc.compute(quad);
	}

	@Override
	protected Random random() {
		return blockInfo.randomSupplier.get();
	}

	@Override
	protected boolean defaultAo() {
		return blockInfo.defaultAo;
	}

	@Override
	protected BlockState blockState() {
		return blockInfo.blockState;
	}

	@Override
	public VertexConsumer consumer(MutableQuadViewImpl quad) {
		final RenderLayer layer = blockInfo.effectiveRenderLayer(quad.material().blendMode(0));
		return collectors.get(MaterialContext.TERRAIN, layer);
	}

	@Override
	public final int indexedColor(int colorIndex) {
		return blockInfo.blockColor(colorIndex);
	}

	@Override
	public final void applyLighting(MutableQuadViewImpl quad) {
		blockInfo.applyBlockLighting(quad);
	}
}
