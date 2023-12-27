/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
	public final int layer;
	public final int face;
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
			layer = 0;
			face = -1;
			clear = false;
			clearColor = 0;
			clearDepth = 1.0f;
		} else {
			image = context.images.dependOn(context.dynamic.getString(config, "image"));
			lod = context.dynamic.getInt(config, "lod", 0);
			layer = context.dynamic.getInt(config, "layer", 0);
			face = context.dynamic.getInt(config, "face", -1);

			if (isDepth) {
				clear = config.containsKey("clearDepth");
				clearDepth = context.dynamic.getFloat(config, "clearDepth", 1.0f);
				clearColor = 0;
			} else {
				clear = config.containsKey("clearColor");
				clearColor = context.dynamic.getInt(config, "clearColor", 0);
				clearDepth = 1.0f;
			}
		}
	}

	private AttachmentConfig(ConfigContext ctx, String name) {
		super(ctx);
		image = context.images.dependOn(name);
		lod = 0;
		layer = 0;
		face = -1;
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
		final int limit = array != null ? array.size() : 0;
		final AttachmentConfig[] result = new AttachmentConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new AttachmentConfig(ctx, ctx.dynamic.getAttachment(array.get(i)), false);
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
