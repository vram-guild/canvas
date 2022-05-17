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

import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import dev.lambdaurora.spruceui.option.SpruceOption;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.option.SpruceDoubleOption;
import dev.lambdaurora.spruceui.widget.SpruceSliderWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.widget.text.SpruceTextFieldWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public abstract class Slider<T> extends SpruceDoubleOption implements Option<T> {
	private static final DecimalFormat DECIMAL = new DecimalFormat("0.0###");
	private static final int INPUT_WIDTH = 56;

	private final double defaultVal;
	private SpruceWidget resetButton;

	Slider(String key, double min, double max, float step, Supplier<Double> getter, Consumer<Double> setter, double defaultVal, Function<SpruceDoubleOption, Component> displayStringGetter, @Nullable Component tooltip) {
		super(key, min, max, step, getter, setter, displayStringGetter, tooltip);
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		CustomSliderWidget slider = CustomSliderWidget.create(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH - INPUT_WIDTH - 2, this, step, new TranslatableComponent(key));
		this.getOptionTooltip().ifPresent(slider::setTooltip);

		CustomTextWidget text = new CustomTextWidget(Position.of(position, width - RESET_BUTTON_WIDTH - INPUT_WIDTH, 0), INPUT_WIDTH, this);

		text.linked = slider;
		slider.linked = text;

		final var _this = this;
		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, slider.getHeight(), Buttons.RESET, e -> {
			_this.set(defaultVal);
			slider.resetDisplay(false);
			// sometimes it's still focused, so forcing is required
			text.resetDisplay(true);
		});
		refreshResetButton();

		SpruceContainerWidget container = new SpruceContainerWidget(position, width, slider.getHeight());
		container.addChild(slider);
		container.addChild(text);
		container.addChild(resetButton);

		return container;
	}

	@Override
	public void refreshResetButton() {
		if (resetButton != null) resetButton.setActive(get() != defaultVal);
	}

	@Override
	public SpruceOption spruceOption() {
		return this;
	}

	public static class IntSlider extends Slider<Integer> {
		IntSlider(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable Component tooltip) {
			super(key, min, max, step, () -> getter.get().doubleValue(), d -> setter.accept(d.intValue()), defaultVal, e -> new TextComponent(getter.get().toString()), tooltip);
		}
	}

	public static class FloatSlider extends Slider<Float> {
		FloatSlider(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable Component tooltip) {
			super(key, min, max, step, () -> getter.get().doubleValue(), d -> setter.accept(d.floatValue()), defaultVal, e -> new TextComponent(DECIMAL.format(e.get())), tooltip);
		}
	}

	private interface LinkedWidget {
		void resetDisplay(boolean forced);
	}

	private static class CustomSliderWidget extends SpruceSliderWidget implements LinkedWidget {
		private final double multiplier;
		private final SpruceDoubleOption option;
		private LinkedWidget linked;

		private static CustomSliderWidget create(Position position, int width, SpruceDoubleOption option, double step, Component label) {
			// guard against integer overflow because why not
			final double multiplier = Math.min((option.getMax() - option.getMin()) / step, Math.floor(Integer.MAX_VALUE / option.getMax()));
			return new CustomSliderWidget(position, width, 20, option, multiplier, label);
		}

		private CustomSliderWidget(Position position, int width, int height, SpruceDoubleOption option, double multiplier, Component label) {
			super(position, width, height, TextComponent.EMPTY, option.getRatio(option.get()), slider -> option.set(option.getValue(slider.getValue())), multiplier, "");
			this.multiplier = multiplier;
			this.option = option;
			this.setMessage(label);
		}

		@Override
		public void resetDisplay(boolean forced) {
			final double realValue = option.get();
			// set approximate only due to API limitation. this can cause rounding error
			setIntValue((int) Math.round(option.getRatio(realValue) * multiplier));
			// prevent rounding error
			option.set(realValue);
		}

		@Override
		protected void updateMessage() {
			if (linked != null) linked.resetDisplay(false);
		}
	}

	private static class CustomTextWidget extends SpruceTextFieldWidget implements LinkedWidget {
		private final SpruceDoubleOption option;
		private boolean lastFocus = isFocused();
		private LinkedWidget linked;

		private CustomTextWidget(Position position, int width, SpruceDoubleOption optionSource) {
			super(position, width, 20, null);
			option = optionSource;
			resetDisplay(true);
			setTextPredicate(this::inputPredicate);
			setChangedListener(this::changedListener);
		}

		private boolean inputPredicate(String s) {
			try {
				if (s.equals("")) {
					return true;
				} else {
					final double parsed = Double.parseDouble(s);
					return parsed >= option.getMin() && parsed <= option.getMax();
				}
			} catch (Exception e) {
				return false;
			}
		}

		private void changedListener(String s) {
			try {
				double value = Double.parseDouble(s);

				if (!option.getDisplayString().getString().equals(getText())) {
					option.set(value);
					if (linked != null) linked.resetDisplay(false);
				}
			} catch (Exception ignored) {
				// NOOP
			}
		}

		@Override
		public void resetDisplay(boolean forced) {
			// only reset if not focused to prevent cursor glitches
			if (!isFocused() || forced) {
				final String display = option.getDisplayString().getString();
				// this can cause rounding error, but it will be the most precise value the user sees either way
				if (!display.equals(getText())) setText(display);
			}
		}

		@Override
		protected void renderWidget(PoseStack matrices, int mouseX, int mouseY, float delta) {
			if (lastFocus != isFocused()) {
				resetDisplay(false);
				lastFocus = isFocused();
			}

			super.renderWidget(matrices, mouseX, mouseY, delta);
		}
	}
}
