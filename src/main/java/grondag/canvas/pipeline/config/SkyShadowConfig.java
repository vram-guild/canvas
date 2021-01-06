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

import net.minecraft.util.Identifier;

import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class SkyShadowConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> framebuffer;
	public final boolean allowDisable;
	public final boolean includeTerrain;
	public final boolean includeEntities;
	public final boolean includeParticles;
	public final boolean supportForwardRender;
	public final Identifier vertexShader;
	public final Identifier fragmentShader;

	SkyShadowConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		framebuffer = ctx.frameBuffers.dependOn(config, "framebuffer");
		vertexShader = JanksonHelper.asIdentifier(config.get("vertexShader"));
		fragmentShader = JanksonHelper.asIdentifier(config.get("fragmentShader"));
		includeTerrain = config.getBoolean("includeTerrain", true);
		includeEntities = config.getBoolean("includeEntities", true);
		includeParticles = config.getBoolean("includeParticles", true);
		supportForwardRender = config.getBoolean("supportForwardRender", true);
		allowDisable = config.getBoolean("allowDisable", true);
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= framebuffer.validate("Invalid pipeline config - skyShadows framebuffer missing or invalid.");
		valid &= assertAndWarn(vertexShader != null, "Invalid pipeline config - skyShadows 'vertexShader' missing or invalid.");
		valid &= assertAndWarn(fragmentShader != null, "Invalid pipeline config - skyShadows 'fragmentShader' missing or invalid.");
		return valid;
	}
}
