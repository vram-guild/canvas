/*
 * Copyright © Original Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.pipeline.config.util;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import grondag.canvas.CanvasMod;

public class JanksonHelper {
	public static @Nullable String asString(JsonElement json) {
		if (json instanceof JsonPrimitive p) {
			if (p.getValue() instanceof String) {
				return (String) p.getValue();
			}
		}

		return null;
	}

	public static @Nullable ResourceLocation asIdentifier(JsonElement json) {
		if (json instanceof JsonPrimitive p) {
			if (p.getValue() instanceof String) {
				final String id = (String) p.getValue();
				return id == null || id.isEmpty() ? null : ResourceLocation.tryParse((String) p.getValue());
			}
		}

		return null;
	}

	public static @Nullable String asStringOrDefault(JsonElement json, String defaultVal) {
		if (json instanceof JsonPrimitive p) {
			if (p.getValue() instanceof String) {
				return (String) p.getValue();
			}
		}

		return defaultVal;
	}

	public static @Nullable JsonArray getJsonArrayOrNull(JsonObject jsonObject, String key, String nonArrayMessage) {
		if (jsonObject == null || !jsonObject.containsKey(key)) {
			return null;
		}

		final JsonElement element = jsonObject.get(key);

		if (element instanceof JsonArray) {
			return (JsonArray) element;
		} else {
			CanvasMod.LOG.warn(nonArrayMessage);
			return null;
		}
	}

	public static String[] asStringArray(JsonElement element) {
		if (element instanceof JsonArray array) {
			if (!array.getString(0, "").equals("")) {
				final String[] result = new String[array.size()];

				for (int i = 0; i < array.size(); ++i) {
					result[i] = array.getString(i, "");
				}

				return result;
			}
		}

		return new String[0];
	}
}
