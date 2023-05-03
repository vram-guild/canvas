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

package grondag.canvas.shader.data;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class AccessibilityData {
	static final ResourceLocation id = new ResourceLocation("canvas:accessibility");

	static double fovEffects = 1.0;
	static double distortionEffects = 1.0;
	static boolean hideLightningFlashes = false;
	static double darknessPulsing = 1.0;

	static String cache = null;

	public static boolean checkChanged() {
		final var mc = Minecraft.getInstance();

		boolean valid = fovEffects == mc.options.fovEffectScale().get();
		valid = valid && distortionEffects == mc.options.screenEffectScale().get();
		valid = valid && hideLightningFlashes == mc.options.hideLightningFlash().get();
		valid = valid && darknessPulsing == mc.options.darknessEffectScale().get();

		if (!valid) {
			generateString();
		}

		return !valid;
	}

	private static void generateString() {
		final var mc = Minecraft.getInstance();

		fovEffects = mc.options.fovEffectScale().get();
		distortionEffects = mc.options.screenEffectScale().get();
		hideLightningFlashes = mc.options.hideLightningFlash().get();
		darknessPulsing = mc.options.darknessEffectScale().get();

		cache = "#define frx_fovEffects " + fovEffects + "\n"
				+ "#define frx_distortionEffects " + distortionEffects + "\n"
				+ "#define frx_hideLightningFlashes " + (hideLightningFlashes ? 1 : 0) + "\n"
				+ "#define frx_darknessPulsing " + darknessPulsing + "\n";
	}

	static String shaderSource() {
		if (cache == null) {
			generateString();
		}

		return cache;
	}
}
