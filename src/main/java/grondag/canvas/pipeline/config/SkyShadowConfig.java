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

package grondag.canvas.pipeline.config;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedDependency;
import grondag.canvas.varia.GFX;
import net.minecraft.resources.ResourceLocation;

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
		framebuffer = ctx.frameBuffers.dependOn(config, "framebuffer");
		vertexSource = JanksonHelper.asIdentifier(ObjectUtils.defaultIfNull(config.get("vertexSource"), config.get("vertexShader")));
		fragmentSource = JanksonHelper.asIdentifier(ObjectUtils.defaultIfNull(config.get("fragmentSource"), config.get("fragmentShader")));
		allowEntities = config.getBoolean("allowEntities", true);
		allowParticles = config.getBoolean("allowParticles", true);
		supportForwardRender = config.getBoolean("supportForwardRender", true);
		offsetSlopeFactor = config.getFloat("offsetSlopeFactor", DEFAULT_SHADOW_SLOPE_FACTOR);
		offsetBiasUnits = config.getFloat("offsetBiasUnits", DEFAULT_SHADOW_BIAS_UNITS);
		samplerNames = readerSamplerNames(ctx, config, "shy shadows");

		final JsonArray radii = JanksonHelper.getJsonArrayOrNull(config, "cascadeRadius", "Invalid pipeline skyShadow config: cascadeRadius must be an array.");

		if (radii != null) {
			if (radii.size() != 3) {
				CanvasMod.LOG.warn("Invalid pipeline skyShadow config: cascadeRadius array must have length 3.");
			}

			for (int i = 0; i < 3; ++i) {
				final int r = radii.getInt(i, -1);

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
