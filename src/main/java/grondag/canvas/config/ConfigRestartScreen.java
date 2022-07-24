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

package grondag.canvas.config;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import grondag.canvas.config.gui.BaseScreen;

public class ConfigRestartScreen extends BaseScreen {
	private List<FormattedCharSequence> lines;

	public ConfigRestartScreen(Screen parent) {
		super(parent, Component.translatable("config.canvas.restart.title"));
	}

	@Override
	protected void init() {
		super.init();

		if (lines == null) {
			this.lines = this.font.split(Component.translatable("config.canvas.restart.prompt"), 320);
		}

		this.addRenderableWidget(new Button(this.width / 2 - 160 - 1, this.height / 2 - 100 + lines.size() * 16 + 60, 160 - 2, 20, Component.translatable("config.canvas.restart.accept"), b -> restart()));
		this.addRenderableWidget(new Button(this.width / 2 + 1, this.height / 2 - 100 + lines.size() * 16 + 60, 160 - 2, 20, Component.translatable("config.canvas.restart.ignore"), b -> onClose()));
	}

	private void restart() {
		this.minecraft.close();
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		super.render(poseStack, i, j, f);

		if (lines != null) {
			int row = 0;

			for (FormattedCharSequence line : lines) {
				drawCenteredString(poseStack, this.font, line, this.width / 2, this.height / 2 - 100 + 30 + 16 * (row++), 16777215);
			}
		}
	}
}
