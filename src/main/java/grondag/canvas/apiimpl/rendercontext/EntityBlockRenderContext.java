/*
 * Copyright 2019, 2020 grondag
 *
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
 */

package grondag.canvas.apiimpl.rendercontext;

import java.util.function.Supplier;

import grondag.canvas.buffer.encoding.EncodingContext;
import grondag.canvas.buffer.encoding.VanillaEncoders;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.fermion.sc.concurrency.SimpleConcurrentList;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

/**
 * Context for non-terrain block rendering.
 */
public class EntityBlockRenderContext extends AbstractBlockRenderContext<BlockRenderView> {
	private static final SimpleConcurrentList<AbstractRenderContext> LOADED = new SimpleConcurrentList<>(AbstractRenderContext.class);

	private static final Supplier<ThreadLocal<EntityBlockRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final EntityBlockRenderContext result = new EntityBlockRenderContext();
		LOADED.add(result);
		return result;
	});

	private static ThreadLocal<EntityBlockRenderContext> POOL = POOL_FACTORY.get();

	private final AoCalculator aoCalc = new AoCalculator() {
		@Override
		protected int ao(int cacheIndex) {
			return 255;
		}

		@Override
		protected int brightness(int cacheIndex) {
			return light;
		}

		@Override
		protected boolean isOpaque(int cacheIndex) {
			return false;
		}
	};

	private VertexConsumer bufferBuilder;
	private boolean didOutput = false;
	private int light;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();
	//private Entity entity;
	private float tickDelta;

	public EntityBlockRenderContext() {
		super("BlockRenderContext", VanillaEncoders.VANILLA_BLOCK);
		// WIP2: should be ENTITY_BLOCK or remove
		//		collectors.setContext(EncodingContext.BLOCK);
	}

	public static void reload() {
		LOADED.forEach(c -> c.close());
		LOADED.clear();
		POOL = POOL_FACTORY.get();
	}

	public static EntityBlockRenderContext get() {
		return POOL.get();
	}

	public void tickDelta(float tickDelta) {
		this.tickDelta = tickDelta;
	}

	public void entity(Entity entity) {
		// WIP2: save entity and eye pos?
		final float tickDelta = this.tickDelta;
		//		this.entity = entity;
		final double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
		final double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) + entity.getStandingEyeHeight();
		final double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
		pos.set(x, y, z);
	}

	public boolean tesselate(BlockModelRenderer vanillaRenderer, BakedModel model, BlockState state, MatrixStack matrixStack, VertexConsumerProvider buffers, int overlay, int light) {
		bufferBuilder = buffers.getBuffer(RenderLayers.getEntityBlockLayer(state, false));
		matrix = matrixStack.peek().getModel();
		normalMatrix = (Matrix3fExt) (Object) matrixStack.peek().getNormal();
		this.light = light;
		this.overlay = overlay;
		didOutput = false;
		aoCalc.prepare(0);
		region = CanvasWorldRenderer.instance().getWorld();
		prepareForBlock(state, pos, model.useAmbientOcclusion(), 42);
		((FabricBakedModel) model).emitBlockQuads(region, state, pos, randomSupplier, this);

		bufferBuilder = null;

		return didOutput;
	}

	@Override
	public EncodingContext materialContext() {
		return EncodingContext.BLOCK;
	}

	@Override
	public VertexConsumer consumer(RenderMaterialImpl mat) {
		didOutput = true;

		if (bufferProvider == null) {
			return bufferBuilder;
		} else {
			BlendMode bm = mat.blendMode();

			if (bm == BlendMode.DEFAULT) {
				bm = defaultBlendMode();
			}

			RenderLayer layer;

			switch (bm) {
				case CUTOUT:
				case CUTOUT_MIPPED:
					// WIP2: should be addressed by new pipeline - cutout isn't mipped
					layer = mat.disableDiffuse() ? RenderLayer.getCutout() : TexturedRenderLayers.getEntityCutout();
					break;
				case TRANSLUCENT:
					// WIP2: should be addessed by new pipeline - only moving blocks are supposed to have item target
					if (!MinecraftClient.isFabulousGraphicsOrBetter()) {
						layer = mat.disableDiffuse() ? RenderLayer.getTranslucent() : TexturedRenderLayers.getEntityTranslucentCull();
					} else {
						layer = mat.disableDiffuse() ? RenderLayer.getTranslucent() : TexturedRenderLayers.getItemEntityTranslucentCull();
					}
					break;
				case DEFAULT:
				case SOLID:
				default:
					layer = mat.disableDiffuse() ? RenderLayer.getSolid() : TexturedRenderLayers.getEntitySolid();
					break;
			}

			return bufferProvider.getBuffer(layer);
		}
	}

	@Override
	public int brightness() {
		return light;
	}

	@Override
	public AoCalculator aoCalc() {
		return aoCalc;
	}

	@Override
	protected int fastBrightness(BlockState blockState, BlockPos pos) {
		return light;
	}

	@Override
	protected void adjustMaterial() {
		finder.disableAo(true);
	}
}
