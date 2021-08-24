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

public class IntConfigEntry extends OptionConfigEntry {
	public final int defaultVal;
	public final int min;
	public final int max;
	private int value;

	protected IntConfigEntry(ConfigContext ctx, String name, JsonObject config) {
		super(ctx, name, config);
		defaultVal = config.getInt("default", 0);
		min = config.getInt("min", Integer.MIN_VALUE);
		max = config.getInt("max", Integer.MAX_VALUE);
		value = defaultVal;
	}

	@Override
	AbstractConfigListEntry<?> buildEntry(ConfigEntryBuilder builder) {
		if (max - min <= 50) {
			return builder.startIntSlider(new TranslatableText(nameKey), value, min, max)
					.setDefaultValue(defaultVal)
					.setTooltip(ConfigManager.parse(descriptionKey))
					.setSaveConsumer(v -> value = v)
					.build();
		} else {
			return builder.startIntField(new TranslatableText(nameKey), value)
					.setMin(min)
					.setMax(max)
					.setDefaultValue(defaultVal)
					.setTooltip(ConfigManager.parse(descriptionKey))
					.setSaveConsumer(v -> value = v)
					.build();
		}
	}

	@Override
	String createSource() {
		return "#define " + name.toUpperCase(Locale.ROOT) + " " + value + "\n";
	}

	@Override
	void readConfig(JsonObject config) {
		value = config.getInt(name, defaultVal);
	}

	@Override
	void writeConfig(JsonObject config) {
		config.put(name, new JsonPrimitive(value));
	}

	@Override
	public boolean validate() {
		boolean valid = super.validate();

		valid &= assertAndWarn(defaultVal >= min && defaultVal <= max, "Invalid pipeline config option - default value out of range");
		valid &= assertAndWarn(min != Integer.MIN_VALUE, "Invalid pipeline config option - missing min value");
		valid &= assertAndWarn(max != Integer.MAX_VALUE, "Invalid pipeline config option - missing max value");

		return valid;
	}
}
