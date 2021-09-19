/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.pipeline;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.varia.GFX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.TextComponent;

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
	private static boolean[] isArray;

	private static int keyOption;

	private static boolean enabled = false;

	static void init(PipelineConfig config) {
		int imageCount = 0;

		for (final ImageConfig img : config.images) {
			imageCount += img.lod + 1 * img.depth;
		}

		glIds = new int[imageCount];
		lods = new int[imageCount];
		labels = new String[imageCount];
		layers = new int[imageCount];
		isDepth = new boolean[imageCount];
		isArray = new boolean[imageCount];

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
					isArray[i] = img.target == GFX.GL_TEXTURE_2D_ARRAY;
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
			Minecraft.getInstance().player.displayClientMessage(new TextComponent("Buffer Debug Mode Toggle: " + (enabled ? "ON" : "OFF")), true);
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
			Minecraft.getInstance().player.displayClientMessage(new TextComponent(labels[VIEWS[keyOption]]), true);
		}

		while (CanvasMod.DEBUG_NEXT.consumeClick()) {
			VIEWS[keyOption] = (VIEWS[keyOption] + viewCount + 1) % viewCount;
			Minecraft.getInstance().player.displayClientMessage(new TextComponent(labels[VIEWS[keyOption]]), true);
		}

		final int n = VIEWS[keyOption];
		PipelineManager.renderDebug(glIds[n], lods[n], layers[n], isDepth[n], isArray[n]);
	}

	public static void renderOverlay(PoseStack matrices, Font fontRenderer) {
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

			final int k = fontRenderer.width(string);
			final int m = 100 + 12 * i;
			GuiComponent.fill(matrices, 20, m - 1, 22 + k + 1, m + 9, backcolor);
			fontRenderer.draw(matrices, string, 21, m, forecolor);
		}
	}
}
