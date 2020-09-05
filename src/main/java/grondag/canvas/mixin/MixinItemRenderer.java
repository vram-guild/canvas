package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;

import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
	@Shadow private ItemModels models;

	@Redirect(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;renderBakedItemModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/item/ItemStack;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;)V"))
	private void onRenderItem(ItemRenderer self, BakedModel model, ItemStack stack, int light, int overlay, MatrixStack matrices, VertexConsumer modelConsumer, ItemStack stack2, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light2, int overlay2, BakedModel model2) {
		ItemRenderContext.get().renderModel(stack2, renderMode, leftHanded, matrixStack, vertexConsumers, modelConsumer, light2, overlay2, (FabricBakedModel) model2);
	}
}
