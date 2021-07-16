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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.buffer.input.CanvasImmediate;
import grondag.canvas.material.state.RenderLayerHelper;
import grondag.canvas.mixinterface.ItemRendererExt;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer implements ItemRendererExt {
	@Shadow private ItemModels models;
	@Shadow private BuiltinModelItemRenderer builtinModelItemRenderer;

	@Override
	public BuiltinModelItemRenderer canvas_builtinModelItemRenderer() {
		return builtinModelItemRenderer;
	}

	/**
	 * @author grondag
	 * @reason simplicity
	 */
	@Overwrite
	public void renderItem(ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model) {
		ItemRenderContext.get().renderItem(models, stack, renderMode, leftHanded, matrices, vertexConsumers, light, overlay, model);
	}

	@Inject(at = @At("HEAD"), method = "getArmorGlintConsumer", cancellable = true)
	private static void onGetArmorGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> ci) {
		if (glint && provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderLayerHelper.copyFromLayer(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getCompassGlintConsumer", cancellable = true)
	private static void onGetCompassGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, MatrixStack.Entry entry, CallbackInfoReturnable<VertexConsumer> ci) {
		if (provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderLayerHelper.copyFromLayer(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getDirectCompassGlintConsumer", cancellable = true)
	private static void onGetDirectCompassGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, MatrixStack.Entry entry, CallbackInfoReturnable<VertexConsumer> ci) {
		if (provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderLayerHelper.copyFromLayer(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getItemGlintConsumer", cancellable = true)
	private static void onGetItemGlintConsumer(VertexConsumerProvider vertexConsumers, RenderLayer layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> ci) {
		if (glint && vertexConsumers instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) vertexConsumers).getConsumer(RenderLayerHelper.copyFromLayer(layer, true)));
		}
	}

	@Inject(at = @At("HEAD"), method = "getDirectItemGlintConsumer", cancellable = true)
	private static void onGetDirectItemGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, boolean solid, boolean glint, CallbackInfoReturnable<VertexConsumer> ci) {
		if (glint && provider instanceof CanvasImmediate) {
			ci.setReturnValue(((CanvasImmediate) provider).getConsumer(RenderLayerHelper.copyFromLayer(layer, true)));
		}
	}
}
