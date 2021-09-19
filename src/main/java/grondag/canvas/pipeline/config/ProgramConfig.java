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

import blue.endless.jankson.JsonObject;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedConfig;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;
import net.minecraft.resources.ResourceLocation;

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
