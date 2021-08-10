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

package grondag.canvas.texture;

import net.minecraft.client.texture.Sprite;

import grondag.canvas.mixinterface.SpriteExt;

public class CanvasSpriteHandler {
	public final Sprite sprite;
	public final SpriteExt spriteExt;

	public CanvasSpriteHandler(Sprite sprite) {
		this.sprite = sprite;
		spriteExt = (SpriteExt) sprite;
	}

	public void afterLoad() {
		final var animation = sprite.getAnimation();

		if (animation != null) {
			System.out.println(String.format("sprite w: %d h: %d   image w: %d h: %d",
				sprite.getWidth(),
				sprite.getHeight(),
				spriteExt.canvas_images()[0].getWidth(),
				spriteExt.canvas_images()[0].getHeight()
			));
		}
	}
}
