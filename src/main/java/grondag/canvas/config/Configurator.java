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

import static grondag.canvas.config.ConfigManager.DEFAULTS;

import net.minecraft.util.Mth;

import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.render.world.SkyShadowRenderer;
import grondag.canvas.terrain.occlusion.TerrainIterator;

public class Configurator {
	public static String pipelineId = DEFAULTS.pipelineId;
	public static boolean blendFluidColors = DEFAULTS.blendFluidColors;
	public static boolean wavyGrass = DEFAULTS.wavyGrass;
	public static boolean disableVignette = DEFAULTS.disableVignette;
	// public static boolean lightmapNoise = DEFAULTS.lightmapNoise;
	public static boolean lightSmoothing = DEFAULTS.lightSmoothing;
	//public static boolean moreLightmap = DEFAULTS.moreLightmap;
	//public static int maxLightmapDelayFrames = DEFAULTS.maxLightmapDelayFrames;
	public static boolean semiFlatLighting = DEFAULTS.semiFlatLighting;
	public static boolean coloredLights = DEFAULTS.coloredLights;

	public static boolean preventDepthFighting = DEFAULTS.preventDepthFighting;
	public static boolean clampExteriorVertices = DEFAULTS.clampExteriorVertices;
	public static boolean fixLuminousBlockShading = DEFAULTS.fixLuminousBlockShading;
	public static boolean advancedTerrainCulling = DEFAULTS.advancedTerrainCulling;
	public static boolean terrainSetupOffThread = DEFAULTS.terrainSetupOffThread;
	public static boolean cullEntityRender = DEFAULTS.cullEntityRender;
	public static boolean greedyRenderThread = DEFAULTS.greedyRenderThread;
	public static boolean forceJmxModelLoading = DEFAULTS.forceJmxModelLoading;
	public static StartupFinalBoolean reduceResolutionOnMac = new StartupFinalBoolean(DEFAULTS.reduceResolutionOnMac);
	public static int staticFrustumPadding = DEFAULTS.staticFrustumPadding;
	public static int dynamicFrustumPadding = DEFAULTS.dynamicFrustumPadding;
	public static boolean cullParticles = DEFAULTS.cullParticles;
	public static StartupFinalBoolean useCombinedThreadPool = new StartupFinalBoolean(DEFAULTS.useCombinedThreadPool);
	public static boolean shaderDebug = DEFAULTS.shaderDebug;
	public static boolean preprocessShaderSource = DEFAULTS.preprocessShaderSource;
	// public static boolean lightmapDebug = DEFAULTS.lightmapDebug;
	public static TerrainIterator.ShadowPriming shadowPrimingStrategy = DEFAULTS.shadowPrimingStrategy;
	// TODO: TEMPORARY, make into pipeline configuration -> this could be the 0th cascade distance
	public static int shadowMaxDistance = DEFAULTS.shadowMaxDistance;
	// EXPERIMENTAL: for now only BACK and NONE are good options btw
	public static SkyShadowRenderer.Culling shadowFaceCulling = DEFAULTS.shadowFaceCulling;
	// EXPERIMENTAL: interpolates shadow center between camera and camera frustum centroid
	// NB: Cascade sometimes fail with fully centered shadow because of depth clipping, despite the next cascade seemingly
	//     having the shadow information. This is possibly fixable by improving the cascade picker during sampling.
	public static float shadowCenterFactor = DEFAULTS.shadowCenterFactor;
	// WIP: Temporary workaround, remove soon
	public static boolean disableShadowSelfOcclusion = DEFAULTS.disableShadowSelfOcclusion;
	public static boolean conciseErrors = DEFAULTS.conciseErrors;
	public static boolean logMachineInfo = DEFAULTS.logMachineInfo;
	public static boolean logGlStateChanges = DEFAULTS.logGlStateChanges;
	public static StartupFinalBoolean debugNativeMemoryAllocation = new StartupFinalBoolean(DEFAULTS.debugNativeMemoryAllocation);
	public static StartupFinalBoolean safeNativeMemoryAllocation = new StartupFinalBoolean(DEFAULTS.safeNativeMemoryAllocation);
	public static boolean enablePerformanceTrace = DEFAULTS.enablePerformanceTrace;
	public static boolean debugOcclusionRaster = DEFAULTS.debugOcclusionRaster;
	public static boolean debugOcclusionBoxes = DEFAULTS.debugOcclusionBoxes;
	public static boolean renderWhiteGlassAsOccluder = DEFAULTS.renderWhiteGlassAsOccluder;
	public static boolean traceOcclusionEdgeCases = DEFAULTS.traceOcclusionEdgeCases;
	public static boolean enableBufferDebug = DEFAULTS.enableBufferDebug;
	public static boolean enableLifeCycleDebug = DEFAULTS.enableLifeCycleDebug;
	public static boolean logMissingUniforms = DEFAULTS.logMissingUniforms;
	public static boolean logMaterials = DEFAULTS.logMaterials;
	public static boolean logRenderLagSpikes = DEFAULTS.logRenderLagSpikes;
	public static int renderLagSpikeFps = DEFAULTS.renderLagSpikeFps;
	public static boolean displayRenderProfiler = DEFAULTS.displayRenderProfiler;
	public static Timekeeper.Mode profilerDisplayMode = DEFAULTS.profilerDisplayMode;
	public static int profilerDetailLevel = DEFAULTS.profilerDetailLevel;
	public static float profilerOverlayScale = DEFAULTS.profilerOverlayScale;
	public static boolean enableNearOccluders = DEFAULTS.enableNearOccluders;
	public static TransferBuffers.Config transferBufferMode = DEFAULTS.transferBufferMode;
	public static boolean steadyDebugScreen = DEFAULTS.steadyDebugScreen;
	public static boolean disableUnseenSpriteAnimation = DEFAULTS.disableUnseenSpriteAnimation;
	public static boolean cullBackfacingTerrain = DEFAULTS.cullBackfacingTerrain;
	public static boolean debugSpriteAtlas = DEFAULTS.debugSpriteAtlas;
	public static boolean traceTextureLoad = DEFAULTS.traceTextureLoad;

	//    @LangKey("config.acuity_fancy_fluids")
	//    @Comment({"Enable fancy water and lava rendering.",
	//        " This feature is currently work in progress and has no visible effect if enabled."})
	public static boolean fancyFluids = false;

	// static boolean hdLightmaps = DEFAULTS.hdLightmaps;
	public static boolean hdLightmaps() {
		return false;
	}

	public static boolean enableVao() {
		return false;
	}

	static void readFromConfig(ConfigData config) {
		readFromConfig(config, false);
	}

	static void readFromConfig(ConfigData config, boolean isStartup) {
		config.clearNulls();

		pipelineId = config.pipelineId;

		if (pipelineId == null || pipelineId.isEmpty()) {
			pipelineId = PipelineConfig.DEFAULT_ID.toString();
		}

		blendFluidColors = config.blendFluidColors;
		wavyGrass = config.wavyGrass;
		disableVignette = config.disableVignette;

		shaderDebug = config.shaderDebug;
		preprocessShaderSource = config.preprocessShaderSource;
		//maxLightmapDelayFrames = config.maxLightmapDelayFrames;
		//moreLightmap = config.moreLightmap;

		// hdLightmaps = config.hdLightmaps;
		// lightmapNoise = config.lightmapNoise;
		lightSmoothing = config.lightSmoothing;
		semiFlatLighting = config.semiFlatLighting;
		coloredLights = config.coloredLights;

		//        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
		preventDepthFighting = config.preventDepthFighting;
		clampExteriorVertices = config.clampExteriorVertices;
		fixLuminousBlockShading = config.fixLuminousBlockShading;
		advancedTerrainCulling = config.advancedTerrainCulling;
		terrainSetupOffThread = config.terrainSetupOffThread;
		safeNativeMemoryAllocation.set(config.safeNativeMemoryAllocation, isStartup);
		cullEntityRender = config.cullEntityRender;
		greedyRenderThread = config.greedyRenderThread;
		forceJmxModelLoading = config.forceJmxModelLoading;
		reduceResolutionOnMac.set(config.reduceResolutionOnMac, isStartup);
		dynamicFrustumPadding = Mth.clamp(config.dynamicFrustumPadding, 0, 30);
		staticFrustumPadding = Mth.clamp(config.staticFrustumPadding, 0, 20);
		cullParticles = config.cullParticles;
		useCombinedThreadPool.set(config.useCombinedThreadPool, isStartup);
		transferBufferMode = config.transferBufferMode;
		steadyDebugScreen = config.steadyDebugScreen;

		// lightmapDebug = config.lightmapDebug;
		shadowPrimingStrategy = config.shadowPrimingStrategy;
		shadowMaxDistance = config.shadowMaxDistance;
		shadowFaceCulling = config.shadowFaceCulling;
		shadowCenterFactor = config.shadowCenterFactor;
		disableShadowSelfOcclusion = config.disableShadowSelfOcclusion;
		conciseErrors = config.conciseErrors;
		logMachineInfo = config.logMachineInfo;
		logGlStateChanges = config.logGlStateChanges;
		debugNativeMemoryAllocation.set(config.debugNativeMemoryAllocation, isStartup);
		enablePerformanceTrace = config.enablePerformanceTrace;
		debugOcclusionBoxes = config.debugOcclusionBoxes;
		debugOcclusionRaster = config.debugOcclusionRaster;
		renderWhiteGlassAsOccluder = config.renderWhiteGlassAsOccluder;
		traceOcclusionEdgeCases = config.traceOcclusionEdgeCases;
		enableBufferDebug = config.enableBufferDebug;
		enableLifeCycleDebug = config.enableLifeCycleDebug;
		logMissingUniforms = config.logMissingUniforms;
		logMaterials = config.logMaterials;
		logRenderLagSpikes = config.logRenderLagSpikes;
		renderLagSpikeFps = Mth.clamp(config.renderLagSpikeFps, 30, 120);
		displayRenderProfiler = config.displayRenderProfiler;
		profilerDisplayMode = config.profilerDisplayMode;
		profilerDetailLevel = Mth.clamp(config.profilerDetailLevel, 0, 2);
		profilerOverlayScale = config.profilerOverlayScale;
		enableNearOccluders = config.enableNearOccluders;
		disableUnseenSpriteAnimation = config.disableUnseenSpriteAnimation;
		cullBackfacingTerrain = config.cullBackfacingTerrain;
		debugSpriteAtlas = config.debugSpriteAtlas;
		traceTextureLoad = config.traceTextureLoad;
	}

	static void writeToConfig(ConfigData config) {
		config.pipelineId = pipelineId;
		config.blendFluidColors = blendFluidColors;
		config.wavyGrass = wavyGrass;
		config.disableVignette = disableVignette;

		config.shaderDebug = shaderDebug;
		config.preprocessShaderSource = preprocessShaderSource;
		//config.maxLightmapDelayFrames = maxLightmapDelayFrames;

		// config.hdLightmaps = hdLightmaps;
		// config.lightmapNoise = lightmapNoise;
		config.lightSmoothing = lightSmoothing;
		//config.moreLightmap = moreLightmap;
		config.semiFlatLighting = semiFlatLighting;
		config.coloredLights = coloredLights;

		config.preventDepthFighting = preventDepthFighting;
		config.clampExteriorVertices = clampExteriorVertices;
		config.advancedTerrainCulling = advancedTerrainCulling;
		config.fixLuminousBlockShading = fixLuminousBlockShading;
		config.terrainSetupOffThread = terrainSetupOffThread;
		config.safeNativeMemoryAllocation = safeNativeMemoryAllocation.current;
		config.cullEntityRender = cullEntityRender;
		config.greedyRenderThread = greedyRenderThread;
		config.forceJmxModelLoading = forceJmxModelLoading;
		config.reduceResolutionOnMac = reduceResolutionOnMac.current;
		config.staticFrustumPadding = staticFrustumPadding;
		config.dynamicFrustumPadding = dynamicFrustumPadding;
		config.cullParticles = cullParticles;
		config.useCombinedThreadPool = useCombinedThreadPool.current;
		config.transferBufferMode = transferBufferMode;
		config.steadyDebugScreen = steadyDebugScreen;

		// config.lightmapDebug = lightmapDebug;
		config.shadowPrimingStrategy = shadowPrimingStrategy;
		config.shadowMaxDistance = shadowMaxDistance;
		config.shadowFaceCulling = shadowFaceCulling;
		config.shadowCenterFactor = shadowCenterFactor;
		config.disableShadowSelfOcclusion = disableShadowSelfOcclusion;
		config.conciseErrors = conciseErrors;
		config.logMachineInfo = logMachineInfo;
		config.logGlStateChanges = logGlStateChanges;
		config.debugNativeMemoryAllocation = debugNativeMemoryAllocation.current;
		config.enablePerformanceTrace = enablePerformanceTrace;
		config.debugOcclusionBoxes = debugOcclusionBoxes;
		config.debugOcclusionRaster = debugOcclusionRaster;
		config.renderWhiteGlassAsOccluder = renderWhiteGlassAsOccluder;
		config.traceOcclusionEdgeCases = traceOcclusionEdgeCases;
		config.enableBufferDebug = enableBufferDebug;
		config.enableLifeCycleDebug = enableLifeCycleDebug;
		config.logMissingUniforms = logMissingUniforms;
		config.logMaterials = logMaterials;
		config.logRenderLagSpikes = logRenderLagSpikes;
		config.renderLagSpikeFps = renderLagSpikeFps;
		config.displayRenderProfiler = displayRenderProfiler;
		config.profilerDisplayMode = profilerDisplayMode;
		config.profilerDetailLevel = profilerDetailLevel;
		config.profilerOverlayScale = profilerOverlayScale;
		config.enableNearOccluders = enableNearOccluders;
		config.disableUnseenSpriteAnimation = disableUnseenSpriteAnimation;
		config.cullBackfacingTerrain = cullBackfacingTerrain;
		config.debugSpriteAtlas = debugSpriteAtlas;
		config.traceTextureLoad = traceTextureLoad;
	}
}
