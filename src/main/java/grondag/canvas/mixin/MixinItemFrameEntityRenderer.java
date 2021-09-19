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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import grondag.canvas.apiimpl.rendercontext.EntityBlockRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ItemFrameRenderer.class)
public abstract class MixinItemFrameEntityRenderer {
	@Shadow private static ModelResourceLocation NORMAL_FRAME;
	@Shadow private static ModelResourceLocation MAP_FRAME;
	@Shadow private Minecraft client;
	@Shadow private ItemRenderer itemRenderer;

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/BlockModelRenderer;render(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/block/BlockState;Lnet/minecraft/client/render/model/BakedModel;FFFII)V"))
	private void onRender(ModelBlockRenderer renderer, PoseStack.Pose entry, VertexConsumer vertexConsumer, @Nullable BlockState blockState, BakedModel bakedModel, float green, float red, float blue, int light, int overlay,
			ItemFrame itemFrameEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i) {
		EntityBlockRenderContext.get().renderItemFrame(renderer, bakedModel, matrixStack, vertexConsumerProvider, OverlayTexture.NO_OVERLAY, light, itemFrameEntity);
	}
}
