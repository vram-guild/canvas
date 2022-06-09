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

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import grondag.canvas.config.gui.ListItem;

public abstract class OptionItem<T> extends ListItem implements Option {
	private static final int RESET_BUTTON_WIDTH = 48;
	private static final Component RESET = Component.translatable("config.canvas.reset");
	private Button resetButton;
	public static final Style VALUE_STYLE = Style.EMPTY.withFont(Style.DEFAULT_FONT).withColor(0xFFFF00);

	protected final Supplier<T> getter;
	protected final Consumer<T> setter;
	protected final T defaultVal;
	protected final String label;

	public OptionItem(String key, Supplier<T> getter, Consumer<T> setter, T defaultVal, @Nullable String tooltipKey) {
		super(key, tooltipKey);
		this.getter = getter;
		this.setter = setter;
		this.defaultVal = defaultVal;
		label = I18n.get(key);
	}

	protected Component label(String value) {
		return Component.literal(label + ": ").append(Component.literal(value).setStyle(VALUE_STYLE));
	}

	protected Component label() {
		return Component.literal(label);
	}

	@Override
	public final void refreshResetButton() {
		resetButton.active = resetActive();
	}

	protected final void createWidget(int x, int y, int width, int height) {
		resetButton = new Button(x + width - RESET_BUTTON_WIDTH, y, RESET_BUTTON_WIDTH, height, RESET, this::doReset);
		createSetterWidget(x, y, width - RESET_BUTTON_WIDTH, height);
		add(resetButton);
		refreshResetButton();
	}

	protected abstract void doReset(AbstractButton button);

	protected abstract void createSetterWidget(int x, int y, int width, int height);

	protected boolean resetActive() {
		return !defaultVal.equals(getter.get());
	}

	@Override
	public ListItem listItem() {
		return this;
	}
}

