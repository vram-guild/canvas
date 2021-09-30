/*
 * Copyright © Contributing Authors
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

import java.util.function.Predicate;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@SuppressWarnings("serial")
public class NamedDependencyMap<T extends NamedConfig<T>> extends Object2ObjectOpenHashMap<String, T> {
	public final Predicate<String> builtInTest;

	public NamedDependencyMap(Predicate<String> builtInTest) {
		this.builtInTest = builtInTest;
	}

	public NamedDependencyMap() {
		this(Predicates.alwaysFalse());
	}

	public NamedDependency<T> dependOn(String name) {
		return new NamedDependency<>(this, name);
	}

	public NamedDependency<T> dependOn(JsonElement json) {
		return dependOn(JanksonHelper.asString(json));
	}

	public NamedDependency<T> dependOn(JsonObject json, String key) {
		return dependOn(json.get(key));
	}

	public NamedDependency<T> dependOn(JsonObject json, String key, String defaultName) {
		return dependOn(JanksonHelper.asStringOrDefault(json.get(key), defaultName));
	}

	public boolean isValidReference(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}

		if (builtInTest.test(name)) {
			return true;
		}

		final T val = get(name);
		return val != null && val.validate();
	}

	public boolean isBuiltInReference(String name) {
		return builtInTest.test(name);
	}
}
