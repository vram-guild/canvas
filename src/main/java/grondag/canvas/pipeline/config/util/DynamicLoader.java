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

package grondag.canvas.pipeline.config.util;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import grondag.canvas.pipeline.GlSymbolLookup;

/**
 * Loads json values verbatim or resolves values of user pipeline options when requested.
 */
public class DynamicLoader {
	private final ConfigContext ctx;

	DynamicLoader(ConfigContext context) {
		ctx = context;
	}

	/**
	 * Get a string verbatim or resolve dynamic string value.
	 *
	 * @param config source config object
	 * @param key config key
	 * @param defaultVal default string value if resolution fails or key not found
	 * @return constant or resolved string value
	 */
	public String getString(JsonObject config, String key, String defaultVal) {
		final JsonElement element = config.get(key);

		return getString(defaultVal, element);
	}

	/**
	 * Overload of {@link #getString(JsonObject, String, String)} with null default.
	 */
	public @Nullable String getString(JsonObject config, String key) {
		return getString(config, key, null);
	}

	/**
	 * Read a Json element as a string constant or resolved dynamic string, or null if failed.
	 *
	 * @param element target Json element
	 * @return resolved string value or null
	 */
	public @Nullable String getString(JsonElement element) {
		return getString(null, element);
	}

	// Order of defaultVal reversed to not mistake it for json key
	private String getString(String defaultVal, JsonElement element) {
		if (element instanceof JsonPrimitive primitive && primitive.getValue() instanceof CharSequence) {
			return primitive.asString();
		} else {
			return deserialize(String.class, element, defaultVal);
		}
	}

	/**
	 * Get a boolean verbatim or resolve dynamic boolean value.
	 *
	 * @param config source config object
	 * @param key config key
	 * @param defaultVal default boolean value if resolution fails or key not found
	 * @return constant or resolved boolean value
	 */
	public boolean getBoolean(JsonObject config, String key, boolean defaultVal) {
		final JsonElement element = config.get(key);

		if (element instanceof JsonPrimitive primitive && primitive.getValue() instanceof Boolean) {
			return primitive.asBoolean(defaultVal);
		} else {
			return deserialize(Boolean.class, element, defaultVal);
		}
	}

	/**
	 * Get an integer verbatim or resolve dynamic integer value.
	 *
	 * @param config source config object
	 * @param key config key
	 * @param defaultVal default integer value if resolution fails or key not found
	 * @return constant or resolved integer value
	 */
	public int getInt(JsonObject config, String key, int defaultVal) {
		final JsonElement element = config.get(key);
		return getInt(element, defaultVal);
	}

	/**
	 * Read a Json element as an integer constant or resolved dynamic string.
	 *
	 * @param element target Json element
	 * @param defaultVal default integer value if parsing or resolution fails
	 * @return resolved integer value
	 */
	public int getInt(JsonElement element, int defaultVal) {
		if (element instanceof JsonPrimitive primitive && primitive.getValue() instanceof Number) {
			return primitive.asInt(defaultVal);
		} else {
			return deserialize(Integer.class, element, defaultVal);
		}
	}

	/**
	 * Get a float verbatim or resolve dynamic float value.
	 *
	 * @param config source config object
	 * @param key config key
	 * @param defaultVal default float value if resolution fails or key not found
	 * @return constant or resolved float value
	 */
	public float getFloat (JsonObject config, String key, float defaultVal) {
		final JsonElement element = config.get(key);

		if (element instanceof JsonPrimitive primitive && primitive.getValue() instanceof Number) {
			return primitive.asFloat(defaultVal);
		} else {
			return deserialize(Float.class, element, defaultVal);
		}
	}

	/**
	 * Get an GL enum int constant from a string constant or a resolved dynamic string.
	 *
	 * @param config source config object
	 * @param key config key
	 * @param fallback default GL enum in string form if resolution fails or key not found
	 * @return GL enum integer constant
	 */
	public int getGlConst(JsonObject config, String key, String fallback) {
		final String glEnum = getString(config, key, fallback);
		return GlSymbolLookup.lookup(glEnum, fallback);
	}

	/**
	 * Special handler for framebuffer attachment object.
	 *
	 * @param config source config object
	 * @param key config key
	 * @return parsed Json object
	 */
	public JsonObject getAttachment(JsonObject config, String key) {
		return getAttachment(config.get(key));
	}

	public JsonObject getAttachment(JsonElement element) {
		if (element instanceof JsonObject attachment) {
			// prioritize framebuffer attachment "primitive"
			if (attachment.containsKey("image")) {
				return attachment;
			} else {
				return deserialize(JsonObject.class, attachment, null);
			}
		} else {
			return null;
		}
	}

	private <ForType> ForType deserialize(Class<ForType> clazz, JsonElement element, ForType defaultVal) {
		if (element instanceof JsonObject obj) {
			final String optionKey = obj.get(String.class, "option");
			final ForType suppliedDefault = obj.get(clazz, "default");

			if (suppliedDefault != null) {
				defaultVal = suppliedDefault;
			}

			ForType resolved = null;

			if (optionKey != null) {
				resolved = resolve(clazz, optionKey);
			}

			if (resolved == null) {
				resolved = deserializeMap(clazz, obj);
			}

			if (resolved != null) {
				return resolved;
			}
		}

		return defaultVal;
	}

	@SuppressWarnings("unchecked")
	private <ForType> ForType resolve(Class<ForType> clazz, String optionKey) {
		if (clazz == JsonObject.class) {
			// not supported
			return null;
		} else if (clazz == String.class) {
			var config = ctx.enumConfigEntries.get(optionKey);

			if (config != null) {
				return (ForType) config.value();
			}
		} else if (clazz == Boolean.class) {
			var config = ctx.booleanConfigEntries.get(optionKey);

			if (config != null) {
				return (ForType) Boolean.valueOf(config.value());
			}
		} else if (clazz == Integer.class) {
			var intOption = ctx.intConfigEntries.get(optionKey);

			if (intOption != null) {
				return (ForType) Integer.valueOf(intOption.value());
			}

			// also try float options
			var floatOption = ctx.floatConfigEntries.get(optionKey);

			if (floatOption != null) {
				return (ForType) Integer.valueOf((int) floatOption.value());
			}
		} else if (clazz == Float.class) {
			var floatOption = ctx.floatConfigEntries.get(optionKey);

			if (floatOption != null) {
				return (ForType) Float.valueOf(floatOption.value());
			}

			// also try int options
			var intOption = ctx.intConfigEntries.get(optionKey);

			if (intOption != null) {
				return (ForType) Float.valueOf((float) intOption.value());
			}
		}

		return null;
	}

	private <ForType> ForType deserializeMap(Class<ForType> clazz, JsonObject obj) {
		final JsonObject optionMap = obj.getObject("optionMap");

		if (optionMap != null) {
			for (var e:optionMap.entrySet()) {
				final String optionKey = e.getKey();
				final JsonElement element = e.getValue();

				if (optionKey != null && (element instanceof JsonArray jsonMap)) {
					final ForType resolved = resolveSingleMap(clazz, optionKey, jsonMap);

					// if we can't get a meaningful value we try another map
					if (resolved != null) {
						return resolved;
					}
				}
			}
		}

		return null;
	}

	private <ToType> ToType resolveSingleMap(Class<ToType> toType, String optionKey, JsonArray jsonMap) {
		var source = resolveOption(optionKey);

		for (JsonElement element : jsonMap) {
			if (element instanceof JsonObject obj) {
				final var from = obj.get(JsonPrimitive.class, "from");
				final var to = obj.get(toType, "to");

				if (from != null && source.equals(from.getValue())) {
					// condition met, return immediately
					return to;
				}
			}
		}

		return null;
	}

	public Object resolveOption(String optionKey) {
		final var enumOption = ctx.enumConfigEntries.get(optionKey);

		if (enumOption != null) {
			return enumOption.value();
		}

		final var booleanOption = ctx.booleanConfigEntries.get(optionKey);

		if (booleanOption != null) {
			return booleanOption.value();
		}

		final var intOption = ctx.intConfigEntries.get(optionKey);

		if (intOption != null) {
			return intOption.value();
		}

		final var floatOption = ctx.floatConfigEntries.get(optionKey);

		if (floatOption != null) {
			return floatOption.value();
		}

		return null;
	}
}
