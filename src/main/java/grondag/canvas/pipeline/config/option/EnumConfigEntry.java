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

import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import grondag.canvas.config.ConfigManager;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class EnumConfigEntry extends OptionConfigEntry<EnumConfigEntry> {
	public final String defaultVal;
	private String value;
	private final String[] choices;
	private final String prefix;
	private final String define;
	private final boolean enumerate;

	protected EnumConfigEntry(ConfigContext ctx, String name, JsonObject config) {
		super(ctx, name, config);
		choices = JanksonHelper.asStringArray(config.get("choices"));
		defaultVal = JanksonHelper.asStringOrDefault(config.get("default"), choices.length > 0 ? choices[0] : "");
		value = defaultVal;
		define = JanksonHelper.asStringOrDefault(config.get("define"), name).toUpperCase(Locale.ROOT);
		prefix = JanksonHelper.asStringOrDefault(config.get("prefix"), "").toUpperCase(Locale.ROOT);
		enumerate = config.getBoolean("enum", false);
	}

	@Override
	AbstractConfigListEntry<?> buildEntry(ConfigEntryBuilder builder) {
		return builder.startSelector(new TranslatableText(nameKey), choices, value)
				.setTooltip(ConfigManager.parse(descriptionKey))
				.setNameProvider(o -> new LiteralText(o.toUpperCase(Locale.ROOT)))
				.setSaveConsumer(v -> value = v)
				.build();
	}

	@Override
	String createSource() {
		final StringBuilder builder = new StringBuilder();

		if (enumerate) {
			for (int i = 0; i < choices.length; ++i) {
				builder.append("#define " + prefix + choices[i].toUpperCase(Locale.ROOT) + " " + i + "\n");
			}

			builder.append("\n");
			builder.append("#define " + define.toUpperCase(Locale.ROOT) + " " + prefix + value.toUpperCase(Locale.ROOT) + "\n");
		} else {
			builder.append("#define " + prefix + value.toUpperCase(Locale.ROOT) + "\n");
		}

		return builder.toString();
	}

	@Override
	void readConfig(JsonObject config) {
		value = JanksonHelper.asStringOrDefault(config.get(name), defaultVal);
	}

	@Override
	void writeConfig(JsonObject config) {
		config.put(name, new JsonPrimitive(value));
	}

	public String value() {
		return value;
	}

	@Override
	public boolean validate() {
		boolean valid = super.validate();

		valid &= assertAndWarn(choices.length > 1, "Invalid pipeline config option - enum options should have at least two choices");
		valid &= assertAndWarn(defaultVal != null && !defaultVal.isEmpty(), "Invalid pipeline config option - default value is missing or empty");

		for (final String s : choices) {
			valid &= assertAndWarn(!(s == null || s.isEmpty()), "Invalid pipeline config option - choices contains missing or empty value.");
		}

		return valid;
	}

	@Override
	public NamedDependencyMap<EnumConfigEntry> nameMap() {
		return context.enumConfigEntries;
	}
}
