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

package grondag.canvas.config.builder;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.CommonComponents;

public class Toggle extends EnumButton<String> {
	Toggle(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultVal, @Nullable String tooltipKey) {
		super(key, wrap(getter), wrap(setter), wrap(defaultVal), values(), tooltipKey);
	}

	private static String[] values() {
		return new String[] {wrap(true), wrap(false)};
	}

	private static Supplier<String> wrap(Supplier<Boolean> getter) {
		return () -> wrap(getter.get());
	}

	private static Consumer<String> wrap(Consumer<Boolean> setter) {
		return (string) -> {
			if (string.equals(CommonComponents.GUI_YES.getString())) {
				setter.accept(true);
			} else {
				setter.accept(false);
			}
		};
	}

	private static String wrap(boolean val) {
		if (val) {
			return CommonComponents.GUI_YES.getString();
		} else {
			return CommonComponents.GUI_NO.getString();
		}
	}
}
