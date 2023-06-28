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

package grondag.canvas.pipeline.config;

import blue.endless.jankson.JsonObject;

import grondag.canvas.light.color.LightDataTexture;
import grondag.canvas.pipeline.GlSymbolLookup;
import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class LightVolumeConfig extends AbstractConfig {
	public final NamedDependency<ImageConfig> lightImage;

	protected LightVolumeConfig(ConfigContext ctx, JsonObject config) {
		super(ctx);
		lightImage = ctx.images.dependOn(ctx.dynamic.getString(config, "lightImage"));
	}

	@Override
	public boolean validate() {
		final var image = lightImage.value();

		if (image != null) {
			boolean valid = image.validate();

			valid &= assertAndWarn(image.target == LightDataTexture.Format.target, "Invalid pipeline config for image %s. Light data image needs to target %s", image.name, GlSymbolLookup.reverseLookup(LightDataTexture.Format.target));
			valid &= assertAndWarn(image.internalFormat == LightDataTexture.Format.internalFormat, "Invalid pipeline config for image %s. Light data image needs to have internal format of  %s", image.name, GlSymbolLookup.reverseLookup(LightDataTexture.Format.internalFormat));
			valid &= assertAndWarn(image.pixelFormat == LightDataTexture.Format.pixelFormat, "Invalid pipeline config for image %s. Light data image needs to have pixel format of  %s", image.name, GlSymbolLookup.reverseLookup(LightDataTexture.Format.pixelFormat));
			valid &= assertAndWarn(image.pixelDataType == LightDataTexture.Format.pixelDataType, "Invalid pipeline config for image %s. Light data image needs to have pixel data type of  %s", image.name, GlSymbolLookup.reverseLookup(LightDataTexture.Format.pixelDataType));
			valid &= assertAndWarn(image.width > 0 && image.height > 0 && image.depth > 0, "Invalid pipeline config for image %s. Light data image needs to have non-zero width, height, and depth", image.name);
			valid &= assertAndWarn(image.width % 16 == 0 && image.height % 16 == 0 && image.depth % 16 == 0, "Invalid pipeline config for image %s. Light data image needs to have width, height, and depth that are multiples of 16", image.name);

			return valid;
		}

		if (lightImage.name == null) {
			return assertAndWarn(false, "Invalid pipeline light volume config. Light image is unspecified.");
		} else {
			return assertAndWarn(false, "Invalid pipeline light volume config. Image %s doesn't exist", lightImage.name);
		}
	}
}
