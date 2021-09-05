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

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureStitcher;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.BlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.ItemRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.mixinterface.SpriteAtlasTextureDataExt;
import grondag.canvas.mixinterface.SpriteAtlasTextureExt;
import grondag.canvas.mixinterface.SpriteExt;
import grondag.canvas.render.world.CanvasWorldRenderer;
import grondag.canvas.texture.CombinedSpriteAnimation;
import grondag.canvas.texture.SpriteIndex;

@Mixin(SpriteAtlasTexture.class)
public abstract class MixinSpriteAtlasTexture extends AbstractTexture implements SpriteAtlasTextureExt {
	@Shadow private Identifier id;
	@Shadow private Map<Identifier, Sprite> sprites;
	@Shadow private int maxTextureSize;
	private int width, height;

	private final BitSet animationBits = new BitSet();

	private int animationMinX = Integer.MAX_VALUE;
	private int animationMinY = Integer.MAX_VALUE;
	private int animationMaxX = Integer.MIN_VALUE;
	private int animationMaxY = Integer.MIN_VALUE;
	private int lodCount = -1;
	private CombinedSpriteAnimation combined = null;

	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/texture/SpriteAtlasTexture;loadSprites(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/texture/TextureStitcher;I)Ljava/util/List;")
	private void onLoadSprites(ResourceManager resourceManager, TextureStitcher textureStitcher, int lodCount, CallbackInfoReturnable<List<Sprite>> ci) {
		this.lodCount = lodCount;
	}

	@Inject(at = @At("HEAD"), method = "upload")
	private void beforeUpload(SpriteAtlasTexture.Data input, CallbackInfo ci) {
		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("Start of pre-upload handling for atlas " + id.toString());
		}

		final var dataExt = (SpriteAtlasTextureDataExt) input;
		width = dataExt.canvas_atlasWidth();
		height = dataExt.canvas_atlasHeight();

		int animationIndex = 0;
		final List<Sprite> sprites = dataExt.canvas_sprites();

		for (final Sprite sprite : sprites) {
			if (sprite.getAnimation() != null) {
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
				CanvasMod.LOG.info("Enabling combined animation for atlas " + id.toString());
				CanvasMod.LOG.info(String.format("Combined dimensions are (%d, %d) to (%d, %d) with LOD count %d", animationMinX, animationMinY, animationMaxX, animationMaxY, lodCount));
			}

			combined = new CombinedSpriteAnimation((SpriteAtlasTexture) (Object) this, animationMinX, animationMinY, animationMaxX, animationMaxY, lodCount);

			for (final Sprite sprite : sprites) {
				((SpriteExt) sprite).canvas_setCombinedAnimation(combined);
			}
		}
	}

	@Inject(at = @At("RETURN"), method = "upload")
	private void afterUpload(SpriteAtlasTexture.Data input, CallbackInfo ci) {
		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("Start of post-upload handling for atlas " + id.toString());
		}

		final ObjectArrayList<Sprite> spriteIndexList = new ObjectArrayList<>();
		int index = 0;

		for (final Sprite sprite : sprites.values()) {
			spriteIndexList.add(sprite);
			final var spriteExt = (SpriteExt) sprite;
			spriteExt.canvas_id(index++);
		}

		if (combined != null) {
			if (Configurator.traceTextureLoad) {
				CanvasMod.LOG.info("Beginning initial combined upload for atlas " + id.toString());
			}

			combined.uploadCombined();
		}

		if (Configurator.debugSpriteAtlas) {
			outputAtlasImage();
		}

		SpriteIndex.getOrCreate(id).reset(input, spriteIndexList, (SpriteAtlasTexture) (Object) this);
	}

	private void outputAtlasImage() {
		RenderSystem.recordRenderCall(() -> {
			if (Configurator.traceTextureLoad) {
				CanvasMod.LOG.info("Capturing atlas image for " + id.toString());
			}

			final NativeImage nativeImage = new NativeImage(width, height, false);
			RenderSystem.bindTexture(this.getGlId());
			nativeImage.loadFromTextureImage(0, true);

			Util.getIoWorkerExecutor().execute(() -> {
				if (Configurator.traceTextureLoad) {
					CanvasMod.LOG.info("Exporting atlas image for " + id.toString());
				}

				try {
					@SuppressWarnings("resource")
					final Path path = MinecraftClient.getInstance().runDirectory.toPath().normalize().resolve("atlas_debug");
					final File atlasDir = path.toFile();

					if (!atlasDir.exists()) {
						atlasDir.mkdir();
						CanvasMod.LOG.info("Created atlas debug output folder" + atlasDir.toString());
					}

					final File file = new File(atlasDir.getAbsolutePath() + File.separator + StringUtils.replaceAll(id.toString(), "[_/]", "_"));
					nativeImage.writeFile(file);
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
		return maxTextureSize;
	}

	@SuppressWarnings("resource")
	@Inject(at = @At("HEAD"), method = "tickAnimatedSprites")
	private void beforeTick(CallbackInfo ci) {
		if (combined != null) {
			combined.reset();
		}

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

	@Inject(at = @At("RETURN"), method = "tickAnimatedSprites")
	private void afterTick(CallbackInfo ci) {
		if (combined != null) {
			combined.uploadCombined();
		}
	}
}
