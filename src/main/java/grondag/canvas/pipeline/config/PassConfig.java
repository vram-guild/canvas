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
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependency;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class PassConfig extends NamedConfig<PassConfig> {
	public final NamedDependency<FramebufferConfig> framebuffer;
	public final NamedDependency<ImageConfig>[] samplerImages;
	public final NamedDependency<ProgramConfig> program;
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

	@SuppressWarnings("unchecked")
	private PassConfig (ConfigContext ctx, JsonObject config) {
		super(ctx, JanksonHelper.asStringOrDefault(config.get("name"), JanksonHelper.asString(config.get("framebuffer"))));
		framebuffer = ctx.frameBuffers.createDependency(JanksonHelper.asString(config.get("framebuffer")));
		program = ctx.programs.createDependency(JanksonHelper.asString(config.get("program")));

		lod = config.getInt("lod", 0);

		if (!config.containsKey("samplerImages")) {
			samplerImages = new NamedDependency[0];
		} else {
			final JsonArray names = config.get(JsonArray.class, "samplerImages");
			final int limit = names.size();
			samplerImages = new NamedDependency[limit];

			for (int i = 0; i < limit; ++i) {
				samplerImages[i] = ctx.images.createDependency(JanksonHelper.asString(names.get(i)));
			}
		}
	}

	public static PassConfig[] deserialize(ConfigContext ctx, JsonObject configJson, String key) {
		if (configJson == null || !configJson.containsKey(key)) {
			return new PassConfig[0];
		}

		final JsonObject passJson = configJson.getObject(key);

		if (passJson == null || !passJson.containsKey("passes")) {
			return new PassConfig[0];
		}

		final JsonArray array = JanksonHelper.getJsonArrayOrNull(passJson, "passes",
				String.format("Error parsing pipeline stage %s.  Passes must be an array. No passes created.", key));

		if (array == null) {
			return new PassConfig[0];
		}

		final int limit = array.size();
		final PassConfig[] result = new PassConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new PassConfig(ctx, (JsonObject) array.get(i));
		}

		return result;
	}

	public static String CLEAR_NAME = "frex_clear";

	@Override
	public boolean validate() {
		boolean valid = super.validate();

		if (!framebuffer.isValid()) {
			CanvasMod.LOG.warn(String.format("Pass %s invalid because framebuffer %s not found or invalid.", name, framebuffer.name));
			valid = false;
		}

		if (!program.isValid()) {
			CanvasMod.LOG.warn(String.format("Pass %s invalid because program %s not found or invalid.", name, program.name));
			valid = false;
		}

		for (final NamedDependency<ImageConfig> img : samplerImages) {
			if (!img.isValid()) {
				CanvasMod.LOG.warn(String.format("Pass %s invalid because samplerImage %s not found or invalid.", name, img.name));
				valid = false;
			}
		}

		return valid;
	}

	@Override
	public NamedDependencyMap<PassConfig> nameMap() {
		return context.passes;
	}
}
