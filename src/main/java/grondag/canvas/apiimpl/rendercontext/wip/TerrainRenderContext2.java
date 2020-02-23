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


package grondag.canvas.apiimpl.rendercontext.wip;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.Matrix3f;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.rendercontext.ChunkRenderInfo;
import grondag.canvas.apiimpl.rendercontext.TerrainBlockRenderInfo;
import grondag.canvas.chunk.FastRenderRegion;
import grondag.canvas.chunk.ProtoRenderRegion;
import grondag.canvas.chunk.RegionData;
import grondag.canvas.chunk.RenderRegionAddressHelper;
import grondag.canvas.light.AoCalculator;

/**
 * Implementation of {@link RenderContext} used during terrain rendering.
 * Dispatches calls from models during chunk rebuild to the appropriate consumer,
 * and holds/manages all of the state needed by them.
 */
public class TerrainRenderContext2 extends AbstractRenderContext implements RenderContext {
	private final TerrainBlockRenderInfo blockInfo = new TerrainBlockRenderInfo();
	private final ChunkRenderInfo chunkInfo = new ChunkRenderInfo();
	public final FastRenderRegion region = null;//new FastRenderRegion(this);

	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();

	public TerrainRenderContext2() {
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

	private final AbstractMeshConsumer2 meshConsumer = new AbstractMeshConsumer2(blockInfo, chunkInfo::getInitializedBuffer, aoCalc, this::transform) {
		@Override
		public int overlay() {
			return overlay;
		}

		@Override
		public Matrix4f matrix() {
			return matrix;
		}

		@Override
		public Matrix3f normalMatrix() {
			return normalMatrix;
		}
	};

	private final TerrainFallbackConsumer2 fallbackConsumer = new TerrainFallbackConsumer2(blockInfo, chunkInfo::getInitializedBuffer, aoCalc, this::transform) {
		@Override
		public int overlay() {
			return overlay;
		}

		@Override
		public Matrix4f matrix() {
			return matrix;
		}

		@Override
		public Matrix3f normalMatrix() {
			return normalMatrix;
		}
	};

	public TerrainRenderContext2 prepareRegion(ProtoRenderRegion protoRegion) {
		nonCullBlockEntities.clear();
		addedBlockEntities.clear();
		removedBlockEntities.clear();
		region.prepare(protoRegion);
		return this;
	}

	public void prepareChunk(RegionData chunkData, BlockBufferBuilderStorage builders, BlockPos origin) {
		chunkInfo.prepare(chunkData, builders, origin);
	}

	public void release() {
		chunkInfo.release();
		blockInfo.release();
	}

	/** Called from chunk renderer hook. */
	public void tesselateBlock(BlockState blockState, BlockPos blockPos, final BakedModel model, MatrixStack matrixStack) {
		matrix = matrixStack.peek().getModel();
		normalMatrix = matrixStack.peek().getNormal();

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
	public Consumer<Mesh> meshConsumer() {
		return meshConsumer;
	}

	@Override
	public Consumer<BakedModel> fallbackConsumer() {
		return fallbackConsumer;
	}

	@Override
	public QuadEmitter getEmitter() {
		return meshConsumer.getEmitter();
	}
}
