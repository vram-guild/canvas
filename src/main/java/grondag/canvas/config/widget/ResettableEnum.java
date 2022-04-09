package grondag.canvas.config.widget;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import dev.lambdaurora.spruceui.option.SpruceCyclingOption;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import static grondag.canvas.config.widget.ResettableCheckbox.RESET_BUTTON_WIDTH;

public class ResettableEnum<T> extends SpruceCyclingOption implements ResettableOption<T>  {
	private final T defaultVal;
	private final Supplier<T> getter;
	private final T[] values;
	private SpruceWidget resetButton;

	ResettableEnum(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, T[] values, @Nullable Component tooltip) {
		super(key, new EnumCycler<>(getter, setter, values), e -> new TextComponent(I18n.get(key) + ": §e" + getter.get().toString().toUpperCase(Locale.ROOT)), tooltip);
		this.getter = getter;
		this.values = values;
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		final SpruceButtonWidget cycler = (SpruceButtonWidget) super.createWidget(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH);
		// TO-DO Translatable
		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, cycler.getHeight(), new TextComponent("Reset"), e -> {
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

	private static class EnumCycler<T> implements Consumer<Integer> {
		private final Supplier<T> getter;
		private final Consumer<T> setter;
		private final T[] values;

		public EnumCycler(Supplier<T> getter, Consumer<T> setter, T[] values) {
			this.getter = getter;
			this.setter = setter;
			this.values = values;
		}

		@Override
		public void accept(Integer i) {
			final int current = search(values, getter.get());
			final int next = (current + i) % values.length;
			setter.accept(values[next]);
		}
	}

	private static int search(Object[] values, Object key) {
		for(int i = 0; i < values.length; i ++) {
			if (key.equals(values[i])) {
				return i;
			}
		}

		return  -1;
	}
}
