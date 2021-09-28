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

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.CombinedAnimationConsumer;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.texture.CombinedSpriteAnimation;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite implements SpriteExt {
	@Shadow protected NativeImage[] mainImage;
	@Shadow void upload(int i, int j, NativeImage[] nativeImages) { }
	@Shadow private TextureAtlasSprite.AnimatedTexture animatedTexture;

	private int canvasId;
	private int animationIndex = -1;
	private BooleanSupplier shouldAnimate = () -> true;

	@Override
	public int canvas_id() {
		return canvasId;
	}

	@Override
	public void canvas_id(int id) {
		canvasId = id;
	}

	@Override
	public NativeImage[] canvas_images() {
		return mainImage;
	}

	@Override
	public void canvas_upload(int i, int j, NativeImage[] images) {
		upload(i, j, images);
	}

	@Override
	public void canvas_initializeAnimation(BooleanSupplier getter, int animationIndex) {
		this.shouldAnimate = getter;
		this.animationIndex = animationIndex;
	}

	@Override
	public boolean canvas_shouldAnimate() {
		return !Configurator.disableUnseenSpriteAnimation || shouldAnimate.getAsBoolean();
	}

	@Override
	public int canvas_animationIndex() {
		return animationIndex;
	}

	@Override
	public void canvas_setCombinedAnimation(CombinedSpriteAnimation combined) {
		@SuppressWarnings("resource")
		final TextureAtlasSprite me = (TextureAtlasSprite) (Object) this;

		if (animatedTexture != null || (me.getX() < combined.width && me.getY() < combined.height)) {
			if (Configurator.traceTextureLoad) {
				CanvasMod.LOG.info("Enabling combined animation upload for sprite " + ((TextureAtlasSprite) (Object) this).getName().toString());
			}

			for (final var img : mainImage) {
				((CombinedAnimationConsumer) (Object) img).canvas_setCombinedAnimation(combined);
			}

			if (animatedTexture != null) {
				((CombinedAnimationConsumer) animatedTexture).canvas_setCombinedAnimation(combined);
			}
		}
	}
}
