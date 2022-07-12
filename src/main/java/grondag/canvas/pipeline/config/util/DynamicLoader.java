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

import java.util.Map;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import grondag.canvas.pipeline.GlSymbolLookup;
import grondag.canvas.pipeline.config.option.BooleanConfigEntry;
import grondag.canvas.pipeline.config.option.EnumConfigEntry;
import grondag.canvas.pipeline.config.option.FloatConfigEntry;
import grondag.canvas.pipeline.config.option.IntConfigEntry;

/**
 * Dynamic property loader. The dynamic resolvers are designed so that they may be stored for
 * deferred use, but the current implementation simply reads the resolved value immediately.
 */
public class DynamicLoader {
	private final ConfigContext ctx;

	DynamicLoader(ConfigContext context) {
		ctx = context;
	}

	public String getString(JsonObject config, String key, String defaultVal) {
		final JsonElement element = config.get(key);
		final Dynamic<String> resolver = deserialize(String.class, element, defaultVal);

		if (resolver != null) {
			return resolver.value();
		}

		return defaultVal;
	}

	public boolean getBoolean(JsonObject config, String key, boolean defaultVal) {
		final JsonElement element = config.get(key);

		if (element instanceof JsonPrimitive primitive) {
			defaultVal = primitive.asBoolean(defaultVal);
		}

		final Dynamic<Boolean> resolver = deserialize(Boolean.class, element, defaultVal);

		if (resolver != null) {
			return resolver.value();
		}

		return defaultVal;
	}

	public int getInt(JsonObject config, String key, int defaultVal) {
		final JsonElement element = config.get(key);

		if (element instanceof JsonPrimitive primitive) {
			defaultVal = primitive.asInt(defaultVal);
		}

		final Dynamic<Integer> resolver = deserialize(Integer.class, element, defaultVal);

		if (resolver != null) {
			return resolver.value();
		}

		return defaultVal;
	}

	public float getFloat (JsonObject config, String key, float defaultVal) {
		final JsonElement element = config.get(key);

		if (element instanceof JsonPrimitive primitive) {
			defaultVal = primitive.asFloat(defaultVal);
		}

		final Dynamic<Float> resolver = deserialize(Float.class, element, defaultVal);

		if (resolver != null) {
			return resolver.value();
		}

		return defaultVal;
	}

	public int getGlConst(JsonObject config, String key, String fallback) {
		final JsonElement element = config.get(key);
		final Dynamic<String> resolver = deserialize(String.class, element, fallback);

		if (resolver != null) {
			return GlSymbolLookup.lookup(resolver.value(), fallback);
		}

		return GlSymbolLookup.lookup(fallback);
	}

	private <ForType> Dynamic<ForType> deserialize(Class<ForType> clazz, JsonElement element, ForType defaultVal) {
		if (element instanceof JsonPrimitive primitive) {
			final Object value = primitive.getValue();

			if (clazz.isAssignableFrom(value.getClass())) {
				return new ConstantResolver<>(clazz.cast(value));
			} else if (value instanceof CharSequence stringVal) {
				return createResolver(clazz, stringVal.toString());
			}
		} else if (element instanceof JsonObject obj) {
			return deserializeMap(clazz, obj, defaultVal);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private <ForType> Dynamic<ForType> createResolver(Class<ForType> clazz, String optionKey) {
		if (clazz.equals(String.class)) {
			return (Dynamic<ForType>) new StringResolver(optionKey);
		}

		if (clazz.equals(Boolean.class)) {
			return (Dynamic<ForType>) new BooleanResolver(optionKey);
		}

		if (clazz.equals(Integer.class)) {
			return (Dynamic<ForType>) new IntResolver(optionKey);
		}

		if (clazz.equals(Float.class)) {
			return (Dynamic<ForType>) new FloatResolver(optionKey);
		}

		return null;
	}

	private <ForType> MapResolver<ForType> deserializeMap(Class<ForType> clazz, JsonObject obj, ForType defaultVal) {
		final ForType suppliedDefault = obj.get(clazz, "default");
		final JsonObject optionMap = obj.getObject("optionMap");

		if (optionMap != null) {
			final String optionKey = optionMap.get(String.class, "option");
			final JsonArray jsonMap = optionMap.get(JsonArray.class, "map");

			if (optionKey != null && jsonMap != null) {
				return new MapResolver<>(clazz, optionKey, jsonMap, suppliedDefault == null ? defaultVal : suppliedDefault);
			}
		}

		return null;
	}

	private abstract static class Dynamic<T> {
		public abstract T value();
	}

	private static class ConstantResolver<T> extends Dynamic<T> {
		private final T value;

		private ConstantResolver(T value) {
			this.value = value;
		}

		@Override
		public T value() {
			return value;
		}
	}

	private class MapResolver<ToType> extends Dynamic<ToType> {
		private final Dynamic<Object> source;
		private final Map<Object, ToType> map;
		private final ToType defaultVal;

		private MapResolver(Class<ToType> toType, String optionKey, JsonArray jsonMap, ToType defaultVal) {
			this.source = new WildcardResolver(optionKey);
			this.defaultVal = defaultVal;
			map = new Object2ObjectOpenHashMap<>(jsonMap.size());

			for (JsonElement element : jsonMap) {
				if (element instanceof JsonObject obj) {
					final var condition = obj.get(Object.class, "condition");
					final var value = obj.get(toType, "value");

					if (condition != null && value != null) {
						map.put(condition, value);
					}
				}
			}
		}

		@Override
		public ToType value() {
			return map.getOrDefault(source.value(), defaultVal);
		}
	}

	private class WildcardResolver extends Dynamic<Object> {
		private final String optionKey;

		private WildcardResolver(String optionKey) {
			this.optionKey = optionKey;
		}

		@Override
		public Object value() {
			var enumOption = ctx.enumConfigEntries.dependOn(optionKey);

			if (enumOption != null) {
				return enumOption.value();
			}

			var booleanOption = ctx.booleanConfigEntries.dependOn(optionKey);

			if (booleanOption != null) {
				return booleanOption.value();
			}

			var intOption = ctx.intConfigEntries.dependOn(optionKey);

			if (intOption != null) {
				return intOption.value();
			}

			var floatOption = ctx.floatConfigEntries.dependOn(optionKey);

			if (floatOption != null) {
				return floatOption.value();
			}

			return null;
		}
	}

	private class StringResolver extends Dynamic<String> {
		private final NamedDependency<EnumConfigEntry> optionEntry;

		private StringResolver(String option) {
			optionEntry = ctx.enumConfigEntries.dependOn(option);
		}

		@Override
		public String value() {
			final var option = optionEntry.value();

			if (option == null) {
				return null;
			} else {
				return option.value();
			}
		}
	}

	private class BooleanResolver extends Dynamic<Boolean> {
		private final NamedDependency<BooleanConfigEntry> optionEntry;

		private BooleanResolver(String option) {
			optionEntry = ctx.booleanConfigEntries.dependOn(option);
		}

		@Override
		public Boolean value() {
			final var option = optionEntry.value();

			if (option == null) {
				return null;
			} else {
				return option.value();
			}
		}
	}

	private class IntResolver extends Dynamic<Integer> {
		private final NamedDependency<IntConfigEntry> optionEntry;

		private IntResolver(String option) {
			optionEntry = ctx.intConfigEntries.dependOn(option);
		}

		@Override
		public Integer value() {
			final var option = optionEntry.value();

			if (option == null) {
				return null;
			} else {
				return option.value();
			}
		}
	}

	private class FloatResolver extends Dynamic<Float> {
		private final NamedDependency<FloatConfigEntry> optionEntry;

		private FloatResolver(String option) {
			optionEntry = ctx.floatConfigEntries.dependOn(option);
		}

		@Override
		public Float value() {
			final var option = optionEntry.value();

			if (option == null) {
				return null;
			} else {
				return option.value();
			}
		}
	}
}
