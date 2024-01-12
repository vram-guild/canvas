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

package grondag.canvas.pipeline;

import static net.minecraft.client.renderer.entity.ItemRenderer.ENCHANTED_GLINT_ITEM;

import java.util.function.IntSupplier;

import org.lwjgl.opengl.GL46;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.light.color.LightDataManager;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class ProgramTextureData {
	public final IntSupplier[] texIds;
	public final int[] texTargets;

	public ProgramTextureData(NamedDependency<ImageConfig>[] samplerImages) {
		texIds = new IntSupplier[samplerImages.length];
		texTargets = new int[samplerImages.length];

		for (int i = 0; i < samplerImages.length; ++i) {
			final String imageName = samplerImages[i].name;

			IntSupplier imageBind = () -> 0;
			int bindTarget = GL46.GL_TEXTURE_2D;

			// TODO: use a registry if there is more of these
			if (imageName.equals("frex:textures/auto/colored_lights")) {
				imageBind = LightDataManager::texId;
			} else if (imageName.contains(":")) {
				final AbstractTexture tex = tryLoadResourceTexture(new ResourceLocation(imageName));

				if (tex != null) {
					final int id = tex.getId();
					imageBind = () -> id;
				}
			} else {
				final Image img = Pipeline.getImage(imageName);

				if (img != null) {
					final int id = img.glId();
					imageBind = () -> id;
					bindTarget = img.config.target;
				}
			}

			texIds[i] = imageBind;
			texTargets[i] = bindTarget;
		}
	}

	private static AbstractTexture tryLoadResourceTexture(ResourceLocation identifier) {
		final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

		// backwards compatibility
		if (identifier.equals(OLD_GLINT_TEXTURE) && textureManager.getTexture(identifier, null) == null) {
			return textureManager.getTexture(ENCHANTED_GLINT_ITEM);
		}

		return textureManager.getTexture(identifier);
	}

	private static final ResourceLocation OLD_GLINT_TEXTURE = new ResourceLocation("minecraft:textures/misc/enchanted_item_glint.png");
}
