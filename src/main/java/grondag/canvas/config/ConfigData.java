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

import blue.endless.jankson.Comment;

import grondag.canvas.pipeline.config.PipelineConfig;

class ConfigData {
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

	// TWEAKS
	@Comment("Adjusts quads on some vanilla models (like iron bars) to avoid z-fighting with neighbor blocks.")
	boolean preventDepthFighting = true;
	@Comment("Treats model geometry outside of block boundaries as on the block for lighting purposes. Helps prevent bad lighting outcomes.")
	boolean clampExteriorVertices = true;
	@Comment("Prevent Glowstone and other blocks that emit light from casting shade on nearby blocks.")
	boolean fixLuminousBlockShading = true;
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

	// DEBUG
	@Comment("Output runtime per-material shader source. For shader development debugging.")
	boolean shaderDebug = false;
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
	@Comment("Uses slower and safer memory allocation method for GL buffers.  Use only if having problems. Requires restart.")
	boolean safeNativeMemoryAllocation = false;
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
	@Comment("Profiler level of detail. 0=Collapse all, 1=Expand program passes, 2=Expand all")
	int profilerDetailLevel = 0;
	@Comment("Size of the profiler overlay relative to GUI scale.")
	float profilerOverlayScale = 0.5f;
	//WIP: docs
	TerrainVertexConfig terrainVertexConfig = TerrainVertexConfig.DEFAULT;
}
