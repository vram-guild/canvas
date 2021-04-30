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
import grondag.canvas.pipeline.config.util.NamedDependency;

public class MaterialProgramConfig extends ProgramConfig {
	public final NamedDependency<ImageConfig>[] samplerImages;

	public MaterialProgramConfig(ConfigContext ctx, JsonObject config) {
		super(ctx, config, "materialProgram");

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

	public MaterialProgramConfig(ConfigContext ctx) {
		super(ctx, "materialProgram", "canvas:shaders/pipeline/standard.vert", "canvas:shaders/pipeline/standard.vert");
		samplerImages = new NamedDependency[0];
	}

	@Override
	public boolean validate() {
		if (samplerImages.length != samplerNames.length) {
			CanvasMod.LOG.warn(String.format("Material program is invalid because it expects %d samplers but the pass binds %d.",
				samplerNames.length, samplerImages.length));
			return false;
		}
		return super.validate();
	}
}
