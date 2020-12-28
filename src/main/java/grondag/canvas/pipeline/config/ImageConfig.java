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
import org.lwjgl.opengl.GL21;

import grondag.canvas.pipeline.GlSymbolLookup;

public class ImageConfig {
	public final String name;
	public final int internalFormat;
	public final int pixelFormat;
	public final int pixelDataType;
	public final int lod;
	public final int[] texParamPairs;

	private ImageConfig(String name, int internalFormat, int lod, int pixelFormat, int pixelDataType, boolean depth) {
		this.name = name;
		this.internalFormat = internalFormat;
		this.lod = lod;
		this.pixelDataType = pixelDataType;
		this.pixelFormat = pixelFormat;

		if (depth) {
			texParamPairs = new int[10];
			texParamPairs[1] = GL21.GL_NEAREST;
			texParamPairs[3] = GL21.GL_NEAREST;
			texParamPairs[8] = GL21.GL_TEXTURE_COMPARE_MODE;
			texParamPairs[9] = GL21.GL_NONE;
		} else {
			texParamPairs = new int[8];
			texParamPairs[1] = GL21.GL_LINEAR;
			texParamPairs[3] = GL21.GL_LINEAR;
		}

		texParamPairs[0] = GL21.GL_TEXTURE_MIN_FILTER;
		texParamPairs[2] = GL21.GL_TEXTURE_MAG_FILTER;
		texParamPairs[4] = GL21.GL_TEXTURE_WRAP_S;
		texParamPairs[5] = GL21.GL_CLAMP;
		texParamPairs[6] = GL21.GL_TEXTURE_WRAP_T;
		texParamPairs[7] = GL21.GL_CLAMP;
	}

	private ImageConfig (JsonObject config) {
		name = config.get(String.class, "name");
		internalFormat = GlSymbolLookup.lookup(config, "internalFormat", "RGBA8");
		lod = config.getInt("lod", 0);
		pixelFormat = GlSymbolLookup.lookup(config, "pixelFormat", "RGBA");
		pixelDataType = GlSymbolLookup.lookup(config, "pixelDataType", "UNSIGNED_BYTE");

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

	public static ImageConfig DEFAULT_MAIN = new ImageConfig("default_main", GL21.GL_RGBA8, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, false);
	public static ImageConfig DEFAULT_DEPTH = new ImageConfig("default_depth", GL21.GL_DEPTH_COMPONENT, 0, GL21.GL_DEPTH_COMPONENT, GL21.GL_FLOAT, true);
}
