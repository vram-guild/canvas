package grondag.canvas.pipeline.config;

import net.minecraft.util.Identifier;

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

public class ShaderConfig {
	public Identifier id;
	public Identifier vertexSource;
	public Identifier fragmentSource;
	public String[] samplerNames;

	public static ShaderConfig of(
			Identifier id,
			Identifier vertexSource,
			Identifier fragmentSource,
			String... samplerNames
	) {
		final ShaderConfig result = new ShaderConfig();
		result.id = id;
		result.vertexSource = vertexSource;
		result.fragmentSource = fragmentSource;
		result.samplerNames = samplerNames;
		return result;
	}

	public static ShaderConfig of(
			Identifier id,
			String vertexSource,
			String fragmentSource,
			String... samplerNames
	) {
		return of(id, new Identifier(vertexSource), new Identifier(fragmentSource), samplerNames);
	}

	public static ShaderConfig[] array(ShaderConfig... configs) {
		return configs;
	}
}
