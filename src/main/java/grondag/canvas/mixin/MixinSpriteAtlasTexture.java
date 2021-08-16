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

import java.util.BitSet;
import java.util.Map;
import java.util.function.BooleanSupplier;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.mixinterface.SpriteAtlasTextureExt;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.texture.SpriteIndex;

// PERF: Try hacking the texture stitcher comparator to stitch animated sprites first.
// If that works, then animated sprites could be uploaded in one go and would probably
// not need the overhead of tracking animated sprites.

@Mixin(SpriteAtlasTexture.class)
public class MixinSpriteAtlasTexture implements SpriteAtlasTextureExt {
	@Shadow private Identifier id;
	@Shadow private Map<Identifier, Sprite> sprites;
	@Shadow private int maxTextureSize;

	private final BitSet animationBits = new BitSet();

	@Inject(at = @At("RETURN"), method = "upload")
	private void afterUpload(SpriteAtlasTexture.Data input, CallbackInfo ci) {
		int index = 0;
		int animationIndex = 0;
		final ObjectArrayList<Sprite> spriteIndexList = new ObjectArrayList<>();

		for (final Sprite sprite : sprites.values()) {
			spriteIndexList.add(sprite);
			final var spriteExt = (SpriteExt) sprite;
			spriteExt.canvas_id(index++);

			if (sprite.getAnimation() != null) {
				final int instanceIndex = animationIndex;
				final BooleanSupplier getter = () -> animationBits.get(instanceIndex);
				spriteExt.canvas_initializeAnimation(getter, instanceIndex);
				++animationIndex;
			}
		}

		// Safeguard for non-terrain animations added by mods - they will always be animated
		animationBits.set(0, animationIndex);

		SpriteIndex.getOrCreate(id).reset(input, spriteIndexList, (SpriteAtlasTexture) (Object) this);
	}

	@Override
	public int canvas_maxTextureSize() {
		return maxTextureSize;
	}

	@SuppressWarnings("resource")
	@Inject(at = @At("HEAD"), method = "tickAnimatedSprites")
	private void beforeTick(CallbackInfo ci) {
		if (Configurator.disableUnseenSpriteAnimation && (SpriteAtlasTexture) (Object) this == TerrainRenderStates.SOLID.texture.atlasInfo().atlas()) {
			animationBits.clear();

			final var itemBits = ItemRenderContext.get().animationBits;
			animationBits.or(itemBits);
			itemBits.clear();

			final var blockBits = BlockRenderContext.get().animationBits;
			animationBits.or(blockBits);
			blockBits.clear();

			animationBits.or(CanvasWorldRenderer.instance().worldRenderState.terrainAnimationBits);
		}
	}
}
