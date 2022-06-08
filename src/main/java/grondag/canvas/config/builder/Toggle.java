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

import grondag.canvas.config.gui.Checkbox;

public class Toggle extends OptionItem<Boolean> implements Runnable {
	private Checkbox checkbox;

	public Toggle(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, Boolean defaultVal, @Nullable String tooltipKey) {
		super(key, getter, setter, defaultVal, tooltipKey);
	}

	@Override
	protected void doReset(AbstractButton button) {
		setter.accept(defaultVal);

		if (checkbox != null) {
			checkbox.changeValueSilently(defaultVal);
		}
	}

	@Override
	protected void createSetterWidget(int x, int y, int width, int height) {
		checkbox = new Checkbox(x, y, width, height, label(), this);
		checkbox.changeValueSilently(getter.get());
		add(checkbox);
	}

	@Override
	public void run() {
		setter.accept(checkbox.getValue());
	}
}
