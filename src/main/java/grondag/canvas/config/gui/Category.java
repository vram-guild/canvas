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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

class Category extends ListItem {
	private static final String NULL_KEY = "";
	private static final Component NULL_TITLE = new TextComponent("");

	Category(String key) {
		super(key);
	}

	Category() {
		this(NULL_KEY);
	}

	@Override
	protected void createWidget(int x, int y, int width, int height) {
		add(new CategoryWidget(x, y, width, key.equals(NULL_KEY) ? NULL_TITLE : new TextComponent("§l" + I18n.get(key))));
	}

	static class CategoryWidget extends AbstractWidget {
		private final Minecraft client = Minecraft.getInstance();
		private final Component title;

		private CategoryWidget(int x, int y, int width, Component title) {
			super(x, y, width, 9, title);
			this.title = title;
		}

		@Override
		public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
			if (title != NULL_TITLE) {
				int titleWidth = this.client.font.width(this.title);
				int titleX = x + (this.getWidth() / 2 - titleWidth / 2);
				drawString(matrices, this.client.font, this.title, titleX, y, 0xffffffff);

				if (this.width > titleWidth) {
					fill(matrices, x, y + 4, titleX - 5, y + 6, 0x66ffffff);
					fill(matrices, titleX + titleWidth + 5, y + 4, x + this.getWidth(), y + 6, 0x66ffffff);
				}
			}
		}

		@Override
		public boolean mouseClicked(double d, double e, int i) {
			return false;
		}

		@Override
		public void updateNarration(NarrationElementOutput narrationElementOutput) {
			narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
		}
	}
}
