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

		// player.glsl
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

		// view.glsl
		COMPAT.add(Pair.of("frx_cameraView()", "frx_cameraView"));
		COMPAT.add(Pair.of("frx_entityView()", "frx_entityView"));
		COMPAT.add(Pair.of("frx_cameraPos()", "frx_cameraPos"));
		COMPAT.add(Pair.of("frx_lastCameraPos()", "frx_lastCameraPos"));
		COMPAT.add(Pair.of("frx_modelToWorld()", "frx_modelToWorld"));
		COMPAT.add(Pair.of("frx_modelOriginWorldPos()", "frx_modelToWorld.xyz"));
		COMPAT.add(Pair.of("frx_modelToCamera()", "frx_modelToCamera"));
		COMPAT.add(Pair.of("frx_modelOriginType()", "_cvu_model_origin_type"));
		COMPAT.add(Pair.of("MODEL_ORIGIN_CAMERA", "0"));
		COMPAT.add(Pair.of("MODEL_ORIGIN_REGION", "1"));
		COMPAT.add(Pair.of("MODEL_ORIGIN_SCREEN", "2"));
		COMPAT.add(Pair.of("frx_isHand()", "frx_isHand"));
		COMPAT.add(Pair.of("frx_isGui()", "frx_isGui"));
		COMPAT.add(Pair.of("frx_guiViewProjectionMatrix()", "frx_guiViewProjectionMatrix"));
		COMPAT.add(Pair.of("frx_normalModelMatrix()", "frx_normalModelMatrix"));
		COMPAT.add(Pair.of("frx_viewMatrix()", "frx_viewMatrix"));
		COMPAT.add(Pair.of("frx_inverseViewMatrix()", "frx_inverseViewMatrix"));
		COMPAT.add(Pair.of("frx_lastViewMatrix()", "frx_lastViewMatrix"));
		COMPAT.add(Pair.of("frx_projectionMatrix()", "frx_projectionMatrix"));
		COMPAT.add(Pair.of("frx_lastProjectionMatrix()", "frx_lastProjectionMatrix"));
		COMPAT.add(Pair.of("frx_inverseProjectionMatrix()", "frx_inverseProjectionMatrix"));
		COMPAT.add(Pair.of("frx_viewProjectionMatrix()", "frx_viewProjectionMatrix"));
		COMPAT.add(Pair.of("frx_inverseViewProjectionMatrix()", "frx_inverseViewProjectionMatrix"));
		COMPAT.add(Pair.of("frx_lastViewProjectionMatrix()", "frx_lastViewProjectionMatrix"));
		COMPAT.add(Pair.of("frx_cleanProjectionMatrix()", "frx_cleanProjectionMatrix"));
		COMPAT.add(Pair.of("frx_lastCleanProjectionMatrix()", "frx_lastCleanProjectionMatrix"));
		COMPAT.add(Pair.of("frx_inverseCleanProjectionMatrix()", "frx_inverseCleanProjectionMatrix"));
		COMPAT.add(Pair.of("frx_cleanViewProjectionMatrix()", "frx_cleanViewProjectionMatrix"));
		COMPAT.add(Pair.of("frx_inverseCleanViewProjectionMatrix()", "frx_inverseCleanViewProjectionMatrix"));
		COMPAT.add(Pair.of("frx_lastCleanViewProjectionMatrix()", "frx_lastCleanViewProjectionMatrix"));
		COMPAT.add(Pair.of("frx_shadowViewMatrix()", "frx_shadowViewMatrix"));
		COMPAT.add(Pair.of("frx_inverseShadowViewMatrix()", "frx_inverseShadowViewMatrix"));
		COMPAT.add(Pair.of("frx_viewWidth()", "frx_viewWidth"));
		COMPAT.add(Pair.of("frx_viewHeight()", "frx_viewHeight"));
		COMPAT.add(Pair.of("frx_viewAspectRatio()", "frx_viewAspectRatio"));
		COMPAT.add(Pair.of("frx_viewBrightness()", "frx_viewBrightness"));
		COMPAT.add(Pair.of("TARGET_SOLID", "0"));
		COMPAT.add(Pair.of("TARGET_OUTLINE", "1"));
		COMPAT.add(Pair.of("TARGET_TRANSLUCENT", "2"));
		COMPAT.add(Pair.of("TARGET_PARTICLES", "3"));
		COMPAT.add(Pair.of("TARGET_WEATHER", "4"));
		COMPAT.add(Pair.of("TARGET_CLOUDS", "5"));
		COMPAT.add(Pair.of("TARGET_ENTITY", "6"));
		COMPAT.add(Pair.of("frx_renderTarget()", "_cvu_context[_CV_TARGET_INDEX]"));
		COMPAT.add(Pair.of("FRX_CAMERA_IN_FLUID", "22"));
		COMPAT.add(Pair.of("FRX_CAMERA_IN_WATER", "23"));
		COMPAT.add(Pair.of("FRX_CAMERA_IN_LAVA", "24"));
		COMPAT.add(Pair.of("frx_viewFlag", "frx_viewFlag"));
		COMPAT.add(Pair.of("frx_viewDistance()", "frx_viewDistance"));

		// world.glsl
		COMPAT.add(Pair.of("frx_renderSeconds()", "frx_renderSeconds"));
		COMPAT.add(Pair.of("frx_renderFrames()", "frx_renderFrames"));
		COMPAT.add(Pair.of("frx_worldDay()", "frx_worldDay"));
		COMPAT.add(Pair.of("frx_worldTime()", "frx_worldTime"));
		COMPAT.add(Pair.of("frx_moonSize()", "frx_moonSize"));
		COMPAT.add(Pair.of("frx_skyAngleRadians()", "frx_skyAngleRadians"));
		COMPAT.add(Pair.of("frx_skyLightVector()", "frx_skyLightVector"));
		COMPAT.add(Pair.of("frx_skyLightColor()", "frx_skyLightColor"));
		COMPAT.add(Pair.of("frx_skyLightIlluminance()", "frx_skyLightIlluminance"));
		COMPAT.add(Pair.of("frx_skyLightAtmosphericColor()", "frx_skyLightAtmosphericColor"));
		COMPAT.add(Pair.of("frx_skyLightTransitionFactor()", "frx_skyLightTransitionFactor"));
		COMPAT.add(Pair.of("frx_ambientIntensity()", "frx_ambientIntensity"));
		COMPAT.add(Pair.of("frx_emissiveColor()", "frx_emissiveColor"));
		COMPAT.add(Pair.of("frx_rainGradient()", "frx_rainGradient"));
		COMPAT.add(Pair.of("frx_thunderGradient()", "frx_thunderGradient"));
		COMPAT.add(Pair.of("frx_smoothedRainGradient()", "frx_smoothedRainGradient"));
		COMPAT.add(Pair.of("frx_vanillaClearColor()", "frx_vanillaClearColor"));
		COMPAT.add(Pair.of("FRX_WORLD_HAS_SKYLIGHT", "0"));
		COMPAT.add(Pair.of("FRX_WORLD_IS_OVERWORLD", "1"));
		COMPAT.add(Pair.of("FRX_WORLD_IS_NETHER", "2"));
		COMPAT.add(Pair.of("FRX_WORLD_IS_END", "3"));
		COMPAT.add(Pair.of("FRX_WORLD_IS_RAINING", "4"));
		COMPAT.add(Pair.of("FRX_WORLD_IS_THUNDERING", "5"));
		COMPAT.add(Pair.of("FRX_WORLD_IS_SKY_DARKENED", "6"));
		COMPAT.add(Pair.of("FRX_WORLD_IS_MOONLIT", "21"));
		COMPAT.add(Pair.of("frx_testCondition", "frx_testCondition"));
		COMPAT.add(Pair.of("frx_worldFlag", "frx_worldFlag"));
		COMPAT.add(Pair.of("frx_isThundering()", "(frx_worldIsThundering == 1)"));
		COMPAT.add(Pair.of("frx_isSkyDarkened()", "(frx_worldIsSkyDarkened == 1)"));
		COMPAT.add(Pair.of("frx_worldHasSkylight()", "(frx_worldHasSkylight == 1)"));
		COMPAT.add(Pair.of("frx_isWorldTheOverworld()", "(frx_worldIsOverworld == 1)"));
		COMPAT.add(Pair.of("frx_isWorldTheNether()", "(frx_worldIsNether == 1)"));
		COMPAT.add(Pair.of("frx_isWorldTheEnd()", "(frx_worldIsEnd == 1)"));
		COMPAT.add(Pair.of("frx_isRaining()", "(frx_worldIsRaining == 1)"));

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
