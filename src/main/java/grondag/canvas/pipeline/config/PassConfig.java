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
import grondag.canvas.pipeline.config.option.BooleanConfigEntry;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependency;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class PassConfig extends NamedConfig<PassConfig> {
	public final NamedDependency<FramebufferConfig> framebuffer;
	public final NamedDependency<ImageConfig>[] samplerImages;
	public final NamedDependency<ProgramConfig> program;
	public final NamedDependency<BooleanConfigEntry> toggleConfig;

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
	PassConfig (ConfigContext ctx, JsonObject config) {
		super(ctx, JanksonHelper.asStringOrDefault(config.get("name"), JanksonHelper.asString(config.get("framebuffer"))));
		framebuffer = ctx.frameBuffers.dependOn(config, "framebuffer");
		program = ctx.programs.dependOn(config, "program");
		toggleConfig = ctx.booleanConfigEntries.dependOn(config, "toggleConfig");

		lod = config.getInt("lod", 0);

		if (!config.containsKey("samplerImages")) {
			samplerImages = new NamedDependency[0];
		} else {
			final JsonArray names = config.get(JsonArray.class, "samplerImages");
			final int limit = names.size();
			samplerImages = new NamedDependency[limit];

			for (int i = 0; i < limit; ++i) {
				samplerImages[i] = ctx.images.dependOn(names.get(i));
			}
		}
	}

	@Override
	public boolean validate() {
		boolean valid = super.validate();
		valid &= framebuffer.validate("Pass %s invalid because framebuffer %s not found or invalid.", name, framebuffer.name);
		valid &= program.validate("Pass %s invalid because program %s not found or invalid.", name, program.name);

		for (final NamedDependency<ImageConfig> img : samplerImages) {
			valid &= img.validate("Pass %s invalid because samplerImage %s not found or invalid.", name, img.name);
		}

		if (program.value().samplerNames.length != samplerImages.length) {
			CanvasMod.LOG.warn(String.format("Pass %s invalid because program %s expects %d samplers but the pass binds %d.",
					name, program.name, program.value().samplerNames.length, samplerImages.length));
		}

		return valid;
	}

	@Override
	public NamedDependencyMap<PassConfig> nameMap() {
		return context.passes;
	}

	public static String CLEAR_NAME = "frex_clear";
}
