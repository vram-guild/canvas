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

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.SpriteContents;

import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.SpriteContentsExt;

@Mixin(SpriteContents.class)
public abstract class MixinSpriteContents implements SpriteContentsExt {
	@Shadow NativeImage[] byMipLevel;
	private BooleanSupplier shouldAnimate = () -> true;
	private int animationIndex = -1;

	@Shadow abstract void upload(int i, int j, int k, int l, NativeImage[] nativeImages);

	@Shadow @Final @Nullable private SpriteContents.AnimatedTexture animatedTexture;

	@Override
	public NativeImage[] canvas_images() {
		return byMipLevel;
	}

	@Override
	public void canvas_upload(int x, int y, int xOffset, int yOffset, NativeImage[] images) {
		upload(x, y, xOffset, yOffset, images);
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
	public boolean canvas_isAnimated() {
		return animatedTexture != null;
	}
}
