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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class Checkbox extends BaseButton {
	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
	private boolean value;
	private final Runnable onValueChange;
	private boolean highlighted;

	public Checkbox(int x, int y, int width, int height, Component component, Runnable onValueChange) {
		super(x, y, width, height, component, ON_PRESS);
		this.onValueChange = onValueChange;
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}

	public boolean getValue() {
		return value;
	}

	public void changeValueSilently(boolean value) {
		this.value = value;
	}

	@Override
	public void renderWidget(PoseStack poseStack, int i, int j, float f) {
		final Minecraft minecraft = Minecraft.getInstance();
		final Font font = minecraft.font;
		final Component message;

		if (highlighted) {
			message = Component.literal("§l" + getMessage().getString() + "*");
		} else {
			message = getMessage();
		}

		RenderSystem.setShaderTexture(0, TEXTURE);
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		blit(poseStack, this.getX(), this.getY(), this.isFocused() ? 20.0F : 0.0F, this.value ? 20.0F : 0.0F, 20, this.height, 64, 64);
		// this.renderBg(poseStack, minecraft, i, j);
		drawString(poseStack, font, message, this.getX() + 24, this.getY() + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
	}

	private void toggleValue() {
		value = !value;
		onValueChange.run();
	}

	private static final OnPress ON_PRESS = button -> {
		if (button instanceof Checkbox checkbox) {
			checkbox.toggleValue();
		}
	};
}
