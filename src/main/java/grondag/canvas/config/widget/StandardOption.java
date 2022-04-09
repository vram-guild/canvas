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

package grondag.canvas.config.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import dev.lambdaurora.spruceui.option.SpruceOption;

import net.minecraft.network.chat.Component;

public class StandardOption {
	private static class ExpandedSetter<T> implements Consumer<T> {
		private ResettableOption<T> option;
		private final Consumer<T> setter;

		private ExpandedSetter(Consumer<T> setter) {
			this.setter = setter;
		}

		private void attach(ResettableOption<T> option) {
			this.option = option;
		}

		@Override
		public void accept(T t) {
			setter.accept(t);
			option.refreshResetButton();
		}
	}

	public static SpruceOption booleanOption(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableCheckbox(key, getter, expandedSetter, defaultVal, tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static <T extends Enum<T>> SpruceOption enumOption(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, Class<T> enumType, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableEnum<T>(key, getter, expandedSetter, defaultVal, enumType.getEnumConstants(), tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static <T> SpruceOption enumOption(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, T[] values, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableEnum<T>(key, getter, expandedSetter, defaultVal, values, tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static SpruceOption intOption(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableSlider.IntSlider(key, min, max, step, getter, expandedSetter, defaultVal, tooltip);
		expandedSetter.attach(created);
		return created;
	}

	public static SpruceOption floatOption(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable Component tooltip) {
		final var expandedSetter = new ExpandedSetter<>(setter);
		final var created = new ResettableSlider.FloatSlider(key, min, max, step, getter, expandedSetter, defaultVal, tooltip);
		expandedSetter.attach(created);
		return created;
	}
}
