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

package grondag.canvas.shader;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.util.Identifier;

import grondag.canvas.CanvasMod;

public class PreReleaseShaderCompat {
	private static final ObjectArrayList<Pair<String, String>> COMPAT = new ObjectArrayList<>();
	private static final ObjectOpenHashSet<Identifier> WARNED = new ObjectOpenHashSet<>();

	static {
		// material.glsl
		COMPAT.add(Pair.of("frx_matEmissive()", "(frx_matEmissive == 1)"));
		COMPAT.add(Pair.of("frx_matEmissiveFactor()", "float(frx_matEmissive)"));
		COMPAT.add(Pair.of("frx_matCutout()", "(frx_matCutout == 1)"));
		COMPAT.add(Pair.of("frx_matCutoutFactor()", "float(frx_matCutout)"));
		COMPAT.add(Pair.of("frx_matUnmipped()", "(frx_matUnmipped == 1)"));
		COMPAT.add(Pair.of("frx_matUnmippedFactor()", "float(frx_matUnmipped)"));
		COMPAT.add(Pair.of("frx_matDisableAo()", "(frx_matDisableAo == 1)"));
		COMPAT.add(Pair.of("frx_matDisableDiffuse()", "(frx_matDisableDiffuse == 1)"));
		COMPAT.add(Pair.of("frx_matHurt()", "(frx_matHurt == 1)"));
		COMPAT.add(Pair.of("frx_matFlash()", "(frx_matFlash == 1)"));
		COMPAT.add(Pair.of("frx_matGlint()", "float(frx_matGlint)"));

		//fog.glsl
		COMPAT.add(Pair.of("frxFogStart", "frx_fogStart"));
		COMPAT.add(Pair.of("frxFogEnd", "frx_fogEnd"));
		COMPAT.add(Pair.of("frxFogColor", "frx_fogColor"));
		COMPAT.add(Pair.of("frxFogEnabled", "(frx_fogEnabled == 1)"));
		COMPAT.add(Pair.of("frx_fogStart()", "frx_fogStart"));
		COMPAT.add(Pair.of("frx_fogEnd()", "frx_fogEnd"));
		COMPAT.add(Pair.of("frx_fogColor()", "frx_fogColor"));
		COMPAT.add(Pair.of("frx_fogEnabled()", "(frx_fogEnabled == 1)"));

		COMPAT.add(Pair.of("frx_effectModifier()", "frx_effectModifier"));
		COMPAT.add(Pair.of("frx_heldLight()", "frx_heldLight"));
		COMPAT.add(Pair.of("frx_heldLightInnerRadius()", "frx_heldLightInnerRadius"));
		COMPAT.add(Pair.of("frx_heldLightOuterRadius()", "frx_heldLightOuterRadius"));
		COMPAT.add(Pair.of("frx_playerHasEffect", "frx_playerHasEffect"));
		COMPAT.add(Pair.of("frx_playerFlag", "frx_playerFlag"));
		COMPAT.add(Pair.of("frx_playerHasNightVision()", "(frx_effectNightVision == 1)"));
		COMPAT.add(Pair.of("frx_playerMood()", "frx_playerMood"));
		COMPAT.add(Pair.of("frx_eyePos()", "frx_eyePos"));
		COMPAT.add(Pair.of("frx_eyeBrightness()", "frx_eyeBrightness"));
		COMPAT.add(Pair.of("frx_smoothedEyeBrightness()", "frx_smoothedEyeBrightness"));

		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
		//COMPAT.add(Pair.of("", ""));
	}

	public static String compatify(String source, Identifier logPath) {
		// Don't update the API implemention files
		if (logPath.getNamespace().equals("frex") && logPath.getPath().equals("shaders/api/player.glsl")) {
			return source;
		}

		boolean found = false;

		for (final var p : COMPAT) {
			if (StringUtils.contains(source, p.getLeft())) {
				found = true;
				source = StringUtils.replace(source, p.getLeft(), p.getRight());
			}
		}

		if (found && WARNED.add(logPath)) {
			CanvasMod.LOG.warn("Shader " + logPath.toString() + " references obsolete pre-release API and should be updated.");
		}

		return source;
	}

	public static void reload() {
		WARNED.clear();
	}
}
