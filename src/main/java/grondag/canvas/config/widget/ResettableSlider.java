package grondag.canvas.config.widget;

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

import static grondag.canvas.config.widget.ResettableCheckbox.RESET_BUTTON_WIDTH;

public abstract class ResettableSlider extends SpruceDoubleOption implements ResettableOption<Double> {
	private double defaultVal;
	private SpruceWidget resetButton;

	ResettableSlider(String key, double min, double max, float step, Supplier<Double> getter, Consumer<Double> setter, double defaultVal, Function<SpruceDoubleOption, Component> displayStringGetter, @Nullable Component tooltip) {
		super(key, min, max, step, getter, setter, displayStringGetter, tooltip);
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		SpruceSliderWidget slider = (SpruceSliderWidget)super.createWidget(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH);
		// TO-DO Translatable
		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, slider.getHeight(), new TextComponent("Reset"), e -> {
			this.set(defaultVal);
			slider.setIntValue((int) (getRatio(defaultVal) * 100d));
		});
		SpruceContainerWidget container = new SpruceContainerWidget(position, width, slider.getHeight());
		container.addChild(slider);
		container.addChild(resetButton);
		refreshResetButton();
		return container;
	}

	@Override
	public void refreshResetButton() {
		resetButton.setActive(get() != defaultVal);
	}

	public static class IntSlider extends ResettableSlider {
		IntSlider(String key, int min, int max, int step, Supplier<Integer> getter, Consumer<Integer> setter, int defaultVal, @Nullable Component tooltip) {
			super(key, min, max, step, () -> getter.get().doubleValue(), d -> setter.accept(d.intValue()), defaultVal, e -> new TextComponent(I18n.get(key) + ": " + getter.get()), tooltip);
		}
	}

	public static class FloatSlider extends ResettableSlider {
		FloatSlider(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, float defaultVal, @Nullable Component tooltip) {
			super(key, min, max, step, () -> getter.get().doubleValue(), d -> setter.accept(d.floatValue()), defaultVal, e -> new TextComponent(String.format("%s: %.1f", I18n.get(key), getter.get())), tooltip);
		}
	}
}
