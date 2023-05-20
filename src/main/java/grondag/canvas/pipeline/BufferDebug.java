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

package grondag.canvas.pipeline;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.varia.GFX;

public class BufferDebug {
	private static final int NONE = 0;
	private static final int SHIFT = 1;
	private static final int ALT = 2;
	private static final int MENU = 3;
	private static final int CTL = 4;

	private static final String[] PREFIX = {"none: ", "shift: ", "alt: ", "menu: ", "ctl: "};

	private static int[] VIEWS = new int[5];

	private static int viewCount;
	private static int[] glIds;
	private static int[] lods;
	private static int[] layers;
	private static String[] labels;
	private static boolean[] isDepth;
	private static int[] targets;

	private static int keyOption;

	private static boolean enabled = false;

	static void init(PipelineConfig config) {
		int imageCount = 0;

		for (final ImageConfig img : config.images) {
			imageCount += (img.lod + 1) * img.depth;
		}

		glIds = new int[imageCount];
		lods = new int[imageCount];
		labels = new String[imageCount];
		layers = new int[imageCount];
		isDepth = new boolean[imageCount];
		targets = new int[imageCount];

		int i = 0;

		for (final ImageConfig img : config.images) {
			final int glId = Pipeline.getImage(img.name).glId();

			for (int lod = 0; lod <= img.lod; ++lod) {
				for (int layer = 0; layer < img.depth; ++layer) {
					labels[i] = img.name + " lod=" + lod + " layer=" + layer;
					glIds[i] = glId;
					lods[i] = lod;
					layers[i] = layer;
					isDepth[i] = img.pixelFormat == GFX.GL_DEPTH_COMPONENT;
					targets[i] = img.target;
					++i;
				}
			}
		}

		if (viewCount != imageCount) {
			viewCount = imageCount;
			VIEWS[NONE] = 0;
			VIEWS[SHIFT] = Math.min(1, imageCount - 1);
			VIEWS[ALT] = Math.min(2, imageCount - 1);
			VIEWS[CTL] = Math.min(3, imageCount - 1);
			VIEWS[MENU] = Math.min(4, imageCount - 1);
		}
	}

	/**
	 * Don't call unless enabled - doesn't check.
	 */
	@SuppressWarnings("resource")
	public static void render() {
		while (CanvasMod.DEBUG_TOGGLE.consumeClick()) {
			enabled = !enabled;
			Minecraft.getInstance().player.displayClientMessage(Component.literal("Buffer Debug Mode Toggle: " + (enabled ? "ON" : "OFF")), true);
		}

		if (!enabled) {
			return;
		}

		final long handle = Minecraft.getInstance().getWindow().getWindow();

		if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
			keyOption = SHIFT;
		} else if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_ALT)) {
			keyOption = ALT;
		} else if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
			keyOption = CTL;
		} else if (InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_LEFT_SUPER) || InputConstants.isKeyDown(handle, GLFW.GLFW_KEY_RIGHT_SUPER)) {
			keyOption = MENU;
		} else {
			keyOption = NONE;
		}

		while (CanvasMod.DEBUG_PREV.consumeClick()) {
			VIEWS[keyOption] = (VIEWS[keyOption] + viewCount - 1) % viewCount;
			Minecraft.getInstance().player.displayClientMessage(Component.literal(labels[VIEWS[keyOption]]), true);
		}

		while (CanvasMod.DEBUG_NEXT.consumeClick()) {
			VIEWS[keyOption] = (VIEWS[keyOption] + viewCount + 1) % viewCount;
			Minecraft.getInstance().player.displayClientMessage(Component.literal(labels[VIEWS[keyOption]]), true);
		}

		final int n = VIEWS[keyOption];
		PipelineManager.renderDebug(glIds[n], lods[n], layers[n], isDepth[n], targets[n]);
	}

	public static void renderOverlay(GuiGraphics graphics, Font font) {
		if (!enabled || !Configurator.enableBufferDebug) {
			return;
		}

		for (int i = 0; i < 5; ++i) {
			String string = PREFIX[i] + labels[VIEWS[i]];
			int forecolor = 0xC0C0C0;
			int backcolor = 0x60606060;

			if (i == keyOption) {
				forecolor = 0xFFFF80;
				backcolor = 0xFF000000;
				string += " (selected)";
			}

			final int k = font.width(string);
			final int m = 100 + 12 * i;
			graphics.fill(20, m - 1, 22 + k + 1, m + 9, backcolor);
			graphics.drawString(font, string, 21, m, forecolor);
		}
	}
}
