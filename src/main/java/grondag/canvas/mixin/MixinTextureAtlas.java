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
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.apiimpl.rendercontext.CanvasBlockRenderContext;
import grondag.canvas.apiimpl.rendercontext.CanvasItemRenderContext;
import grondag.canvas.config.Configurator;
import grondag.canvas.material.state.TerrainRenderStates;
import grondag.canvas.mixinterface.SpriteContentsExt;
import grondag.canvas.mixinterface.TextureAtlasExt;
import grondag.canvas.render.world.CanvasWorldRenderer;

@Mixin(TextureAtlas.class)
public abstract class MixinTextureAtlas extends AbstractTexture implements TextureAtlasExt {
	@Shadow private ResourceLocation location;
	private int width, height;

	private final BitSet animationBits = new BitSet();
	private final BitSet perFrameBits = new BitSet();

	@Inject(at = @At("HEAD"), method = "upload")
	private void beforeReload(SpriteLoader.Preparations preparations, CallbackInfo ci) {
		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("Start of reload for atlas " + location.toString());
		}

		width = preparations.width();
		height = preparations.height();

		int animationIndex = 0;

		for (final TextureAtlasSprite sprite : preparations.regions().values()) {
			final var spriteExt = (SpriteContentsExt) sprite.contents();

			if (spriteExt.canvas_isAnimated()) {
				final int instanceIndex = animationIndex;
				final BooleanSupplier getter = () -> animationBits.get(instanceIndex);
				spriteExt.canvas_initializeAnimation(getter, instanceIndex);
				++animationIndex;
			}
		}

		// Safeguard for non-terrain animations added by mods - they will always be animated
		animationBits.set(0, animationIndex);
	}

	@Inject(at = @At("RETURN"), method = "upload")
	private void afterReload(SpriteLoader.Preparations preparations, CallbackInfo ci) {
		if (Configurator.debugSpriteAtlas) {
			outputAtlasImage();
		}

		if (Configurator.traceTextureLoad) {
			CanvasMod.LOG.info("End of reload for atlas " + location.toString());
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

					final File file = new File(atlasDir.getAbsolutePath() + File.separator + location.toString().replaceAll("[_/:]", "_"));
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
	public void canvas_trackFrameAnimation(int animationIndex) {
		perFrameBits.set(animationIndex);
	}

	@SuppressWarnings("resource")
	@Inject(at = @At("HEAD"), method = "cycleAnimationFrames")
	private void beforeTick(CallbackInfo ci) {
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
}
