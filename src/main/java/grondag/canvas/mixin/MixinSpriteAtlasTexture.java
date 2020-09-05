/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.mixin;

import grondag.canvas.mixinterface.SpriteAtlasTextureExt;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.texture.SpriteInfoTexture;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture implements SpriteAtlasTextureExt {
	private final ObjectArrayList<Sprite> spriteIndex = new ObjectArrayList<>();
	@Shadow
	private Identifier id;
	@Shadow
	private Map<Identifier, Sprite> sprites;

	@Override
	public Map<Identifier, Sprite> canvas_spriteMap() {
		return sprites;
	}

	@Override
	public ObjectArrayList<Sprite> canvas_spriteIndex() {
		return spriteIndex;
	}

	@Inject(at = @At("RETURN"), method = "upload")
	private void afterUpload(SpriteAtlasTexture.Data input, CallbackInfo info) {
		if (id.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEX)) {
			SpriteInfoTexture.reset(input);
			int index = 0;

			for (final Sprite sprite : sprites.values()) {
				spriteIndex.add(sprite);
				((SpriteExt) sprite).canvas_id(index++);
			}
		}
	}
}
