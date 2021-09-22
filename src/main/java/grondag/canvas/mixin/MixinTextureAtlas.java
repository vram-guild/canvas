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

import java.io.File;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.mixinterface.TextureAtlasExt;
import grondag.canvas.mixinterface.TextureAtlasPreparationExt;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.texture.CombinedSpriteAnimation;
import grondag.canvas.texture.SpriteIndex;

@Mixin(TextureAtlas.class)
public abstract class MixinTextureAtlas extends AbstractTexture implements TextureAtlasExt {
	@Shadow private ResourceLocation location;
	@Shadow private Map<ResourceLocation, TextureAtlasSprite> texturesByName;
	@Shadow private int maxSupportedTextureSize;
	private int width, height;

	private final BitSet animationBits = new BitSet();
	private final BitSet perFrameBits = new BitSet();

	private int animationMinX = Integer.MAX_VALUE;
	private int animationMinY = Integer.MAX_VALUE;
	private int animationMaxX = Integer.MIN_VALUE;
	private int animationMaxY = Integer.MIN_VALUE;
	private int lodCount = -1;
	private CombinedSpriteAnimation combined = null;

	@Inject(at = @At("HEAD"), method = "getLoadedSprites")
	private void onGetLoadedSprites(ResourceManager resourceManager, Stitcher textureStitcher, int lodCount, CallbackInfoReturnable<List<TextureAtlasSprite>> ci) {
		this.lodCount = lodCount;
	}

	@Inject(at = @At("HEAD"), method = "reload")
	private void beforeReload(TextureAtlas.Preparations input, CallbackInfo ci) {
		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("Start of pre-upload handling for atlas " + location.toString());
		}

		final var dataExt = (TextureAtlasPreparationExt) input;
		width = dataExt.canvas_atlasWidth();
		height = dataExt.canvas_atlasHeight();

		int animationIndex = 0;
		final List<TextureAtlasSprite> sprites = dataExt.canvas_sprites();

		for (final TextureAtlasSprite sprite : sprites) {
			if (sprite.getAnimationTicker() != null) {
				final var spriteExt = (SpriteExt) sprite;
				final int instanceIndex = animationIndex;
				final BooleanSupplier getter = () -> animationBits.get(instanceIndex);
				spriteExt.canvas_initializeAnimation(getter, instanceIndex);
				++animationIndex;
				animationMinX = Math.min(animationMinX, sprite.getX());
				animationMinY = Math.min(animationMinY, sprite.getY());
				animationMaxX = Math.max(animationMaxX, sprite.getX() + sprite.getWidth());
				animationMaxY = Math.max(animationMaxY, sprite.getY() + sprite.getHeight());
			}
		}

		// Safeguard for non-terrain animations added by mods - they will always be animated
		animationBits.set(0, animationIndex);

		if (Configurator.groupAnimatedSprites && animationMinX != Integer.MAX_VALUE) {
			if (Configurator.traceTextureLoad) {
				CanvasMod.LOG.info("Enabling combined animation for atlas " + location.toString());
				CanvasMod.LOG.info(String.format("Combined dimensions are (%d, %d) to (%d, %d) with LOD count %d", animationMinX, animationMinY, animationMaxX, animationMaxY, lodCount));
			}

			combined = new CombinedSpriteAnimation((TextureAtlas) (Object) this, animationMinX, animationMinY, animationMaxX, animationMaxY, lodCount);

			for (final TextureAtlasSprite sprite : sprites) {
				((SpriteExt) sprite).canvas_setCombinedAnimation(combined);
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "reload")
	private void afterReload(TextureAtlas.Preparations input, CallbackInfo ci) {
		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("Start of post-upload handling for atlas " + location.toString());
		}

		final ObjectArrayList<TextureAtlasSprite> spriteIndexList = new ObjectArrayList<>();
		int index = 0;

		for (final TextureAtlasSprite sprite : texturesByName.values()) {
			spriteIndexList.add(sprite);
			final var spriteExt = (SpriteExt) sprite;
			spriteExt.canvas_id(index++);
		}

		if (combined != null) {
			if (Configurator.traceTextureLoad) {
				CanvasMod.LOG.info("Beginning initial combined upload for atlas " + location.toString());
			}

			combined.uploadCombined();
		}

		if (Configurator.debugSpriteAtlas) {
			outputAtlasImage();
		}

		SpriteIndex.getOrCreate(location).reset(input, spriteIndexList, (TextureAtlas) (Object) this);
	}

	private void outputAtlasImage() {
		RenderSystem.recordRenderCall(() -> {
			if (Configurator.traceTextureLoad) {
				CanvasMod.LOG.info("Capturing atlas image for " + location.toString());
			}

			final NativeImage nativeImage = new NativeImage(width, height, false);
			RenderSystem.bindTexture(this.getId());
			nativeImage.downloadTexture(0, true);

			Util.ioPool().execute(() -> {
				if (Configurator.traceTextureLoad) {
					CanvasMod.LOG.info("Exporting atlas image for " + location.toString());
				}

				try {
					@SuppressWarnings("resource")
					final Path path = Minecraft.getInstance().gameDirectory.toPath().normalize().resolve("atlas_debug");
					final File atlasDir = path.toFile();

					if (!atlasDir.exists()) {
						atlasDir.mkdir();
						CanvasMod.LOG.info("Created atlas debug output folder" + atlasDir.toString());
					}

					final File file = new File(atlasDir.getAbsolutePath() + File.separator + StringUtils.replaceAll(location.toString(), "[_/]", "_"));
					nativeImage.writeToFile(file);
				} catch (final Exception e) {
					CanvasMod.LOG.warn("Couldn't save atlas image", e);
				} finally {
					nativeImage.close();
				}
			});
		});
	}

	@Override
	public int canvas_maxTextureSize() {
		return maxSupportedTextureSize;
	}

	@Override
	public IntConsumer canvas_frameAnimationConsumer() {
		return i -> perFrameBits.set(i);
	}

	@SuppressWarnings("resource")
	@Inject(at = @At("HEAD"), method = "cycleAnimationFrames")
	private void beforeTick(CallbackInfo ci) {
		if (combined != null) {
			combined.reset();
		}

		// WIP: this first check seems like it can't be right
		if (Configurator.disableUnseenSpriteAnimation && (TextureAtlas) (Object) this == TerrainRenderStates.SOLID.texture.atlasInfo().atlas()) {
			animationBits.clear();
			animationBits.or(perFrameBits);
			perFrameBits.clear();

			final var itemBits = ItemRenderContext.get().animationBits;
			animationBits.or(itemBits);
			itemBits.clear();

			final var blockBits = BlockRenderContext.get().animationBits;
			animationBits.or(blockBits);
			blockBits.clear();

			animationBits.or(CanvasWorldRenderer.instance().worldRenderState.terrainAnimationBits);
		}
	}

	@Inject(at = @At("RETURN"), method = "cycleAnimationFrames")
	private void afterTick(CallbackInfo ci) {
		if (combined != null) {
			combined.uploadCombined();
		}
	}
}
