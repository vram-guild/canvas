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
import java.util.List;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import com.google.common.collect.Lists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.AnimationResourceMetadataExt;

@Mixin(Stitcher.class)
public class MixinTextureStitcher {
	@Shadow private List<Stitcher.Region> slots = Lists.newArrayListWithCapacity(256);
	@Shadow private int width;
	@Shadow private int height;
	@Shadow private int maxWidth;
	@Shadow private int maxHeight;
	@Shadow private int mipLevel;

	@Shadow static int applyMipLevel(int i, int j) {
		return 0;
	}

	private int maxHolderWidth, maxHolderHeight;

	/**
	 * Cause animated sprites to be stiched first so we can upload them in one call per LOD.
	 */
	private static final Comparator<Stitcher.Holder> ANIMATION_COMPARATOR = Comparator.comparing((Stitcher.Holder holder) -> {
		return ((AnimationResourceMetadataExt) holder.spriteInfo.metadata).canvas_willAnimate(holder.width, holder.height) ? -1 : 1;
	}).thenComparing((holder) -> {
		return -holder.height;
	}).thenComparing((holder) -> {
		return -holder.width;
	}).thenComparing((holder) -> {
		return holder.spriteInfo.name();
	});

	@ModifyArg(method = "stitch", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"), index = 0)
	private Comparator<Stitcher.Holder> onSort(Comparator<Stitcher.Holder> var) {
		return Configurator.groupAnimatedSprites ? ANIMATION_COMPARATOR : var;
	}

	/**
	 * Capture largest sprite size.
	 */
	@Inject(at = @At("HEAD"), method = "add")
	private void onAdd(TextureAtlasSprite.Info info, CallbackInfo ci) {
		maxHolderWidth = Math.max(maxHolderWidth, applyMipLevel(info.width(), mipLevel));
		maxHolderHeight = Math.max(maxHolderHeight, applyMipLevel(info.height(), mipLevel));
	}

	/**
	 * While we are adding animated sprites, ensure the size of new empty slots
	 * are as big as the largest sprite to prevent breaking assumptions of the
	 * original design.
	 */
	@Inject(at = @At("HEAD"), method = "growAndFit", cancellable = true)
	private void onGrowAndFit(Stitcher.Holder holder, CallbackInfoReturnable<Boolean> ci) {
		final boolean animated = Configurator.groupAnimatedSprites && ((AnimationResourceMetadataExt) holder.spriteInfo.metadata).canvas_willAnimate(holder.width, holder.height);

		if (animated) {
			final int slotWidth = animated ? maxHolderWidth : holder.width;
			final int slotHeight = animated ? maxHolderHeight : holder.height;
			final int curEffectiveWidth = Mth.smallestEncompassingPowerOfTwo(width);
			final int curEffectiveHeight = Mth.smallestEncompassingPowerOfTwo(height);
			final int newEffectiveWidth = Mth.smallestEncompassingPowerOfTwo(width + slotWidth);
			final int newEffectiveHeight = Mth.smallestEncompassingPowerOfTwo(height + slotHeight);
			final boolean canFitWidth = newEffectiveWidth <= maxWidth;
			final boolean canFitHeight = newEffectiveHeight <= maxHeight;

			if (!(canFitWidth || canFitHeight)) {
				ci.setReturnValue(false);
			} else {
				final boolean wouldGrowWidth = canFitWidth && curEffectiveWidth != newEffectiveWidth;
				final boolean wouldGrowHeight = canFitHeight && curEffectiveHeight != newEffectiveHeight;
				final boolean addWidth;

				if (wouldGrowWidth ^ wouldGrowHeight) {
					addWidth = wouldGrowWidth;
				} else {
					addWidth = canFitWidth && curEffectiveWidth <= curEffectiveHeight;
				}

				final Stitcher.Region slot;

				if (addWidth) {
					if (this.height == 0) {
						this.height = slotHeight;
					}

					slot = new Stitcher.Region(this.width, 0, slotWidth, this.height);
					this.width += slotWidth;
				} else {
					slot = new Stitcher.Region(0, this.height, this.width, slotHeight);
					this.height += slotHeight;
				}

				slot.add(holder);
				slots.add(slot);

				ci.setReturnValue(true);
			}
		}
	}
}
