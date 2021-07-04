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

package grondag.canvas.config;

import static grondag.canvas.config.ConfigManager.DEFAULTS;

import net.minecraft.util.math.MathHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.pipeline.config.PipelineConfig;

@Environment(EnvType.CLIENT)
public class Configurator {
	public static String pipelineId = DEFAULTS.pipelineId;
	public static boolean blendFluidColors = DEFAULTS.blendFluidColors;
	public static boolean wavyGrass = DEFAULTS.wavyGrass;
	// public static boolean lightmapNoise = DEFAULTS.lightmapNoise;
	public static boolean lightSmoothing = DEFAULTS.lightSmoothing;
	//public static boolean moreLightmap = DEFAULTS.moreLightmap;
	//public static int maxLightmapDelayFrames = DEFAULTS.maxLightmapDelayFrames;
	public static boolean semiFlatLighting = DEFAULTS.semiFlatLighting;
	public static boolean preventDepthFighting = DEFAULTS.preventDepthFighting;
	public static boolean clampExteriorVertices = DEFAULTS.clampExteriorVertices;
	public static boolean fixLuminousBlockShading = DEFAULTS.fixLuminousBlockShading;
	public static boolean terrainSetupOffThread = DEFAULTS.terrainSetupOffThread;
	public static boolean cullEntityRender = DEFAULTS.cullEntityRender;
	public static boolean greedyRenderThread = DEFAULTS.greedyRenderThread;
	public static boolean forceJmxModelLoading = DEFAULTS.forceJmxModelLoading;
	public static boolean reduceResolutionOnMac = DEFAULTS.reduceResolutionOnMac;
	public static int staticFrustumPadding = DEFAULTS.staticFrustumPadding;
	public static int dynamicFrustumPadding = DEFAULTS.dynamicFrustumPadding;
	public static boolean cullParticles = DEFAULTS.cullParticles;
	public static boolean shaderDebug = DEFAULTS.shaderDebug;
	// public static boolean lightmapDebug = DEFAULTS.lightmapDebug;
	public static boolean conciseErrors = DEFAULTS.conciseErrors;
	public static boolean logMachineInfo = DEFAULTS.logMachineInfo;
	public static boolean logGlStateChanges = DEFAULTS.logGlStateChanges;
	public static boolean debugNativeMemoryAllocation = DEFAULTS.debugNativeMemoryAllocation;
	public static boolean safeNativeMemoryAllocation = DEFAULTS.safeNativeMemoryAllocation;
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
	public static boolean profileGpuTime = DEFAULTS.profileGpuTime;
	public static int profilerDetailLevel = DEFAULTS.profilerDetailLevel;
	public static float profilerOverlayScale = DEFAULTS.profilerOverlayScale;
	public static boolean vf = DEFAULTS.vf;

	//    @LangKey("config.acuity_fancy_fluids")
	//    @Comment({"Enable fancy water and lava rendering.",
	//        " This feature is currently work in progress and has no visible effect if enabled."})
	public static boolean fancyFluids = false;
	static boolean reload = false;
	// static boolean hdLightmaps = DEFAULTS.hdLightmaps;
	public static boolean hdLightmaps() {
		return false;
	}

	public static boolean enableVao() {
		return false;
	}

	static void readFromConfig(ConfigData config) {
		pipelineId = config.pipelineId;

		if (pipelineId == null || pipelineId.isEmpty()) {
			pipelineId = PipelineConfig.DEFAULT_ID.toString();
		}

		blendFluidColors = config.blendFluidColors;
		wavyGrass = config.wavyGrass;

		shaderDebug = config.shaderDebug;
		//maxLightmapDelayFrames = config.maxLightmapDelayFrames;
		//moreLightmap = config.moreLightmap;

		// hdLightmaps = config.hdLightmaps;
		// lightmapNoise = config.lightmapNoise;
		lightSmoothing = config.lightSmoothing;
		semiFlatLighting = config.semiFlatLighting;

		//        disableVanillaChunkMatrix = config.disableVanillaChunkMatrix;
		preventDepthFighting = config.preventDepthFighting;
		clampExteriorVertices = config.clampExteriorVertices;
		fixLuminousBlockShading = config.fixLuminousBlockShading;
		terrainSetupOffThread = config.terrainSetupOffThread;
		safeNativeMemoryAllocation = config.safeNativeMemoryAllocation;
		cullEntityRender = config.cullEntityRender;
		greedyRenderThread = config.greedyRenderThread;
		forceJmxModelLoading = config.forceJmxModelLoading;
		reduceResolutionOnMac = config.reduceResolutionOnMac;
		dynamicFrustumPadding = MathHelper.clamp(config.dynamicFrustumPadding, 0, 20);
		staticFrustumPadding = MathHelper.clamp(config.staticFrustumPadding, 0, 30);
		cullParticles = config.cullParticles;

		// lightmapDebug = config.lightmapDebug;
		conciseErrors = config.conciseErrors;
		logMachineInfo = config.logMachineInfo;
		logGlStateChanges = config.logGlStateChanges;
		debugNativeMemoryAllocation = config.debugNativeMemoryAllocation;
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
		renderLagSpikeFps = MathHelper.clamp(config.renderLagSpikeFps, 30, 120);
		displayRenderProfiler = config.displayRenderProfiler;
		profileGpuTime = config.profileGpuTime;
		profilerDetailLevel = MathHelper.clamp(config.profilerDetailLevel, 0, 2);
		profilerOverlayScale = config.profilerOverlayScale;
		vf = config.vf;
	}

	static void writeToConfig(ConfigData config) {
		config.pipelineId = pipelineId;
		config.blendFluidColors = blendFluidColors;
		config.wavyGrass = wavyGrass;

		config.shaderDebug = shaderDebug;
		//config.maxLightmapDelayFrames = maxLightmapDelayFrames;

		// config.hdLightmaps = hdLightmaps;
		// config.lightmapNoise = lightmapNoise;
		config.lightSmoothing = lightSmoothing;
		//config.moreLightmap = moreLightmap;
		config.semiFlatLighting = semiFlatLighting;

		config.preventDepthFighting = preventDepthFighting;
		config.clampExteriorVertices = clampExteriorVertices;
		config.fixLuminousBlockShading = fixLuminousBlockShading;
		config.terrainSetupOffThread = terrainSetupOffThread;
		config.safeNativeMemoryAllocation = safeNativeMemoryAllocation;
		config.cullEntityRender = cullEntityRender;
		config.greedyRenderThread = greedyRenderThread;
		config.forceJmxModelLoading = forceJmxModelLoading;
		config.reduceResolutionOnMac = reduceResolutionOnMac;
		config.staticFrustumPadding = staticFrustumPadding;
		config.dynamicFrustumPadding = dynamicFrustumPadding;
		config.cullParticles = cullParticles;

		// config.lightmapDebug = lightmapDebug;
		config.conciseErrors = conciseErrors;
		config.logMachineInfo = logMachineInfo;
		config.logGlStateChanges = logGlStateChanges;
		config.debugNativeMemoryAllocation = debugNativeMemoryAllocation;
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
		config.profileGpuTime = profileGpuTime;
		config.profilerDetailLevel = profilerDetailLevel;
		config.profilerOverlayScale = profilerOverlayScale;
		config.vf = vf;
	}
}
