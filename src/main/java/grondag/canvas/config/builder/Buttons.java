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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class Buttons {
	/**
	 * Helps in deciphering button constructor params.
	 */
	public static Button create(int x, int y, int width, int height, Component message, Button.OnPress action) {
		return new Button(x, y, width, height, message, action);
	}

	public static class CustomButton extends Button {
		public CustomButton(int i, int j, int k, int l, Component component, OnPress onPress) {
			super(i, j, k, l, component, onPress);
		}

		public void renderTitle(PoseStack poseStack, int i, int j, float f) {
			final int l = this.active ? 16777215 : 10526880;
			@SuppressWarnings("resource")
			final Font font = Minecraft.getInstance().font;
			drawCenteredString(poseStack, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, l | Mth.ceil(this.alpha * 255.0F) << 24);
		}
	}

	public static class SidebarButton extends CustomButton {
		public SidebarButton(int x, int y, int width, int height, Component message, Button.OnPress action) {
			super(x, y, width, height, message, action);
		}

		@Override
		public void renderButton(PoseStack poseStack, int i, int j, float f) {
			if (this.isHoveredOrFocused()) {
				fill(poseStack, x, y, x + width, y + height - 3, 0x66FFFFFF);
			}

			hLine(poseStack, x, x + width - 1, y + height - 4, 0x99FFFFFF);

			renderTitle(poseStack, i, j, f);
		}
	}

	public static class MinimalistButton extends CustomButton {
		public MinimalistButton(int x, int y, int width, int height, Component message, Button.OnPress action) {
			super(x, y, width, height, message, action);
		}

		@Override
		public void renderButton(PoseStack poseStack, int i, int j, float f) {
			if (isHoveredOrFocused()) {
				fill(poseStack, x, y, x + width, y + height, 0x66FFFFFF);
			}

			hLine(poseStack, x, x + width - 1, y, 0x99FFFFFF);
			hLine(poseStack, x, x + width - 1, y + height - 1, 0x99FFFFFF);
			vLine(poseStack, x, y, y + height - 1, 0x99FFFFFF);
			vLine(poseStack, x + width - 1, y, y + height - 1, 0x99FFFFFF);

			renderTitle(poseStack, i, j, f);
		}
	}

	public static class BrowseButton extends Button {
		public BrowseButton(int x, int y, int width, int height, Component message, Button.OnPress action) {
			super(x, y, width, height, message, action);
		}

		@Override
		public void renderButton(PoseStack ps, int ii, int j, float f) {
			super.renderButton(ps, ii, j, f);
			final int boxW = getHeight();
			final int box = getHeight() / 2;

			// fill(ps, x + width - boxW / 2 - box / 2, y + boxW / 2 - box / 2, x + width - boxW / 2 + box / 2, y + height - boxW / 2 + box / 2, 0xFFFFFFFF);

			for (int i = 0; i < box / 2; i++) {
				drawArrowShadow(ps, i, x, y, boxW, box);
				drawArrow(ps, i, x, y, boxW, box);
			}
		}

		private void drawArrow(PoseStack ps, int i, int x, int y, int boxW, int box) {
			vLine(ps, x + width - boxW / 2 - box / 2 + i, y + boxW / 2 - box / 2 + i - 1, y + height - boxW / 2 + box / 2 - i - 2, 0xFFFFFFFF);
		}

		private void drawArrowShadow(PoseStack ps, int i, int x, int y, int boxW, int box) {
			vLine(ps, x + width - boxW / 2 - box / 2 + i, y + height - boxW / 2 + box / 2 - i - 3, y + height - boxW / 2 + box / 2 - i - 1, 0x99000000);
		}
	}
}
