/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.SpriteExt;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite implements SpriteExt {
	@Shadow protected NativeImage[] mainImage;
	@Shadow void upload(int i, int j, NativeImage[] nativeImages) { }

	private int animationIndex = -1;
	private BooleanSupplier shouldAnimate = () -> true;

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
}
