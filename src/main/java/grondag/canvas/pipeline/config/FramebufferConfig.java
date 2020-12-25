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

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;

public class FramebufferConfig {
	public final String name;
	public final AttachmentConfig[] colorAttachments;

	private FramebufferConfig(JsonObject config) {
		name = config.get(String.class, "name");
		colorAttachments = AttachmentConfig.deserialize(config);
	}

	public static FramebufferConfig[] deserialize(JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("framebuffers")) {
			return new FramebufferConfig[0];
		}

		final JsonArray array = configJson.get(JsonArray.class, "framebuffers");
		final int limit = array.size();
		final FramebufferConfig[] result = new FramebufferConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new FramebufferConfig((JsonObject) array.get(i));
		}

		return result;
	}
}
