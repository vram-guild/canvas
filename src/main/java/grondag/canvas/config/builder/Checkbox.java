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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import dev.lambdaurora.spruceui.option.SpruceOption;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.option.SpruceCheckboxBooleanOption;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import dev.lambdaurora.spruceui.widget.SpruceCheckboxWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceContainerWidget;

import net.minecraft.network.chat.Component;

public class Checkbox extends SpruceCheckboxBooleanOption implements Option<Boolean> {
	public static final int RESET_BUTTON_WIDTH = 48;
	private final boolean defaultVal;
	private SpruceWidget resetButton;

	Checkbox(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultVal, @Nullable Component tooltip) {
		super(key, getter, setter, tooltip);
		this.defaultVal = defaultVal;
	}

	@Override
	public SpruceWidget createWidget(Position position, int width) {
		final SpruceCheckboxWidget checkbox = (SpruceCheckboxWidget) super.createWidget(Position.of(position, 0, 0), width - RESET_BUTTON_WIDTH);
		resetButton = new SpruceButtonWidget(Position.of(position, width - RESET_BUTTON_WIDTH + 2, 0), RESET_BUTTON_WIDTH - 2, checkbox.getHeight(), Buttons.RESET, e -> {
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

	@Override
	public SpruceOption spruceOption() {
		return this;
	}
}
