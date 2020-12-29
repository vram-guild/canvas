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

public class AttachmentConfig {
	public final String imageName;
	public final int lod;
	public final int clearColor;
	public final boolean isValid;
	public final boolean clear;
	public final boolean isDepth;
	public final float clearDepth;

	AttachmentConfig(JsonObject config, boolean isDepth) {
		this.isDepth = isDepth;

		if (config == null) {
			imageName = "invalid";
			lod = 0;
			clear = false;
			clearColor = 0;
			isValid = false;
			clearDepth = 1.0f;
		} else {
			imageName = config.get(String.class, "image");
			lod = config.getInt("lod", 0);

			if (isDepth) {
				clear = config.containsKey("clearDepth");
				clearDepth = config.getFloat("clearDepth", 1.0f);
				clearColor = 0;
			} else {
				clear = config.containsKey("clearColor");
				clearColor = config.getInt("clearColor", 0);
				clearDepth = 1.0f;
			}

			isValid = true;
		}
	}

	private AttachmentConfig(String name) {
		imageName = name;
		lod = 0;
		clearColor = 0;
		clear = false;
		isValid = true;
		isDepth = false;
		clearDepth = 1.0f;
	}

	public static AttachmentConfig[] deserialize(JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("colorAttachments")) {
			return new AttachmentConfig[0];
		}

		final JsonArray array = configJson.get(JsonArray.class, "colorAttachments");
		final int limit = array.size();
		final AttachmentConfig[] result = new AttachmentConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new AttachmentConfig((JsonObject) array.get(i), false);
		}

		return result;
	}

	public static final AttachmentConfig DEFAULT_MAIN = new AttachmentConfig("default_main");
	public static final AttachmentConfig DEFAULT_DEPTH = new AttachmentConfig("default_depth");
}
