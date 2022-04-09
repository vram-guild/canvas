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

import grondag.canvas.config.ConfigManager;
import grondag.canvas.config.widget.StandardOption;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.NamedDependencyMap;

public class FloatConfigEntry extends OptionConfigEntry<FloatConfigEntry> {
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
	SpruceOption buildEntry() {
		return StandardOption.floatOption(nameKey,
				min,
				max,
				0.005f,
				() -> value,
				f -> value = f,
				defaultVal,
				ConfigManager.parseTooltip(descriptionKey));
	}

	@Override
	String createSource() {
		return "#define " + name.toUpperCase(Locale.ROOT) + " " + value + "\n";
	}

	@Override
	void readConfig(JsonObject config) {
		value = config.getFloat(name, defaultVal);
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

	@Override
	public NamedDependencyMap<FloatConfigEntry> nameMap() {
		return context.floatConfigEntries;
	}
}
