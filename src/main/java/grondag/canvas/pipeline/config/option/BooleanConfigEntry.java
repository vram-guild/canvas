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

import java.util.Locale;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.text.TranslatableText;

import grondag.canvas.config.ConfigManager;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class BooleanConfigEntry extends OptionConfigEntry<BooleanConfigEntry> {
	public final boolean defaultVal;
	private boolean value;

	protected BooleanConfigEntry(ConfigContext ctx, String name, JsonObject config) {
		super(ctx, name, config);
		defaultVal = config.getBoolean("default", true);
		value = defaultVal;
	}

	@Override
	AbstractConfigListEntry<?> buildEntry(ConfigEntryBuilder builder) {
		return builder.startBooleanToggle(new TranslatableText(nameKey), value)
				.setDefaultValue(defaultVal)
				.setTooltip(ConfigManager.parse(descriptionKey))
				.setSaveConsumer(b -> value = b)
				.build();
	}

	@Override
	String createSource() {
		final String result = "#define " + name.toUpperCase(Locale.ROOT) + "\n";

		return value ? result : "// " + result;
	}

	@Override
	void readConfig(JsonObject config) {
		value = config.getBoolean(name, defaultVal);
	}

	@Override
	void writeConfig(JsonObject config) {
		config.put(name, new JsonPrimitive(value));
	}

	public boolean value() {
		return value;
	}

	@Override
	public NamedDependencyMap<BooleanConfigEntry> nameMap() {
		return context.booleanConfigEntries;
	}
}
