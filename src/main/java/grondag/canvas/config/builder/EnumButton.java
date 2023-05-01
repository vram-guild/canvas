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

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class EnumButton<T> extends OptionItem<T> {
	private final T[] values;
	private int cycleIndex;
	private Button cycler;

	EnumButton(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, T[] values, @Nullable String tooltipKey) {
		super(key, getter, setter, defaultVal, tooltipKey);
		this.values = values;
		cycleIndex = search(getter.get(), 0);
	}

	@Override
	protected void doReset(AbstractButton button) {
		cycleIndex = search(defaultVal, 0);
		set();
	}

	@Override
	protected void createSetterWidget(int x, int y, int width, int height) {
		cycler = new Button(x, y, width - 2, height, fullLabel(), this::onUserCycle);
		add(cycler);
	}

	private void onUserCycle(AbstractButton button) {
		cycleIndex = (cycleIndex + 1) % values.length;
		set();
	}

	private void set() {
		setter.accept(values[cycleIndex]);
		cycler.setMessage(fullLabel());
	}

	private Component fullLabel() {
		return label(values[cycleIndex].toString().toUpperCase(Locale.ROOT));
	}

	private int search(Object key, int fallback) {
		for (int i = 0; i < values.length; i++) {
			if (key.equals(values[i])) {
				return i;
			}
		}

		return fallback;
	}
}
