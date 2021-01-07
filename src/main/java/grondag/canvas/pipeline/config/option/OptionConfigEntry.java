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

package grondag.canvas.pipeline.config.option;

import blue.endless.jankson.JsonObject;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import grondag.canvas.pipeline.config.util.AbstractConfig;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;

public abstract class OptionConfigEntry extends AbstractConfig {
	public final String name;
	public final String nameKey;
	public final String descriptionKey;

	protected OptionConfigEntry(ConfigContext ctx, String name, JsonObject config) {
		super(ctx);
		this.name = name;
		nameKey = JanksonHelper.asString(config.get("nameKey"));
		descriptionKey = JanksonHelper.asString(config.get("descriptionKey"));
	}

	abstract AbstractConfigListEntry<?> buildEntry(ConfigEntryBuilder builder);

	abstract String createSource();

	abstract void readConfig(JsonObject config);

	abstract void writeConfig(JsonObject config);

	@Override
	public boolean validate() {
		boolean valid = true;

		valid &= assertAndWarn(name != null && !name.isEmpty(), "Invalid pipeline config option - name is missing");
		valid &= assertAndWarn(name != null && !nameKey.isEmpty(), "Invalid pipeline config option - nameKey is missing");

		return valid;
	}
}
