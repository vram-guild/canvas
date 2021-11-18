/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.pipeline;

import org.lwjgl.opengl.GL46;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class ProgramTextureData {
	public final int[] texIds;
	public final int[] texTargets;

	public ProgramTextureData(NamedDependency<ImageConfig>[] samplerImages) {
		texIds = new int[samplerImages.length];
		texTargets = new int[samplerImages.length];

		for (int i = 0; i < samplerImages.length; ++i) {
			final String imageName = samplerImages[i].name;

			int imageBind = 0;
			int bindTarget = GL46.GL_TEXTURE_2D;

			if (imageName.contains(":")) {
				final AbstractTexture tex = tryLoadResourceTexture(new ResourceLocation(imageName));

				if (tex != null) {
					imageBind = tex.getId();
				}
			} else {
				final Image img = Pipeline.getImage(imageName);

				if (img != null) {
					imageBind = img.glId();
					bindTarget = img.config.target;
				}
			}

			texIds[i] = imageBind;
			texTargets[i] = bindTarget;
		}
	}

	private static AbstractTexture tryLoadResourceTexture(ResourceLocation identifier) {
		final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		final AbstractTexture existingTexture = textureManager.getTexture(identifier);

		if (existingTexture != null) {
			return existingTexture;
		} else {
			// NB: `registerTexture` will replace the texture with MissingSprite if not found. This is useful for
			//     pipeline developers.
			//     Additionally, TextureManager will handle removing missing textures on resource reload.
			final SimpleTexture resourceTexture = new SimpleTexture(identifier);
			textureManager.register(identifier, resourceTexture);
			return textureManager.getTexture(identifier);
		}
	}
}
