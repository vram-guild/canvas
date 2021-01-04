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

package grondag.canvas.pipeline.config.util;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;

public class JanksonHelper {
	public static @Nullable String asString(JsonElement json) {
		if (json instanceof JsonPrimitive) {
			final JsonPrimitive p = (JsonPrimitive) json;

			if (p.getValue() instanceof String) {
				return (String) p.getValue();
			}
		}

		return null;
	}

	public static @Nullable Identifier asIdentifier(JsonElement json) {
		if (json instanceof JsonPrimitive) {
			final JsonPrimitive p = (JsonPrimitive) json;

			if (p.getValue() instanceof String) {
				final String id = (String) p.getValue();
				return id == null || id.isEmpty() ? null : Identifier.tryParse((String) p.getValue());
			}
		}

		return null;
	}

	public static @Nullable String asStringOrDefault(JsonElement json, String defaultVal) {
		if (json instanceof JsonPrimitive) {
			final JsonPrimitive p = (JsonPrimitive) json;

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
}
