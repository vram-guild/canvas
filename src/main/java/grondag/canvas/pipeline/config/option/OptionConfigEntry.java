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

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import dev.lambdaurora.spruceui.option.SpruceOption;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import grondag.canvas.CanvasMod;
import grondag.canvas.pipeline.config.util.ConfigContext;
import grondag.canvas.pipeline.config.util.JanksonHelper;
import grondag.canvas.pipeline.config.util.NamedConfig;

public abstract class OptionConfigEntry<T extends OptionConfigEntry<T>> extends NamedConfig<T> {
	public final String nameKey;
	public final String descriptionKey;

	protected OptionConfigEntry(ConfigContext ctx, String name, JsonObject config) {
		super(ctx, name);
		nameKey = JanksonHelper.asString(config.get("nameKey"));
		descriptionKey = JanksonHelper.asString(config.get("descriptionKey"));
	}

	abstract SpruceOption buildEntry();

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

	/**
	 *
	 * @param ctx
	 * @param key will be reliably present and is key for element - only called if exists
	 * @param obj will be reliably present and is element matching key - only called if exists
	 * @return
	 */
	static OptionConfigEntry<?> of(ConfigContext ctx, String key, JsonObject obj) {
		if (obj.containsKey("choices")) {
			return new EnumConfigEntry(ctx, key, obj);
		} else {
			final JsonElement defaultVal = obj.get("default");

			if (defaultVal instanceof final JsonPrimitive val) {
				if (val.getValue().getClass() == Double.class || val.getValue().getClass() == Float.class) {
					return new FloatConfigEntry(ctx, key, obj);
				} else if (val.getValue().getClass() == Integer.class || val.getValue().getClass() == Long.class) {
					return new IntConfigEntry(ctx, key, obj);
				} else {
					if (val.getValue().getClass() != Boolean.class) {
						CanvasMod.LOG.warn(String.format("Could not infer type of pipeline option \"%s\". Defaulting to boolean.", key));
					}

					return new BooleanConfigEntry(ctx, key, obj);
				}
			} else {
				CanvasMod.LOG.warn(String.format("Could not infer type of pipeline option \"%s\". Defaulting to boolean.", key));
				return new BooleanConfigEntry(ctx, key, obj);
			}
		}
	}
}
