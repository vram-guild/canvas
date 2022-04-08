package grondag.canvas.config.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.option.SpruceCheckboxBooleanOption;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceCheckboxWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class ResettableCheckbox extends SpruceCheckboxBooleanOption implements ResettableOption<Boolean> {
	public static final int RESET_BUTTON_WIDTH = 48;
	private final boolean defaultVal;
	private SpruceWidget resetButton;

	ResettableCheckbox(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultVal, @Nullable Component tooltip) {
		super(key, getter, setter, tooltip);
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		final SpruceCheckboxWidget checkbox = (SpruceCheckboxWidget) super.createWidget(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH);
		// TO-DO Translatable
		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, checkbox.getHeight(), new TextComponent("Reset"), e -> {
			if (this.get() != defaultVal) {
				checkbox.onPress();
			}
		});
		SpruceContainerWidget container = new SpruceContainerWidget(position, width, checkbox.getHeight());
		container.addChild(checkbox);
		container.addChild(resetButton);
		refreshResetButton();
		return container;
	}

	@Override
	public void refreshResetButton() {
		resetButton.setActive(get() != defaultVal);
	}
}
