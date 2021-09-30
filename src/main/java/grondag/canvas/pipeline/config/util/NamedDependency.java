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

import org.jetbrains.annotations.Nullable;

import grondag.canvas.CanvasMod;

public class NamedDependency<T extends NamedConfig<T>> {
	private final NamedDependencyMap<T> map;
	public final String name;

	protected NamedDependency(NamedDependencyMap<T> map, String name) {
		this.map = map;
		this.name = name;
	}

	public boolean isValid() {
		return map.isValidReference(name);
	}

	public boolean isBuiltIn() {
		return map.isBuiltInReference(name);
	}

	public @Nullable T value() {
		return map.get(name);
	}

	public boolean validate(String msg, Object... args) {
		if (!isValid()) {
			CanvasMod.LOG.warn(String.format(msg, args));
			return false;
		} else {
			return true;
		}
	}
}
