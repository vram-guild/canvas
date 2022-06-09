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

package grondag.canvas.config.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ActionItem extends ListItem {
	private final ButtonFactory factory;
	private final Runnable action;
	private Button button;

	public ActionItem(String key, String tooltipKey, ButtonFactory factory, Runnable action) {
		super(key, tooltipKey);
		this.factory = factory;
		this.action = action;
	}

	public ActionItem(String key, ButtonFactory factory, Runnable action) {
		this(key, null, factory, action);
	}

	@Override
	protected void createWidget(int x, int y, int width, int height) {
		button = factory.createButton(x, y, width, height, Component.translatable(key), b -> action.run());

		add(button);
	}

	public interface ButtonFactory {
		Button createButton(int x, int y, int width, int height, Component component, Button.OnPress action);
	}
}
