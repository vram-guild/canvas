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

import java.text.DecimalFormat;
import java.text.Format;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class Slider<T> extends OptionItem<Double> {
	private static final DecimalFormat INT = new DecimalFormat("0");
	private static final DecimalFormat DECIMAL = new DecimalFormat("0.0###");
	private static final int INPUT_WIDTH = 60;

	public static class IntSlider extends Slider<Integer> {
		IntSlider(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable String tooltipKey) {
			super(key, min, max, step, INT, () -> getter.get().doubleValue(), d -> setter.accept(d.intValue()), defaultVal, tooltipKey);
		}

		@Override
		protected boolean isInteger() {
			return true;
		}
	}

	public static class FloatSlider extends Slider<Float> {
		FloatSlider(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable String tooltipKey) {
			super(key, min, max, step, DECIMAL, () -> getter.get().doubleValue(), d -> setter.accept(d.floatValue()), defaultVal, tooltipKey);
		}
	}

	private final double min;
	private final double max;
	private final float step;
	private final Format format;
	private SliderWidget slider;
	private InputWidget input;

	Slider(String key, double min, double max, float step, Format format, Supplier<Double> getter, Consumer<Double> setter, double defaultVal, @Nullable String tooltipKey) {
		super(key, getter, setter, defaultVal, tooltipKey);
		this.min = min;
		this.max = max;
		this.step = step;
		this.format = format;
	}

	@Override
	protected void doReset(AbstractButton button) {
		setter.accept(defaultVal);
		displaySlider();
		displayEdit();
	}

	protected boolean isInteger() {
		return false;
	}

	@Override
	protected void createSetterWidget(int x, int y, int width, int height) {
		slider = new SliderWidget(x, y, width - INPUT_WIDTH, height, label(), sliderRatio(getter.get()), this::applySlider);
		input = new InputWidget(x + width - INPUT_WIDTH, y, INPUT_WIDTH, height, label(), isInteger(), min, max, this::applyInput);
		displayEdit();
		add(slider);
		add(input);
	}

	private void applyInput() {
		if (input != null) {
			try {
				setter.accept(Double.parseDouble(input.getValue()));
			} catch (Throwable e) {
				// NOOP
			}

			displaySlider();
		}
	}

	private void applySlider() {
		if (slider != null) {
			setter.accept(sliderValue(slider.getRatio()));
			displayEdit();
		}
	}

	private void displaySlider() {
		if (slider != null) {
			slider.setRatio(sliderRatio(getter.get()));
		}
	}

	private double sliderRatio(double value) {
		return (value - min) / (max - min);
	}

	private double sliderValue(double ratio) {
		return roundToStep(ratio * (max - min) + min);
	}

	private double roundToStep(double rawValue) {
		return Math.round(rawValue / step) * step;
	}

	private void displayEdit() {
		if (input != null) {
			input.setText(format.format(getter.get()));
		}
	}

	private static class SliderWidget extends AbstractSliderButton {
		private final Runnable applyAction;

		private SliderWidget(int x, int y, int width, int height, Component message, double defaultRatio, Runnable action) {
			super(x, y, width, height, message, defaultRatio);
			this.applyAction = action;
		}

		@Override
		protected final void updateMessage() {
			// We do this on the slider option
		}

		private void setRatio(double ratio) {
			this.value = ratio;
		}

		private double getRatio() {
			return this.value;
		}

		@Override
		protected final void applyValue() {
			applyAction.run();
		}
	}

	private static class InputWidget extends EditBox implements Consumer<String>, Predicate<String> {
		private final boolean integer;
		private final Runnable changeAction;
		private final double min;
		private final double max;
		private boolean suppressListener = false;

		private InputWidget(int x, int y, int w, int h, Component message, boolean integer, double min, double max, Runnable changeAction) {
			super(Minecraft.getInstance().font, x + 2, y, w - 4, h, message);
			this.integer = integer;
			this.changeAction = changeAction;
			this.min = min;
			this.max = max;
			setFilter(this);
			setResponder(this);
		}

		private void setText(String text) {
			suppressListener = true;
			setValue(text);
			suppressListener = false;
		}

		@Override
		public void accept(String s) {
			if (!suppressListener) {
				changeAction.run();
			}
		}

		@Override
		public boolean test(String s) {
			if (s.equals("")) {
				return true;
			}

			try {
				double value;

				if (integer) {
					value = Integer.parseInt(s);
				} else {
					value = Double.parseDouble(s);
				}

				return value >= min && value <= max;
			} catch (Throwable e) {
				return false;
			}
		}
	}
}
