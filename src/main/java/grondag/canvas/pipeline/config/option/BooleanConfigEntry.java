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
	SpruceOption buildEntry() {
		return StandardOption.booleanOption(nameKey,
				() -> value,
				b -> value = b,
				defaultVal,
				ConfigManager.parseTooltip(descriptionKey));
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
