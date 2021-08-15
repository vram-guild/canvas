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

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;

import grondag.canvas.mixinterface.SpriteExt;

@Mixin(Sprite.class)
public class MixinSprite implements SpriteExt {
	@Shadow protected NativeImage[] images;
	@Shadow void upload(int i, int j, NativeImage[] nativeImages) { }

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
		return images;
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
		return shouldAnimate.getAsBoolean();
	}

	@Override
	public int canvas_animationIndex() {
		return animationIndex;
	}
}
