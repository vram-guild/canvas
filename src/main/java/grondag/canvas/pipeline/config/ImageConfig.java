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

import grondag.canvas.pipeline.GlSymbolLookup;

public class ImageConfig {
	public final String name;
	public final boolean depth;
	public final int internalFormat;
	public final int lod;
	public final int[] texParamPairs;

	public ImageConfig(String name, boolean depth, int internalFormat, int lod) {
		this.name = name;
		this.depth = depth;
		this.internalFormat = internalFormat;
		this.lod = lod;
		texParamPairs = new int[0];
	}

	private ImageConfig (JsonObject config) {
		name = config.get(String.class, "name");
		depth = config.getBoolean("depth", false);
		internalFormat = GlSymbolLookup.lookup(config, "internalFormat", "RGBA8");
		lod = config.getInt("lod", 0);

		if (!config.containsKey("texParams")) {
			texParamPairs = new int[0];
		} else {
			final JsonArray params = config.get(JsonArray.class, "texParams");
			final int limit = params.size();
			texParamPairs = new int[limit * 2];

			int j = 0;

			for (int i = 0; i < limit; ++i) {
				final JsonObject p = (JsonObject) params.get(i);
				texParamPairs[j++] = GlSymbolLookup.lookup(p, "name", "NONE");
				texParamPairs[j++] = GlSymbolLookup.lookup(p, "val", "NONE");
			}
		}
	}

	public static ImageConfig[] deserialize(JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("images")) {
			return new ImageConfig[0];
		}

		final JsonArray images = configJson.get(JsonArray.class, "images");
		final int limit = images.size();
		final ImageConfig[] result = new ImageConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new ImageConfig((JsonObject) images.get(i));
		}

		return result;
	}
}
