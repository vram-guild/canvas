/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
