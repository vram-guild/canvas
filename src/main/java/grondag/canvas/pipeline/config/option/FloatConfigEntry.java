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
import blue.endless.jankson.JsonPrimitive;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.text.TranslatableText;

import grondag.canvas.config.ConfigManager;
import grondag.canvas.pipeline.config.util.ConfigContext;

public class FloatConfigEntry extends OptionConfigEntry {
	public final float defaultVal;
	public final float min;
	public final float max;
	private float value;

	protected FloatConfigEntry(ConfigContext ctx, String name, JsonObject config) {
		super(ctx, name, config);
		defaultVal = config.getFloat("default", Float.NaN);
		min = config.getFloat("min", Float.NaN);
		max = config.getFloat("max", Float.NaN);
		value = defaultVal;
	}

	@Override
	AbstractConfigListEntry<?> buildEntry(ConfigEntryBuilder builder) {
		return builder.startIntSlider(new TranslatableText(nameKey),
				(int) (value * 100), (int) (min * 100), (int) (max * 100))
				.setDefaultValue((int) (defaultVal * 100))
				.setTooltip(ConfigManager.parse(descriptionKey))
				.build();
	}

	@Override
	String createSource() {
		return "#define " + name.toUpperCase() + " " + value + "\n";
	}

	@Override
	void readConfig(JsonObject config) {
		value = config.getFloat("name", defaultVal);
	}

	@Override
	void writeConfig(JsonObject config) {
		config.put(name, new JsonPrimitive(value));
	}

	@Override
	public boolean validate() {
		boolean valid = super.validate();

		valid &= assertAndWarn(!Float.isNaN(defaultVal), "Invalid pipeline config option - missing default value");
		valid &= assertAndWarn(!Float.isNaN(min), "Invalid pipeline config option - missing min value");
		valid &= assertAndWarn(!Float.isNaN(max), "Invalid pipeline config option - missing max value");

		return valid;
	}
}
