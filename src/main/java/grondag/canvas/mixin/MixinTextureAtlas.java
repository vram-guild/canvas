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

import java.io.File;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import io.vram.frex.impl.texture.TextureAtlasPreparationExt;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.CanvasBlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.CanvasItemRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.mixinterface.TextureAtlasExt;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.texture.CombinedSpriteAnimation;

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
		width = dataExt.frex_atlasWidth();
		height = dataExt.frex_atlasHeight();

		int animationIndex = 0;
		final List<TextureAtlasSprite> sprites = dataExt.frex_sprites();

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

		if (combined != null) {
			if (Configurator.traceTextureLoad) {
				CanvasMod.LOG.info("Beginning initial combined upload for atlas " + location.toString());
			}

			combined.uploadCombined();
		}

		if (Configurator.debugSpriteAtlas) {
			outputAtlasImage();
		}
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
	public void canvas_trackFrameAnimation(int animationIndex) {
		perFrameBits.set(animationIndex);
	}

	@SuppressWarnings("resource")
	@Inject(at = @At("HEAD"), method = "cycleAnimationFrames")
	private void beforeTick(CallbackInfo ci) {
		if (combined != null) {
			combined.reset();
		}

		if (Configurator.disableUnseenSpriteAnimation && (TextureAtlas) (Object) this == TerrainRenderStates.SOLID.texture.spriteIndex().atlas()) {
			animationBits.clear();
			animationBits.or(perFrameBits);
			perFrameBits.clear();

			final var itemBits = CanvasItemRenderContext.get().encoder.animationBits;
			animationBits.or(itemBits);
			itemBits.clear();

			final var blockBits = CanvasBlockRenderContext.get().encoder.animationBits;
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
