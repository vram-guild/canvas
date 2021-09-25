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

package grondag.canvas.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.mixinterface.ItemRendererExt;
import grondag.canvas.wip.RenderTypeUtil;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer implements ItemRendererExt {
	@Shadow private ItemModelShaper itemModelShaper;
	@Shadow private BlockEntityWithoutLevelRenderer blockEntityRenderer;

	@Override
	public BlockEntityWithoutLevelRenderer canvas_builtinModelItemRenderer() {
		return blockEntityRenderer;
	}

	/**
	 * @author grondag
	 * @reason simplicity
	 */
	@Overwrite
	public void render(ItemStack stack, ItemTransforms.TransformType renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, BakedModel model) {
		ItemRenderContext.get().renderItem(itemModelShaper, stack, renderMode, leftHanded, matrices, vertexConsumers, light, overlay, model);
	}

	@Inject(at = @At("HEAD"), method = "getArmorFoilBuffer", cancellable = true)
	private static void onGetArmorFoilBuffer(MultiBufferSource provider, RenderType layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> ci) {
		if (glint && provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderTypeUtil.toMaterial(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getCompassFoilBuffer", cancellable = true)
	private static void onGetCompassFoilBuffer(MultiBufferSource provider, RenderType layer, PoseStack.Pose entry, CallbackInfoReturnable<VertexConsumer> ci) {
		if (provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderTypeUtil.toMaterial(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getCompassFoilBufferDirect", cancellable = true)
	private static void onGetCompassFoilBufferDirect(MultiBufferSource provider, RenderType layer, PoseStack.Pose entry, CallbackInfoReturnable<VertexConsumer> ci) {
		if (provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderTypeUtil.toMaterial(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getFoilBuffer", cancellable = true)
	private static void onGetFoilBuffer(MultiBufferSource vertexConsumers, RenderType layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> ci) {
		if (glint && vertexConsumers instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) vertexConsumers).getConsumer(RenderTypeUtil.toMaterial(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getFoilBufferDirect", cancellable = true)
	private static void onGetFoilBufferDirect(MultiBufferSource provider, RenderType layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> ci) {
		if (glint && provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderTypeUtil.toMaterial(layer, true)));
		}
	}
}
