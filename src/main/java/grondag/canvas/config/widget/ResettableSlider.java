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

import static grondag.canvas.config.widget.ResettableCheckbox.RESET_BUTTON_WIDTH;

import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.option.SpruceDoubleOption;
import dev.lambdaurora.spruceui.widget.SpruceSliderWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public abstract class ResettableSlider<T> extends SpruceDoubleOption implements ResettableOption<T> {
	private static final DecimalFormat DECIMAL = new DecimalFormat("0.0###");

	private final double defaultVal;
	private SpruceWidget resetButton;

	ResettableSlider(String key, double min, double max, float step, Supplier<Double> getter, Consumer<Double> setter, double defaultVal, Function<SpruceDoubleOption, Component> displayStringGetter, @Nullable Component tooltip) {
		super(key, min, max, step, getter, setter, displayStringGetter, tooltip);
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		CustomSliderWidget slider = CustomSliderWidget.create(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH, this, step);
		this.getOptionTooltip().ifPresent(slider::setTooltip);

		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, slider.getHeight(), Buttons.RESET, e -> slider.setValue(defaultVal));
		refreshResetButton();

		SpruceContainerWidget container = new SpruceContainerWidget(position, width, slider.getHeight());
		container.addChild(slider);
		container.addChild(resetButton);

		return container;
	}

	@Override
	public void refreshResetButton() {
		resetButton.setActive(get() != defaultVal);
	}

	public static class IntSlider extends ResettableSlider<Integer> {
		IntSlider(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable Component tooltip) {
			super(key, min, max, step, () -> getter.get().doubleValue(), d -> setter.accept(d.intValue()), defaultVal, e -> new TextComponent(I18n.get(key) + ": §b" + getter.get()), tooltip);
		}
	}

	public static class FloatSlider extends ResettableSlider<Float> {
		FloatSlider(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable Component tooltip) {
			super(key, min, max, step, () -> getter.get().doubleValue(), d -> setter.accept(d.floatValue()), defaultVal, e -> new TextComponent(I18n.get(key) + ": §b" + DECIMAL.format(getter.get())), tooltip);
		}
	}

	private static class CustomSliderWidget extends SpruceSliderWidget {
		private final double multiplier;
		private final SpruceDoubleOption option;

		private static CustomSliderWidget create(Position position, int width, SpruceDoubleOption option, double step) {
			// guard against integer overflow because why not
			final double multiplier = Math.min((option.getMax() - option.getMin()) / step, Math.floor(Integer.MAX_VALUE / option.getMax()));
			return new CustomSliderWidget(position, width, 20, option, multiplier);
		}

		private CustomSliderWidget(Position position, int width, int height, SpruceDoubleOption option, double multiplier) {
			super(position, width, height, TextComponent.EMPTY, option.getRatio(option.get()), slider -> option.set(option.getValue(slider.getValue())), multiplier, "");
			this.multiplier = multiplier;
			this.option = option;
			this.updateMessage();
		}

		private void setValue(double value) {
			// set approximate for visuals. can't set directly due to API limitation
			setIntValue((int) Math.round(option.getRatio(value) * multiplier));

			// set accurate
			if (getValue() != value) {
				option.set(value);
				updateMessage();
			}
		}

		@Override
		protected void updateMessage() {
			if (this.option != null) {
				this.setMessage(this.option.getDisplayString());
			}
		}
	}
}
