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

import java.util.Map;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.Sprite.Info;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureStitcher;
import net.minecraft.util.Identifier;

import grondag.canvas.mixinterface.SpriteAtlasTextureExt;
import grondag.canvas.mixinterface.SpriteExt;

public class CanvasSpriteAtlasHandler {
	public final SpriteAtlasTexture atlas;
	public final SpriteAtlasTextureExt atlasExt;
	public final int maxTextureSize;

	public CanvasSpriteAtlasHandler(SpriteAtlasTexture atlas) {
		this.atlas = atlas;
		atlasExt = (SpriteAtlasTextureExt) atlas;
		maxTextureSize = atlasExt.canvas_maxTextureSize();
	}

	public void afterUpload(Map<Identifier, Sprite> sprites) {
		final var stitcher = new TextureStitcher(maxTextureSize, maxTextureSize, 0);

		for (final var sprite : sprites.values()) {
			((SpriteExt) sprite).canvas_handler().afterLoad();

			stitcher.add(new Info(null, maxTextureSize, maxTextureSize, null));
		}
	}
}
