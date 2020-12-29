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

public class DrawTargetsConfig extends AbstractConfig {
	public final NamedDependency<FramebufferConfig> solidTerrain;
	public final NamedDependency<FramebufferConfig> translucentTerrain;
	public final NamedDependency<FramebufferConfig> translucentEntity;
	public final NamedDependency<FramebufferConfig> weather;
	public final NamedDependency<FramebufferConfig> clouds;
	public final NamedDependency<FramebufferConfig> translucentParticles;

	private DrawTargetsConfig(ConfigContext ctx) {
		super(ctx);
		solidTerrain = ctx.frameBuffers.createDependency("default");
		translucentTerrain = ctx.frameBuffers.createDependency("default");
		translucentEntity = ctx.frameBuffers.createDependency("default");
		weather = ctx.frameBuffers.createDependency("default");
		clouds = ctx.frameBuffers.createDependency("default");
		translucentParticles = ctx.frameBuffers.createDependency("default");
	}

	private DrawTargetsConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		solidTerrain = ctx.frameBuffers.createDependency(config.get(String.class, "solidTerrain"));
		translucentTerrain = ctx.frameBuffers.createDependency(config.get(String.class, "translucentTerrain"));
		translucentEntity = ctx.frameBuffers.createDependency(config.get(String.class, "translucentEntity"));
		weather = ctx.frameBuffers.createDependency(config.get(String.class, "weather"));
		clouds = ctx.frameBuffers.createDependency(config.get(String.class, "clouds"));
		translucentParticles = ctx.frameBuffers.createDependency(config.get(String.class, "translucentParticles"));
	}

	public static @Nullable DrawTargetsConfig deserialize(ConfigContext ctx, JsonObject config) {
		if (config == null || !config.containsKey("drawTargets")) {
			CanvasMod.LOG.warn("Invalid pipeline config - missing drawTargets config.");
			return null;
		}

		return new DrawTargetsConfig(ctx, config.getObject("drawTargets"));
	}

	public static DrawTargetsConfig makeDefault(ConfigContext ctx) {
		return new DrawTargetsConfig(ctx);
	}

	@Override
	public boolean validate() {
		boolean valid = true;

		if (!solidTerrain.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets solidTerrain missing or invalid.");
			valid = false;
		}

		if (!translucentTerrain.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets translucentTerrain missing or invalid.");
			valid = false;
		}

		if (!translucentEntity.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets translucentEntity missing or invalid.");
			valid = false;
		}

		if (!weather.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets weather missing or invalid.");
			valid = false;
		}

		if (!clouds.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets clouds missing or invalid.");
			valid = false;
		}

		if (!translucentParticles.isValid()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets translucentParticles missing or invalid.");
			valid = false;
		}

		return valid;
	}
}
