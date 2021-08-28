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

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureStitcher;

import grondag.canvas.mixinterface.AnimationResourceMetadataExt;
import grondag.canvas.texture.CombinedSpriteAnimation;

@Mixin(TextureStitcher.class)
public class MixinTextureStitcher {
	// Cause animated sprites to be stiched first so we can upload them in one call per LOD
	private static final Comparator<TextureStitcher.Holder> ANIMATION_COMPARATOR = Comparator.comparing((TextureStitcher.Holder holder) -> {
		return ((AnimationResourceMetadataExt) holder.sprite.animationData).canvas_willAnimate(holder.width, holder.height) ? -1 : 1;
	}).thenComparing((holder) -> {
		return -holder.height;
	}).thenComparing((holder) -> {
		return -holder.width;
	}).thenComparing((holder) -> {
		return holder.sprite.getId();
	});

	@ModifyArg(method = "stitch", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"), index = 0)
	private Comparator<TextureStitcher.Holder> onSort(Comparator<TextureStitcher.Holder> var) {
		return ANIMATION_COMPARATOR;
	}

	@Inject(at = @At("HEAD"), method = "add")
	private void onAdd(Sprite.Info info, CallbackInfo ci) {
		// WIP: remove
		if (info.getId().getPath().contains("sea_lantern")) {
			CombinedSpriteAnimation.boop();
		}
	}
}
