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

import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class DrawTargetsConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> solidTerrain;
	public final NamedDependency<FramebufferConfig> translucentTerrain;
	public final NamedDependency<FramebufferConfig> translucentEntity;
	public final NamedDependency<FramebufferConfig> weather;
	public final NamedDependency<FramebufferConfig> clouds;
	public final NamedDependency<FramebufferConfig> translucentParticles;

	private DrawTargetsConfig(ConfigContext ctx) {
		super(ctx);
		solidTerrain = ctx.frameBuffers.dependOn("default");
		translucentTerrain = ctx.frameBuffers.dependOn("default");
		translucentEntity = ctx.frameBuffers.dependOn("default");
		weather = ctx.frameBuffers.dependOn("default");
		clouds = ctx.frameBuffers.dependOn("default");
		translucentParticles = ctx.frameBuffers.dependOn("default");
	}

	DrawTargetsConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		solidTerrain = ctx.frameBuffers.dependOn(config, "solidTerrain");
		translucentTerrain = ctx.frameBuffers.dependOn(config, "translucentTerrain");
		translucentEntity = ctx.frameBuffers.dependOn(config, "translucentEntity");
		weather = ctx.frameBuffers.dependOn(config, "weather");
		clouds = ctx.frameBuffers.dependOn(config, "clouds");
		translucentParticles = ctx.frameBuffers.dependOn(config, "translucentParticles");
	}

	public static DrawTargetsConfig makeDefault(ConfigContext ctx) {
		return new DrawTargetsConfig(ctx);
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= solidTerrain.validate("Invalid pipeline config - drawTargets solidTerrain missing or invalid.");
		valid &= translucentTerrain.validate("Invalid pipeline config - drawTargets translucentTerrain missing or invalid.");
		valid &= translucentEntity.validate("Invalid pipeline config - drawTargets translucentEntity missing or invalid.");
		valid &= weather.validate("Invalid pipeline config - drawTargets weather missing or invalid.");
		valid &= clouds.validate("Invalid pipeline config - drawTargets clouds missing or invalid.");
		valid &= translucentParticles.validate("Invalid pipeline config - translucentParticles solidTerrain missing or invalid.");
		return valid;
	}
}
