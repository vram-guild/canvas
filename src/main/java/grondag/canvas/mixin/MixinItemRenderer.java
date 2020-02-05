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


package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

import grondag.canvas.apiimpl.rendercontext.CanvasQuadHandler;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext.VanillaQuadHandler;
import grondag.canvas.mixinterface.AccessItemRenderer;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer implements AccessItemRenderer {
	@Shadow
	protected abstract void renderBakedItemModel(BakedModel model, ItemStack stack, int light, int overlay, MatrixStack matrixStack, VertexConsumer buffer);

	@Shadow
	protected ItemColors colorMap;

	private final VanillaQuadHandler vanillaHandler = new CanvasQuadHandler(this);

	private final ThreadLocal<ItemRenderContext> CONTEXTS = ThreadLocal.withInitial(() -> new ItemRenderContext(colorMap));

	@Inject(at = @At("HEAD"), method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V", cancellable = true)
	public void hook_method_23179(ItemStack stack, ModelTransformation.Mode transformMode, boolean invert, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, int overlay, BakedModel model, CallbackInfo ci) {
		final FabricBakedModel fabricModel = (FabricBakedModel) model;

		if (!(stack.isEmpty() || fabricModel.isVanillaAdapter())) {
			CONTEXTS.get().renderModel(stack, transformMode, invert, matrixStack, vertexConsumerProvider, light, overlay, fabricModel, vanillaHandler);
			ci.cancel();
		}
	}

	@Override
	public void canvas_renderBakedItemModel(BakedModel model, ItemStack stack, int light, int overlay, MatrixStack matrixStack, VertexConsumer buffer) {
		renderBakedItemModel(model, stack, light, overlay, matrixStack, buffer);
	}
}
