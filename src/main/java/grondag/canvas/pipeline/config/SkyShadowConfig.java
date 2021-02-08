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

import blue.endless.jankson.JsonObject;
import org.lwjgl.opengl.GL46;

import net.minecraft.util.Identifier;

import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class SkyShadowConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> framebuffer;
	public final boolean allowEntities;
	public final boolean allowParticles;
	public final boolean supportForwardRender;
	public final Identifier vertexShader;
	public final Identifier fragmentShader;
	public final float offsetSlopeFactor;
	public final float offsetBiasUnits;

	SkyShadowConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		framebuffer = ctx.frameBuffers.dependOn(config, "framebuffer");
		vertexShader = JanksonHelper.asIdentifier(config.get("vertexShader"));
		fragmentShader = JanksonHelper.asIdentifier(config.get("fragmentShader"));
		allowEntities = config.getBoolean("allowEntities", true);
		allowParticles = config.getBoolean("allowParticles", true);
		supportForwardRender = config.getBoolean("supportForwardRender", true);
		offsetSlopeFactor = config.getFloat("offsetSlopeFactor", DEFAULT_SHADOW_SLOPE_FACTOR);
		offsetBiasUnits = config.getFloat("offsetBiasUnits", DEFAULT_SHADOW_BIAS_UNITS);
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= framebuffer.validate("Invalid pipeline config - skyShadows framebuffer missing or invalid.");
		valid &= assertAndWarn(vertexShader != null, "Invalid pipeline config - skyShadows 'vertexShader' missing or invalid.");
		valid &= assertAndWarn(fragmentShader != null, "Invalid pipeline config - skyShadows 'fragmentShader' missing or invalid.");

		if (valid) {
			valid &= assertAndWarn(framebuffer.value().depthAttachment.image.value().target == GL46.GL_TEXTURE_2D_ARRAY,
					"Invalid pipeline config - skyShadows depth image must be a 2D array texture.");
		}

		return valid;
	}

	public static final float DEFAULT_SHADOW_BIAS_UNITS = 4.0f;
	public static final float DEFAULT_SHADOW_SLOPE_FACTOR = 1.1f;
}
