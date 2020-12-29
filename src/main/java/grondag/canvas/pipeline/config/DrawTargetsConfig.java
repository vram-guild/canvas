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

public class DrawTargetsConfig extends AbstractConfig {
	public final String solidTerrain;
	public final String translucentTerrain;
	public final String translucentEntity;
	public final String weather;
	public final String clouds;
	public final String translucentParticles;
	public final boolean isValid;

	private DrawTargetsConfig(ConfigContext ctx) {
		super(ctx);
		solidTerrain = "default";
		translucentTerrain = "default";
		translucentEntity = "default";
		weather = "default";
		clouds = "default";
		translucentParticles = "default";
		isValid = true;
	}

	private DrawTargetsConfig (ConfigContext ctx, JsonObject config) {
		super(ctx);
		solidTerrain = config.get(String.class, "solidTerrain");
		translucentTerrain = config.get(String.class, "translucentTerrain");
		translucentEntity = config.get(String.class, "translucentEntity");
		weather = config.get(String.class, "weather");
		clouds = config.get(String.class, "clouds");
		translucentParticles = config.get(String.class, "translucentParticles");

		boolean valid = true;

		if (solidTerrain == null || solidTerrain.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets missing solidTerrain.");
			valid = false;
		}

		if (translucentTerrain == null || translucentTerrain.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets missing translucentTerrain.");
			valid = false;
		}

		if (translucentEntity == null || translucentEntity.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets missing translucentEntity.");
			valid = false;
		}

		if (weather == null || weather.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets missing weather.");
			valid = false;
		}

		if (clouds == null || clouds.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets missing clouds.");
			valid = false;
		}

		if (translucentParticles == null || translucentParticles.isEmpty()) {
			CanvasMod.LOG.warn("Invalid pipeline config - drawTargets missing translucentParticles.");
			valid = false;
		}

		isValid = valid;
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
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
}
