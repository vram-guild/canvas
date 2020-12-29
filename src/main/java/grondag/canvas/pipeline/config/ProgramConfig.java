package grondag.canvas.pipeline.config;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;

import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

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

public class ProgramConfig extends NamedConfig<ProgramConfig> {
	public final Identifier vertexSource;
	public final Identifier fragmentSource;
	public final String[] samplerNames;
	public final boolean isBuiltIn;

	private ProgramConfig(ConfigContext ctx, JsonObject config) {
		super(ctx, config.get(String.class, "name"));
		vertexSource = new Identifier(config.get(String.class, "vertexSource"));
		fragmentSource = new Identifier(config.get(String.class, "fragmentSource"));

		if (!config.containsKey("samplers")) {
			samplerNames = new String[0];
		} else {
			final JsonArray names = config.get(JsonArray.class, "samplers");
			final int limit = names.size();
			samplerNames = new String[limit];

			for (int i = 0; i < limit; ++i) {
				final String s = JanksonHelper.asString(names.get(i));

				if (s == null) {
					CanvasMod.LOG.warn(String.format("Sampler name %s (%d of %d) for pipeline shader %s is not a valid string and was skipped.",
							names.get(i).toString(), i, limit, name));
				} else {
					samplerNames[i] = s;
				}
			}
		}

		isBuiltIn = false;
	}

	public ProgramConfig(ConfigContext ctx, String name) {
		super(ctx, name);
		vertexSource = null;
		fragmentSource = null;
		samplerNames = null;
		isBuiltIn = true;
	}

	public static ProgramConfig[] deserialize(ConfigContext ctx, JsonObject configJson) {
		if (configJson == null || !configJson.containsKey("programs")) {
			return new ProgramConfig[0];
		}

		final JsonArray array = configJson.get(JsonArray.class, "programs");
		final int limit = array.size();
		final ProgramConfig[] result = new ProgramConfig[limit];

		for (int i = 0; i < limit; ++i) {
			result[i] = new ProgramConfig(ctx, (JsonObject) array.get(i));
		}

		return result;
	}

	public static ProgramConfig builtIn(ConfigContext ctx, String name) {
		return new ProgramConfig(ctx, name);
	}

	@Override
	public NamedDependencyMap<ProgramConfig> nameMap() {
		return context.programs;
	}

	@Override
	public boolean validate() {
		return super.validate();
	}
}
