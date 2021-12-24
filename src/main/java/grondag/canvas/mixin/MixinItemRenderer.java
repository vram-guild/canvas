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

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.apiimpl.rendercontext.CanvasItemRenderContext;
import grondag.canvas.buffer.input.CanvasImmediate;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
	@Shadow private ItemModelShaper itemModelShaper;

	/**
	 * @author grondag
	 * @reason simplicity
	 */
	@Overwrite
	public void render(ItemStack stack, ItemTransforms.TransformType renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, BakedModel model) {
		CanvasItemRenderContext.get().renderItem(itemModelShaper, stack, renderMode, leftHanded, matrices, vertexConsumers, light, overlay, model);
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
