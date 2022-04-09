/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.pipeline.config.option;

import java.util.Locale;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import dev.lambdaurora.spruceui.option.SpruceOption;
import grondag.canvas.config.widget.StandardOption;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

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
	SpruceOption buildEntry() {
		return StandardOption.enumOption(nameKey,
						() -> value,
						s -> value = s,
						defaultVal,
						choices,
						ConfigManager.parseTooltip(descriptionKey));
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
