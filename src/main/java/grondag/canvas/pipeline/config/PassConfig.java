/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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
	public final int layer;

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
		layer = config.getInt("layer", 0);

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
