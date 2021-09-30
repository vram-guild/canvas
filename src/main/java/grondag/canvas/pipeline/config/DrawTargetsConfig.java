/*
 * Copyright © Contributing Authors
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
