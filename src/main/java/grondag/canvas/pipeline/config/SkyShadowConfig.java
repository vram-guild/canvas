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

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedDependency;
import grondag.canvas.varia.GFX;

public class SkyShadowConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> framebuffer;
	public final boolean allowEntities;
	public final boolean allowParticles;
	public final boolean supportForwardRender;
	public final ResourceLocation vertexSource;
	public final ResourceLocation fragmentSource;
	public final float offsetSlopeFactor;
	public final float offsetBiasUnits;
	public final int[] cascadeRadii = {32, 16, 8};
	public final String[] samplerNames;

	SkyShadowConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		framebuffer = ctx.frameBuffers.dependOn(ctx.dynamic.getString(config, "framebuffer"));
		vertexSource = ResourceLocation.tryParse(ctx.dynamic.getString(config, "vertexSource", ctx.dynamic.getString(config, "vertexShader")));
		fragmentSource = ResourceLocation.tryParse(ctx.dynamic.getString(config, "fragmentSource", ctx.dynamic.getString(config, "fragmentShader")));
		allowEntities = ctx.dynamic.getBoolean(config, "allowEntities", true);
		allowParticles = ctx.dynamic.getBoolean(config, "allowParticles", true);
		supportForwardRender = ctx.dynamic.getBoolean(config, "supportForwardRender", true);
		offsetSlopeFactor = ctx.dynamic.getFloat(config, "offsetSlopeFactor", DEFAULT_SHADOW_SLOPE_FACTOR);
		offsetBiasUnits = ctx.dynamic.getFloat(config, "offsetBiasUnits", DEFAULT_SHADOW_BIAS_UNITS);
		samplerNames = readerSamplerNames(ctx, config, "shy shadows");

		final JsonArray radii = JanksonHelper.getJsonArrayOrNull(config, "cascadeRadius", "Invalid pipeline skyShadow config: cascadeRadius must be an array.");

		if (radii != null) {
			if (radii.size() != 3) {
				CanvasMod.LOG.warn("Invalid pipeline skyShadow config: cascadeRadius array must have length 3.");
			}

			for (int i = 0; i < 3; ++i) {
				final int r = ctx.dynamic.getInt(radii.get(i), -1);

				if (r <= 0) {
					CanvasMod.LOG.warn("Invalid pipeline skyShadow config: cascadeRadius array must contain integers > 0.");
					break;
				}

				cascadeRadii[i] = r;
			}
		}
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= framebuffer.validate("Invalid pipeline config - skyShadows framebuffer missing or invalid.");
		valid &= assertAndWarn(vertexSource != null, "Invalid pipeline config - skyShadows 'vertexSource' missing or invalid.");
		valid &= assertAndWarn(fragmentSource != null, "Invalid pipeline config - skyShadows 'fragmentSource' missing or invalid.");

		if (valid) {
			valid &= assertAndWarn(framebuffer.value().depthAttachment.image.value().target == GFX.GL_TEXTURE_2D_ARRAY,
					"Invalid pipeline config - skyShadows depth image must be a 2D array texture.");
		}

		return valid;
	}

	public static final float DEFAULT_SHADOW_BIAS_UNITS = 4.0f;
	public static final float DEFAULT_SHADOW_SLOPE_FACTOR = 1.1f;
}
