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

import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;

public class ConfigRestartScreen extends SpruceScreen {
	private List<FormattedCharSequence> lines;
	private final Screen parent;

	public ConfigRestartScreen(Screen parent) {
		// TO-DO Translatable
		super(new TextComponent("Restart Required"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		if (lines == null) {
			// TO-DO Translatable
			this.lines = this.font.split(new TextComponent("One of your changes requires restarting Minecraft. Would you like to proceed?"), 320);
		}

		// TO-DO Translatable
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 - 160 - 1, this.height / 2 - 100 + lines.size() * 16 + 60), 160 - 2, 20, new TextComponent("Exit Minecraft"), b -> restart()));
		this.addWidget(new SpruceButtonWidget(Position.of(this.width / 2 + 1, this.height / 2 - 100 + lines.size() * 16 + 60), 160 - 2, 20, new TextComponent("Ignore Restart"), b -> close()));
	}

	private void restart() {
		this.minecraft.close();
	}

	private void close() {
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public void renderTitle(PoseStack matrices, int mouseX, int mouseY, float delta) {
		if (lines != null) {
			drawCenteredString(matrices, this.font, this.title, this.width / 2, this.height / 2 - 100, 16777215);

			int i = 0;

			for (FormattedCharSequence line : lines) {
				drawCenteredString(matrices, this.font, line, this.width / 2, this.height / 2 - 100 + 30 + 16 * (i++), 16777215);
			}
		}
	}
}
