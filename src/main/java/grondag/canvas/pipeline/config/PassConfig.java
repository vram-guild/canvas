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

import grondag.canvas.CanvasMod;

public class PassConfig {
	public final String name;
	public final String framebufferName;
	public final String[] samplerNames;
	public final String programName;
	// for computing size
	public final int lod;

	//	// For blit operations
	//	public final String sourceFrameBufferName;
	//
	//	//	readFramebuffer
	//	//	Specifies the name of the source framebuffer object for glBlitNamedFramebuffer.
	//
	//	//	drawFramebuffer
	//	//	Specifies the name of the destination framebuffer object for glBlitNamedFramebuffer.
	//
	//	//Specify the bounds of the source rectangle within the read buffer of the read framebuffer.
	//	public float srcX0, srcY0, srcX1, srcY1;
	//
	//	//Specify the bounds of the destination rectangle within the write buffer of the write framebuffer.
	//	public float dstX0, dstY0, dstX1, dstY1;
	//
	//	// The bitwise OR of the flags indicating which buffers are to be copied. The allowed flags are GL_COLOR_BUFFER_BIT, GL_DEPTH_BUFFER_BIT and GL_STENCIL_BUFFER_BIT.
	//	public String[] maskFlags;
	//
	//	// Specifies the interpolation to be applied if the image is stretched. Must be GL_NEAREST or GL_LINEAR.
	//	public String filter;

	private PassConfig (JsonObject config) {
		framebufferName = JanksonHelper.asString(config.get("framebuffer"));
		programName = JanksonHelper.asString(config.get("program"));

		final String name = JanksonHelper.asString(config.get("name"));
		this.name = name == null ? framebufferName : name;

		lod = config.getInt("lod", 0);

		if (!config.containsKey("samplerImages")) {
			samplerNames = new String[0];
		} else {
			final JsonArray names = config.get(JsonArray.class, "samplerImages");
			final int limit = names.size();
			samplerNames = new String[limit];

			for (int i = 0; i < limit; ++i) {
				final String s = JanksonHelper.asString(names.get(i));

				if (s == null) {
					CanvasMod.LOG.warn(String.format("Sampler image name %s (%d of %d) for pass %s is not a valid string and was skipped.",
							names.get(i).toString(), i, limit, name));
				} else {
					samplerNames[i] = s;
				}
			}
		}
	}

	public static PassConfig[] deserialize(JsonObject configJson, String key) {
		if (configJson == null || !configJson.containsKey(key)) {
			return new PassConfig[0];
		}

		final JsonObject passJson = configJson.getObject(key);

		if (passJson == null || !passJson.containsKey("passes")) {
			return new PassConfig[0];
		}

		final JsonArray array = JanksonHelper.getJsonArrayOrNull(passJson, "passes",
				String.format("Error parsing pipeline stage %s, resulting in no passes.  Passes must be an array.", key));

		if (array == null) {
			return new PassConfig[0];
		}

		final int limit = array.size();
		final PassConfig[] result = new PassConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new PassConfig((JsonObject) array.get(i));
		}

		return result;
	}

	public static String CLEAR_NAME = "frex_clear";
}
