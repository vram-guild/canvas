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

public class FabulousConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> entityFramebuffer;
	public final NamedDependency<FramebufferConfig> particleFramebuffer;
	public final NamedDependency<FramebufferConfig> weatherFramebuffer;
	public final NamedDependency<FramebufferConfig> cloudsFramebuffer;
	public final NamedDependency<FramebufferConfig> translucentFramebuffer;

	FabulousConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		entityFramebuffer = ctx.frameBuffers.dependOn(config, "entity");
		particleFramebuffer = ctx.frameBuffers.dependOn(config, "particles");
		weatherFramebuffer = ctx.frameBuffers.dependOn(config, "weather");
		cloudsFramebuffer = ctx.frameBuffers.dependOn(config, "clouds");
		translucentFramebuffer = ctx.frameBuffers.dependOn(config, "translucent");
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		valid &= entityFramebuffer.validate("Invalid pipeline config - fabulousTarget 'entity' missing or invalid.");
		valid &= particleFramebuffer.validate("Invalid pipeline config - fabulousTarget 'particles' missing or invalid.");
		valid &= weatherFramebuffer.validate("Invalid pipeline config - fabulousTarget 'weather' missing or invalid.");
		valid &= cloudsFramebuffer.validate("Invalid pipeline config - fabulousTarget 'clouds' missing or invalid.");
		valid &= translucentFramebuffer.validate("Invalid pipeline config - fabulousTarget 'translucent' missing or invalid.");
		return valid;
	}
}
