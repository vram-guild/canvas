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

package grondag.canvas.mixin;

import grondag.canvas.Configurator;
import grondag.canvas.shader.wip.encoding.PaintingHelper;
import grondag.canvas.shader.wip.encoding.WipVertexCollectorImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.painting.PaintingEntity;

@Mixin(PaintingEntityRenderer.class)
abstract class MixinPaintingEntityRenderer {
	@Inject(at = @At("HEAD"), method = "method_4074", cancellable = true)
	private void on_method_4074(MatrixStack matrixStack, VertexConsumer vertexConsumer, PaintingEntity paintingEntity, int width, int height, Sprite paintSprite, Sprite frameSprite, CallbackInfo ci) {
		if (Configurator.enableExperimentalPipeline && vertexConsumer instanceof WipVertexCollectorImpl) {
			PaintingHelper.bufferPainting(matrixStack, (WipVertexCollectorImpl) vertexConsumer, paintingEntity, width, height, paintSprite, frameSprite);
			ci.cancel();
		}
	}
}
