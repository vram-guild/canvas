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
import grondag.canvas.wip.encoding.WipVertexCollectorImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;

@Mixin(SpriteTexturedVertexConsumer.class)
abstract class MixinSpriteTexturedVertexConsumer {
	// PERF: can we avoid instantiating this damn thing every frame multiple times per texture?
	@Inject(at = @At("RETURN"), method = "<init>*")
	private void on_init(VertexConsumer parent, Sprite sprite, CallbackInfo ci) {
		if (Configurator.enableExperimentalPipeline && parent instanceof WipVertexCollectorImpl) {
			((WipVertexCollectorImpl) parent).sprite(sprite);
		}
	}
}
