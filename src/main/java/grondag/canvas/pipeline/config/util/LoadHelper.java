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
