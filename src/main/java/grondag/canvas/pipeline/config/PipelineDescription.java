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
import grondag.canvas.pipeline.config.util.JanksonHelper;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

public class PipelineDescription {
	public final ResourceLocation id;
	public final String nameKey;
	public final String descriptionKey;
	public final boolean isFabulous;

	public PipelineDescription (ResourceLocation id, JsonObject config) {
		this.id = id;

		final String nameKey = JanksonHelper.asString(config.get("nameKey"));
		this.nameKey = nameKey == null ? id.toString() : nameKey;
		isFabulous = config.containsKey("fabulousTargets");

		final String descriptionKey = JanksonHelper.asString(config.get("descriptionKey"));
		this.descriptionKey = descriptionKey == null ? "pipeline.no_desc" : descriptionKey;
	}

	public String name() {
		return I18n.get(nameKey);
	}

	public String description() {
		return I18n.get(descriptionKey);
	}
}
