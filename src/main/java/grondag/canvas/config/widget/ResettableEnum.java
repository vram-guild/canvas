package grondag.canvas.config.widget;

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

public class ResettableEnum<T extends Enum<T>> extends SpruceCyclingOption implements ResettableOption<T>  {
	private final T defaultVal;
	private final Supplier<T> getter;
	private final T[] values;
	private SpruceWidget resetButton;

	ResettableEnum(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, Class<T> enumType, @Nullable Component tooltip) {
		super(key, new EnumCycler<>(getter, setter, enumType), e -> new TextComponent(I18n.get(key) + ": " + getter.get()), tooltip);
		this.getter = getter;
		this.values = enumType.getEnumConstants();
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		final SpruceButtonWidget cycler = (SpruceButtonWidget) super.createWidget(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH);
		// TO-DO Translatable
		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, cycler.getHeight(), new TextComponent("Reset"), e -> {
			int i = defaultVal.ordinal() - getter.get().ordinal();
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
		resetButton.setActive(getter.get() != defaultVal);
	}

	private static class EnumCycler<T extends Enum<T>> implements Consumer<Integer> {
		private final Supplier<T> getter;
		private final Consumer<T> setter;
		private final T[] values;

		public EnumCycler(Supplier<T> getter, Consumer<T> setter, Class<T> enumType) {
			this.getter = getter;
			this.setter = setter;
			this.values = enumType.getEnumConstants();
		}

		@Override
		public void accept(Integer i) {
			final int current = getter.get().ordinal();
			final int next = (current + i) % values.length;
			setter.accept(values[next]);
		}
	}
}
