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

import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class AttachmentConfig extends AbstractConfig {
	public final NamedDependency<ImageConfig> image;
	public final int lod;
	public final int clearColor;
	public final boolean clear;
	public final boolean isDepth;
	public final float clearDepth;

	AttachmentConfig(ConfigContext ctx, JsonObject config, boolean isDepth) {
		super(ctx);
		this.isDepth = isDepth;

		if (config == null) {
			image = context.images.dependOn("__invalid__");
			lod = 0;
			clear = false;
			clearColor = 0;
			clearDepth = 1.0f;
		} else {
			image = context.images.dependOn(config, "image");
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
		}
	}

	private AttachmentConfig(ConfigContext ctx, String name) {
		super(ctx);
		image = context.images.dependOn(name);
		lod = 0;
		clearColor = 0;
		clear = false;
		isDepth = false;
		clearDepth = 1.0f;
	}

	public static AttachmentConfig[] deserialize(ConfigContext ctx, JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("colorAttachments")) {
			return new AttachmentConfig[0];
		}

		final JsonArray array = configJson.get(JsonArray.class, "colorAttachments");
		final int limit = array.size();
		final AttachmentConfig[] result = new AttachmentConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new AttachmentConfig(ctx, (JsonObject) array.get(i), false);
		}

		return result;
	}

	public static AttachmentConfig defaultMain(ConfigContext ctx) {
		return new AttachmentConfig(ctx, "default_main");
	}

	public static AttachmentConfig defaultDepth(ConfigContext ctx) {
		return new AttachmentConfig(ctx, "default_depth");
	}

	@Override
	public boolean validate() {
		return image.isValid();
	}
}
