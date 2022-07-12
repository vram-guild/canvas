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

import grondag.canvas.pipeline.GlSymbolLookup;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;
import grondag.canvas.varia.GFX;

public class ImageConfig extends NamedConfig<ImageConfig> {
	public final int target;
	public final int internalFormat;
	public final int pixelFormat;
	public final int pixelDataType;
	public final int lod;
	/** 0 if tied to framebuffer size. */
	public final int width;
	/** 0 if tied to framebuffer size. */
	public final int height;
	public final int depth;
	public final int[] texParamPairs;

	private ImageConfig(ConfigContext ctx, String name, int internalFormat, int lod, int pixelFormat, int pixelDataType, boolean depth) {
		super(ctx, name);
		target = GFX.GL_TEXTURE_2D;
		this.internalFormat = internalFormat;
		this.lod = lod;
		this.pixelDataType = pixelDataType;
		this.pixelFormat = pixelFormat;
		width = 0;
		height = 0;
		this.depth = 1;

		if (depth) {
			texParamPairs = new int[10];
			texParamPairs[1] = GFX.GL_NEAREST;
			texParamPairs[3] = GFX.GL_NEAREST;
			texParamPairs[8] = GFX.GL_TEXTURE_COMPARE_MODE;
			texParamPairs[9] = GFX.GL_NONE;
		} else {
			texParamPairs = new int[8];
			texParamPairs[1] = GFX.GL_LINEAR;
			texParamPairs[3] = GFX.GL_LINEAR;
		}

		texParamPairs[0] = GFX.GL_TEXTURE_MIN_FILTER;
		texParamPairs[2] = GFX.GL_TEXTURE_MAG_FILTER;
		texParamPairs[4] = GFX.GL_TEXTURE_WRAP_S;
		texParamPairs[5] = GFX.GL_CLAMP_TO_EDGE;
		texParamPairs[6] = GFX.GL_TEXTURE_WRAP_T;
		texParamPairs[7] = GFX.GL_CLAMP_TO_EDGE;
	}

	ImageConfig (ConfigContext ctx, JsonObject config) {
		super(ctx, config.get(String.class, "name"));
		target = ctx.dynamic.getGlConst(config, "target", "TEXTURE_2D");
		internalFormat = ctx.dynamic.getGlConst(config, "internalFormat", "RGBA8");
		lod = ctx.dynamic.getInt(config,"lod", 0);
		pixelFormat = ctx.dynamic.getGlConst(config, "pixelFormat", "RGBA");
		pixelDataType = ctx.dynamic.getGlConst(config, "pixelDataType", "UNSIGNED_BYTE");
		depth = ctx.dynamic.getInt(config,"depth", 1);

		final int size = ctx.dynamic.getInt(config, "size", 0);
		width = ctx.dynamic.getInt(config, "width", size);
		height = ctx.dynamic.getInt(config, "height", size);

		if (!config.containsKey("texParams")) {
			texParamPairs = new int[0];
		} else {
			final JsonArray params = config.get(JsonArray.class, "texParams");
			final int limit = params.size();
			texParamPairs = new int[limit * 2];

			int j = 0;

			for (int i = 0; i < limit; ++i) {
				final JsonObject p = (JsonObject) params.get(i);
				texParamPairs[j++] = ctx.dynamic.getGlConst(p, "name", "NONE");
				texParamPairs[j++] = ctx.dynamic.getGlConst(p, "val", "NONE");
			}
		}
	}

	public static ImageConfig defaultMain(ConfigContext ctx) {
		return new ImageConfig(ctx, "default_main", GFX.GL_RGBA8, 0, GFX.GL_RGBA, GFX.GL_UNSIGNED_BYTE, false);
	}

	public static ImageConfig defaultDepth(ConfigContext ctx) {
		return new ImageConfig(ctx, "default_depth", GFX.GL_DEPTH_COMPONENT, 0, GFX.GL_DEPTH_COMPONENT, GFX.GL_FLOAT, true);
	}

	@Override
	public NamedDependencyMap<ImageConfig> nameMap() {
		return context.images;
	}

	@Override
	public boolean validate() {
		boolean valid = super.validate();

		valid &= assertAndWarn(target == GFX.GL_TEXTURE_3D || target == GFX.GL_TEXTURE_2D_ARRAY || target == GFX.GL_TEXTURE_2D, "Invalid pipeline config for image %s. Unsupported target.", name, GlSymbolLookup.reverseLookup(target));
		valid &= assertAndWarn(!(target == GFX.GL_TEXTURE_2D && depth > 1), "Invalid pipeline config for image %s.  2D texture has depth > 1.", name);
		valid &= assertAndWarn(!((target == GFX.GL_TEXTURE_3D || target == GFX.GL_TEXTURE_2D_ARRAY) && depth < 1), "Invalid pipeline config for image %s.  3D texture must have depth >= 1.", name);

		return valid;
	}
}
