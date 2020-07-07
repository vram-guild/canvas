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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;

import grondag.canvas.Configurator;
import grondag.canvas.render.CanvasWorldRenderer;

/**
 * @reason Avoid allocation overhead and use more accurate test.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer<T extends Entity> {
	@Inject(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getVisibilityBoundingBox()Lnet/minecraft/util/math/Box;"), cancellable = true)
	private void onShouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> ci) {
		if (Configurator.cullEntityRender) {
			ci.setReturnValue(CanvasWorldRenderer.instance().isEntityVisible(entity));
		}
	}
}
