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

public class FabulousConfig {
	public final String entityFrambuffer;
	public final String particleFrambuffer;
	public final String weatherFrambuffer;
	public final String cloudsFrambuffer;
	public final String translucentFrambuffer;
	public final boolean isValid;

	private FabulousConfig (JsonObject config) {
		entityFrambuffer = config.get(String.class, "entity");
		particleFrambuffer = config.get(String.class, "particles");
		weatherFrambuffer = config.get(String.class, "weather");
		cloudsFrambuffer = config.get(String.class, "clouds");
		translucentFrambuffer = config.get(String.class, "translucent");

		isValid = entityFrambuffer != null && !entityFrambuffer.isEmpty()
				&& particleFrambuffer != null && !particleFrambuffer.isEmpty()
				&& weatherFrambuffer != null && !weatherFrambuffer.isEmpty()
				&& cloudsFrambuffer != null && !cloudsFrambuffer.isEmpty()
				&& translucentFrambuffer != null && !translucentFrambuffer.isEmpty();

		if (!isValid) {
			CanvasMod.LOG.warn("Invalid pipeline config - incomplete/invalid fabulousTargets config.");
		}
	}

	public static @Nullable FabulousConfig deserialize(JsonObject config) {
		if (config == null || !config.containsKey("fabulousTargets")) {
			return null;
		}

		return new FabulousConfig(config.getObject("fabulousTargets"));
	}
}
