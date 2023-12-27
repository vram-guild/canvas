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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import io.vram.frex.api.rendertype.RenderTypeUtil;

import grondag.canvas.buffer.input.CanvasImmediate;

@Mixin(HumanoidArmorLayer.class)
public class MixinHumanoidArmorLayer {
	private boolean canvas_hasFoil = false;

	@Inject(method = "renderArmorPiece", at = @At("HEAD"))
	void captureFoil(PoseStack poseStack, MultiBufferSource multiBufferSource, LivingEntity livingEntity, EquipmentSlot equipmentSlot, int i, HumanoidModel<?> humanoidModel, CallbackInfo ci) {
		canvas_hasFoil = livingEntity.getItemBySlot(equipmentSlot).hasFoil();
	}

	@Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	VertexConsumer adjustModelGlint(MultiBufferSource bufferSource, RenderType renderType) {
		if (bufferSource instanceof final CanvasImmediate immediate) {
			return immediate.getConsumer(RenderTypeUtil.toMaterial(renderType, canvas_hasFoil));
		} else {
			return bufferSource.getBuffer(renderType);
		}
	}

	@Redirect(method = "renderTrim", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	VertexConsumer adjustTrimGlint(MultiBufferSource bufferSource, RenderType renderType) {
		if (bufferSource instanceof final CanvasImmediate immediate) {
			return immediate.getConsumer(RenderTypeUtil.toMaterial(renderType, canvas_hasFoil));
		} else {
			return bufferSource.getBuffer(renderType);
		}
	}

	@Inject(method = "renderGlint", at = @At("HEAD"), cancellable = true)
	void onRenderGlint(PoseStack poseStack, MultiBufferSource bufferSource, int i, HumanoidModel<?> humanoidModel, CallbackInfo ci) {
		if (bufferSource instanceof CanvasImmediate) {
			ci.cancel();
		}
	}
}
