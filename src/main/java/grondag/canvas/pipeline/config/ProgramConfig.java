/*
 * Copyright © Contributing Authors
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

import blue.endless.jankson.JsonObject;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class ProgramConfig extends NamedConfig<ProgramConfig> {
	public final ResourceLocation vertexSource;
	public final ResourceLocation fragmentSource;
	public final String[] samplerNames;
	public final boolean isBuiltIn;

	ProgramConfig(ConfigContext ctx, JsonObject config, String name) {
		super(ctx, name);
		vertexSource = new ResourceLocation(config.get(String.class, "vertexSource"));
		fragmentSource = new ResourceLocation(config.get(String.class, "fragmentSource"));
		samplerNames = readerSamplerNames(ctx, config, "program " + name);
		isBuiltIn = false;
	}

	ProgramConfig(ConfigContext ctx, JsonObject config) {
		this(ctx, config, config.get(String.class, "name"));
	}

	public ProgramConfig(ConfigContext ctx, String name) {
		super(ctx, name);
		vertexSource = null;
		fragmentSource = null;
		samplerNames = null;
		isBuiltIn = true;
	}

	protected ProgramConfig(ConfigContext ctx, String name, String vertexSource, String fragmentSource) {
		super(ctx, name);
		this.vertexSource = new ResourceLocation(vertexSource);
		this.fragmentSource = new ResourceLocation(fragmentSource);
		samplerNames = new String[0];
		isBuiltIn = false;
	}

	public static ProgramConfig builtIn(ConfigContext ctx, String name) {
		return new ProgramConfig(ctx, name);
	}

	@Override
	public NamedDependencyMap<ProgramConfig> nameMap() {
		return context.programs;
	}
}
