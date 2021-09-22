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

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import grondag.canvas.mixinterface.TextureAtlasPreparationExt;

@Mixin(TextureAtlas.Preparations.class)
public class MixinTextureAtlasPreparation implements TextureAtlasPreparationExt {
	@Shadow int width;
	@Shadow int height;
	@Shadow List<TextureAtlasSprite> regions;

	@Override
	public int canvas_atlasWidth() {
		return width;
	}

	@Override
	public int canvas_atlasHeight() {
		return height;
	}

	@Override
	public List<TextureAtlasSprite> canvas_sprites() {
		return regions;
	}
}
