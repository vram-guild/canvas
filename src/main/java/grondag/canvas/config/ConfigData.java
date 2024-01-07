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

import blue.endless.jankson.Comment;

import grondag.canvas.buffer.render.TransferBuffers;
import grondag.canvas.perf.Timekeeper;
import grondag.canvas.pipeline.config.PipelineConfig;
import grondag.canvas.render.world.SkyShadowRenderer;
import grondag.canvas.terrain.occlusion.TerrainIterator;

class ConfigData {
	public static final ConfigData DEFAULT_VALUES = new ConfigData();

	@Comment("Renderer configuration. Determines appearance, performance and available options.")
	public String pipelineId = PipelineConfig.DEFAULT_ID.toString();
	@Comment("Glow effect around light sources.")
	public boolean wavyGrass = true;
	@Comment("Enable rendering of internal buffers for debug purposes. Off by default to prevent accidental activation.")
	public boolean enableBufferDebug = false;
	@Comment("Output load/reload trace data to log. Will have performance impact.")
	public boolean enableLifeCycleDebug = false;
	@Comment("Fluid biome colors are blended at block corners to avoid patchy appearance. Slight performance impact to chunk loading.")
	boolean blendFluidColors = true;
	@Comment("Turns off darkened screen corners present in vanilla.")
	boolean disableVignette = false;
	//@Comment("Truly smooth lighting. Some impact to memory use, chunk loading and frame rate.")
	//boolean hdLightmaps = false;
	//@Comment("Slight variation in light values - may prevent banding. Slight performance impact and not usually necessary.")
	//boolean lightmapNoise = false;
	@Comment("Makes light sources less cross-shaped. Chunk loading a little slower. Overall light levels remain similar.")
	boolean lightSmoothing = false;
	//@Comment("Setting > 0 may give slightly better FPS at cost of potential flickering when lighting changes.")
	//int maxLightmapDelayFrames = 0;
	//@Comment("Extra lightmap capacity. Ensure enabled if you are getting `unable to create HD lightmap(s) - out of space' messages.")
	//boolean moreLightmap = true;
	@Comment("Models with flat lighting have smoother lighting (but no ambient occlusion).")
	boolean semiFlatLighting = true;
	@Comment("Enable colored block lights on pipelines that support it. Replaces vanilla lighting but only visually.")
	boolean coloredLights = false;
	@Comment("Enable entity as dynamic light sources. Requires colored lights and supporting pipeline.")
	boolean entityLightSource = false;

	// TWEAKS
	@Comment("Adjusts quads on some vanilla models (like iron bars) to avoid z-fighting with neighbor blocks.")
	boolean preventDepthFighting = true;
	@Comment("Treats model geometry outside of block boundaries as on the block for lighting purposes. Helps prevent bad lighting outcomes.")
	boolean clampExteriorVertices = true;
	@Comment("Prevent Glowstone and other blocks that emit light from casting shade on nearby blocks.")
	boolean fixLuminousBlockShading = true;
	@Comment("Uses more CPU to avoid drawing terrain that isn't visible. Improves FPS. Best for machines with 6+ fast CPU cores. Ignored if shadow map is enabled.")
	boolean advancedTerrainCulling = true;
	@Comment("Terrain setup done off the main render thread. Increases FPS when moving. May see occasional flashes of blank chunks")
	boolean terrainSetupOffThread = true;
	@Comment("Use more efficient entity culling. Improves framerate in most scenes.")
	boolean cullEntityRender = true;
	@Comment("When true, render thread does not yield to other threads every frame. Vanilla behavior is false (yields).")
	boolean greedyRenderThread = true;
	@Comment("Use more efficient model loading. Improves chunk rebuild speed and reduces memory use.")
	boolean forceJmxModelLoading = true;
	@Comment("Use half resolution on retina displays - greatly improves frame rate on Macs.")
	boolean reduceResolutionOnMac = true;
	@Comment("Padding at edges of screen to reduce how often terrain visibility is computed. In degrees. Values 0 to 20. Zero disables.")
	int staticFrustumPadding = 10;
	@Comment("Extra padding at edges of screen to reduce missing chunks when view rotates and terrainSetupOffThread is on. In degrees. Values 0 to 30. Zero disables.")
	int dynamicFrustumPadding = 20;
	@Comment("Culls particles that are not in view. Should always be faster.")
	boolean cullParticles = true;
	@Comment("Use Canvas thread pool for rendering and local server tasks. May reduce lag spikes caused by thread contention.")
	boolean useCombinedThreadPool = false;
	@Comment("Strategy used to prime shadow regions. Tiered has fewer gaps but is more expensive, while Padded is slightly cleverer than Naive.")
	TerrainIterator.ShadowPriming shadowPrimingStrategy = TerrainIterator.ShadowPriming.PADDED;
	@Comment("Maximum shadow render distance to be compared against render distance. TEMPORARY, meant to become a pipeline configuration.")
	int shadowMaxDistance = 32;
	@Comment("Face culling mode for depth pass rendering. TEMPORARY, meant to become a pipeline configuration.")
	SkyShadowRenderer.Culling shadowFaceCulling = SkyShadowRenderer.Culling.BACK;
	@Comment("Interpolate shadow map center to the approximated camera frustum centroid. Increases precision but may cause clipping.")
	float shadowCenterFactor = 1.0f;
	@Comment("Only use target occluder for shadow culling. WIP, temporary workaround for gaps in shadow map.")
	boolean disableShadowSelfOcclusion = false;
	@Comment("When enabled, F3 debug screen output is refreshed 20X per second instead of every frame. Improves accuracy and reduces variability of FPS measurement.")
	boolean steadyDebugScreen = true;
	@Comment("When true, animated sprites not in view are not updated. Improves frame rate.")
	boolean disableUnseenSpriteAnimation = true;
	@Comment("When true, terrain facing away from the camera is not rendered.  Usually improves frame rate.")
	boolean cullBackfacingTerrain = true;
	@Comment("Enabling may help performance by drawing fewer regions but some regions may flicker as you move around nearby blocks.")
	boolean enableNearOccluders = false;
	@Comment("Method used to transfer data to GPU. AUTO is recommended but performance can be specific to your system. Other options are DIRECT, HYBRID, and MAPPED")
	public TransferBuffers.Config transferBufferMode = TransferBuffers.Config.AUTO;
	@Comment("Uses slower and safer memory allocation method for GL buffers.  Use only if having problems. Requires restart.")
	boolean safeNativeMemoryAllocation = false;

	// DEBUG
	@Comment("Output runtime per-material shader source. For shader development debugging.")
	boolean shaderDebug = false;
	@Comment("Pre-process OpenGL source before compilation. Makes source more concise but possibly harder to read.")
	boolean preprocessShaderSource = true;
	//@Comment("Shows HD lightmap pixels for debug purposes. Also looks cool.")
	//boolean lightmapDebug = false;
	@Comment("Summarizes multiple errors and warnings to single-line entries in the log.")
	boolean conciseErrors = true;
	@Comment("Writes information useful for bug reports to the game log at startup.")
	boolean logMachineInfo = true;
	@Comment("Writes OpenGL state changes to log.  *VERY SPAMMY - KILLS FRAME RATE*  Used only for debugging.")
	boolean logGlStateChanges = false;
	@Comment("Enables LWJGL memory allocation tracking.  Will harm performance. Use for debugging memory leaks. Requires restart.")
	boolean debugNativeMemoryAllocation = false;
	@Comment("Output performance trace data to log. Will have significant performance impact. Requires restart.")
	boolean enablePerformanceTrace = false;
	@Comment("Output periodic snapshots of terrain occlusion raster. Will have performance impact.")
	boolean debugOcclusionRaster = false;
	@Comment("Render active occlusion boxes of targeted render region. Will have performance impact and looks strange.")
	boolean debugOcclusionBoxes = false;
	@Comment("White stained glass occludes terrain. Use to debug terrain occlusion.")
	boolean renderWhiteGlassAsOccluder = false;
	@Comment("Log clipping or other non-critical failures detected by terrain occluder. May spam the log.")
	boolean traceOcclusionEdgeCases = false;
	@Comment("Log uniforms not found in shaders. Sometimes useful for shader debug. Will spam the log.")
	boolean logMissingUniforms = false;
	@Comment("Log render material states and vanilla RenderLayer mapping. Useful for material debug and pack makers. Will spam the log.")
	boolean logMaterials = false;
	@Comment("Log information on render lag spikes - when they happen and where. Will spam the log.")
	boolean logRenderLagSpikes = false;
	@Comment("Approximate target FPS when logRenderLagSpikes is enabled. If elapsed time exceeds an entire frame, a spike is logged. 30-120")
	int renderLagSpikeFps = 30;
	@Comment("Enable and display render profiler data.")
	boolean displayRenderProfiler = false;
	@Comment("Type of profiler data to display.")
	Timekeeper.Mode profilerDisplayMode = Timekeeper.Mode.CPU;
	@Comment("Profiler level of detail. 0=Collapse all, 1=Expand program passes, 2=Expand all")
	int profilerDetailLevel = 0;
	@Comment("Size of the profiler overlay relative to GUI scale.")
	float profilerOverlayScale = 0.5f;
	@Comment("Export sprite atlas textures to atlas_debug folder within run folder.")
	boolean debugSpriteAtlas = false;
	@Comment("Log significant events of texture/sprite atlas loading. For debugging use. Will spam the log.")
	boolean traceTextureLoad = false;
	@Comment("Enables debug flag in the shader. Only intended for internal Canvas development purposes.")
	boolean debugShaderFlag = false;

	// GSON doesn't do this automatically
	public void clearNulls() {
		if (shadowPrimingStrategy == null) {
			shadowPrimingStrategy = DEFAULT_VALUES.shadowPrimingStrategy;
		}

		if (shadowFaceCulling == null) {
			shadowFaceCulling = DEFAULT_VALUES.shadowFaceCulling;
		}

		if (transferBufferMode == null) {
			transferBufferMode = DEFAULT_VALUES.transferBufferMode;
		}

		if (profilerDisplayMode == null) {
			profilerDisplayMode = DEFAULT_VALUES.profilerDisplayMode;
		}
	}
}
