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

	AttachmentConfig(JsonObject config) {
		imageName = config.get(String.class, "imageName");
		lod = config.getInt("lod", 0);
		clearColor = config.getInt("clearColor", 0);
	}

	public static AttachmentConfig[] deserialize(JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("colorAttachments")) {
			return new AttachmentConfig[0];
		}

		final JsonArray array = configJson.get(JsonArray.class, "colorAttachments");
		final int limit = array.size();
		final AttachmentConfig[] result = new AttachmentConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new AttachmentConfig((JsonObject) array.get(i));
		}

		return result;
	}
}
