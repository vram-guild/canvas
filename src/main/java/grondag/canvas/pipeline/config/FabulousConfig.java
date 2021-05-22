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
