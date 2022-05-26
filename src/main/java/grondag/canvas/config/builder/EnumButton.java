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

import static grondag.canvas.config.builder.Checkbox.RESET_BUTTON_WIDTH;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import dev.lambdaurora.spruceui.option.SpruceOption;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.option.SpruceCyclingOption;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class EnumButton<T> extends SpruceCyclingOption implements Option<T> {
	private final T defaultVal;
	private final Supplier<T> getter;
	private final T[] values;
	private SpruceWidget resetButton;

	EnumButton(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, T[] values, @Nullable Component tooltip) {
		super(key, new EnumCycler<>(getter, setter, values), e -> new TextComponent(I18n.get(key) + ": §e" + getter.get().toString().toUpperCase(Locale.ROOT)), tooltip);
		this.getter = getter;
		this.values = values;
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		final SpruceButtonWidget cycler = (SpruceButtonWidget) super.createWidget(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH);
		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, cycler.getHeight(), Buttons.RESET, e -> {
			int i = search(values, defaultVal) - search(values, getter.get());
			this.cycle(((i - 1) + values.length) % values.length);
			cycler.onPress();
		});
		SpruceContainerWidget container = new SpruceContainerWidget(position, width, cycler.getHeight());
		container.addChild(cycler);
		container.addChild(resetButton);
		refreshResetButton();
		return container;
	}

	@Override
	public void refreshResetButton() {
		resetButton.setActive(!getter.get().equals(defaultVal));
	}

	@Override
	public SpruceOption spruceOption() {
		return this;
	}

	private record EnumCycler<T>(Supplier<T> getter, Consumer<T> setter, T[] values) implements Consumer<Integer> {
		@Override
		public void accept(Integer i) {
			final int current = search(values, getter.get());
			final int next = (current + i) % values.length;
			setter.accept(values[next]);
		}
	}

	private static int search(Object[] values, Object key) {
		for (int i = 0; i < values.length; i++) {
			if (key.equals(values[i])) {
				return i;
			}
		}

		return -1;
	}
}
