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

import java.util.function.BiFunction;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

public class LoadHelper {
	private LoadHelper() { }

	public static <T extends AbstractConfig> @Nullable T loadObject(ConfigContext ctx, JsonObject config, String key, BiFunction<ConfigContext, JsonObject, T> factory) {
		if (config == null || !config.containsKey(key)) {
			return null;
		}

		final JsonObject obj = config.getObject(key);

		if (obj == null || obj.isEmpty()) {
			return null;
		}

		return factory.apply(ctx, obj);
	}

	public static <T extends AbstractConfig> void loadSubList(ConfigContext ctx, JsonObject configJson, String key, String subKey, ObjectArrayList<T> list, BiFunction<ConfigContext, JsonObject, T> factory) {
		if (configJson == null || !configJson.containsKey(key)) {
			return;
		}

		final JsonObject listJson = configJson.getObject(key);

		if (listJson == null || !listJson.containsKey(subKey)) {
			return;
		}

		final JsonArray array = JanksonHelper.getJsonArrayOrNull(listJson, subKey,
				String.format("Error parsing pipeline config %s: %s must be an array.", key, subKey));

		if (array == null) {
			return;
		}

		final int limit = array.size();

		for (int i = 0; i < limit; ++i) {
			list.add(factory.apply(ctx, (JsonObject) array.get(i)));
		}
	}

	public static <T extends AbstractConfig> void loadList(ConfigContext ctx, JsonObject configJson, String key, ObjectArrayList<T> list, BiFunction<ConfigContext, JsonObject, T> factory) {
		if (configJson == null || !configJson.containsKey(key)) {
			return;
		}

		final JsonArray array = JanksonHelper.getJsonArrayOrNull(configJson, key,
				String.format("Error parsing pipeline config: %s must be an array.", key));

		if (array == null) {
			return;
		}

		final int limit = array.size();

		for (int i = 0; i < limit; ++i) {
			list.add(factory.apply(ctx, (JsonObject) array.get(i)));
		}
	}
}
