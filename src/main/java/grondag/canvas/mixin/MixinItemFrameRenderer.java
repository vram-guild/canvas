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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.state.BlockState;

import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;

@Mixin(ItemFrameRenderer.class)
public abstract class MixinItemFrameRenderer {
	@Redirect(method = "Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;renderModel(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/resources/model/BakedModel;FFFII)V"))
	private void onRender(
		// these are the redirect params
		ModelBlockRenderer renderer,
		PoseStack.Pose entry,
		VertexConsumer vertexConsumer,
		@Nullable BlockState blockState,
		BakedModel bakedModel,
		float green, float red, float blue, int light, int overlay,
		// these are the locals
		ItemFrame itemFrameEntity,
		float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i
	) {
		EntityBlockRenderContext.get().renderItemFrame(renderer, bakedModel, matrixStack, vertexConsumerProvider, OverlayTexture.NO_OVERLAY, light, itemFrameEntity);
	}
}
