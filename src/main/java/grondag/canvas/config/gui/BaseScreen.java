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

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BaseScreen extends Screen {
	protected final Screen parent;

	protected BaseScreen(Screen parent, Component component) {
		super(component);
		this.parent = parent;
	}

	@Override
	public void render(GuiGraphics graphics, int i, int j, float f) {
		this.renderBackground(graphics);
		// graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
		super.render(graphics, i, j, f);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);
		renderTooltips(graphics, this.font, i, j);
	}

	protected void renderTooltips(GuiGraphics graphics, Font font, int i, int j) {
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
