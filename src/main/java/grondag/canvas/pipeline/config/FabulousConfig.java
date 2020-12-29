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
import org.jetbrains.annotations.Nullable;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class FabulousConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> entityFrambuffer;
	public final NamedDependency<FramebufferConfig> particleFrambuffer;
	public final NamedDependency<FramebufferConfig> weatherFrambuffer;
	public final NamedDependency<FramebufferConfig> cloudsFrambuffer;
	public final NamedDependency<FramebufferConfig> translucentFrambuffer;

	private FabulousConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		entityFrambuffer = ctx.frameBuffers.createDependency(config.get(String.class, "entity"));
		particleFrambuffer = ctx.frameBuffers.createDependency(config.get(String.class, "particles"));
		weatherFrambuffer = ctx.frameBuffers.createDependency(config.get(String.class, "weather"));
		cloudsFrambuffer = ctx.frameBuffers.createDependency(config.get(String.class, "clouds"));
		translucentFrambuffer = ctx.frameBuffers.createDependency(config.get(String.class, "translucent"));
	}

	public static @Nullable FabulousConfig deserialize(ConfigContext ctx, JsonObject config) {
		if (config == null || !config.containsKey("fabulousTargets")) {
			return null;
		}

		return new FabulousConfig(ctx, config.getObject("fabulousTargets"));
	}

	@Override
	public boolean validate() {
		boolean valid = true;

		if (!entityFrambuffer.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - fabulousTarget entity missing or invalid.");
			valid = false;
		}

		if (!particleFrambuffer.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - fabulousTarget particles missing or invalid.");
			valid = false;
		}

		if (!weatherFrambuffer.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - fabulousTarget weather missing or invalid.");
			valid = false;
		}

		if (!cloudsFrambuffer.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - fabulousTarget clouds missing or invalid.");
			valid = false;
		}

		if (!translucentFrambuffer.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - fabulousTarget translucent missing or invalid.");
			valid = false;
		}

		return valid;
	}
}
