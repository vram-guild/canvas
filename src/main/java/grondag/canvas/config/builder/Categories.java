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

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.option.SpruceOption;
import dev.lambdaurora.spruceui.util.ColorUtil;
import dev.lambdaurora.spruceui.widget.AbstractSpruceWidget;
import dev.lambdaurora.spruceui.widget.SpruceWidget;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class Categories {
	/**
	 * Adds a category separator with prior padding except for the first category.
	 *
	 * @param list the option list
	 * @param key the translation key for the category title
	 * @return entry index of the category separator
	 */
	public static int addTo(SpruceOptionListWidget list, String key) {
		if (list.children().size() > 0) {
			list.addSingleOptionEntry(new CategoryOption());
		}

		return list.addSingleOptionEntry(new CategoryOption(key));
	}

	private static class CategoryOption extends SpruceOption {
		private static final String NULL_KEY = "";

		private CategoryOption(String key) {
			super(key);
		}

		private CategoryOption() {
			this(NULL_KEY);
		}

		@Override
		public SpruceWidget createWidget(Position position, int width) {
			return new CategoryWidget(position, width, key.equals(NULL_KEY) ? null : new TextComponent("§l" + I18n.get(key)));
		}
	}

	private static class CategoryWidget extends AbstractSpruceWidget {
		private final Minecraft client = Minecraft.getInstance();
		private final Component title;

		private CategoryWidget(Position position, int width, Component title) {
			super(position);
			this.width = width;
			this.height = 9;
			this.title = title;
		}

		@Override
		protected void renderWidget(PoseStack matrices, int mouseX, int mouseY, float delta) {
			if (title != null) {
				int titleWidth = this.client.font.width(this.title);
				int titleX = this.getX() + (this.getWidth() / 2 - titleWidth / 2);
				drawString(matrices, this.client.font, this.title, titleX, this.getY(), ColorUtil.WHITE);

				if (this.width > titleWidth) {
					fill(matrices, this.getX(), this.getY() + 4, titleX - 5, this.getY() + 6, 0x66ffffff);
					fill(matrices, titleX + titleWidth + 5, this.getY() + 4, this.getX() + this.getWidth(), this.getY() + 6, 0x66ffffff);
				}
			}
		}

		@Override
		protected Component getNarrationMessage() {
			return title;
		}
	}
}
