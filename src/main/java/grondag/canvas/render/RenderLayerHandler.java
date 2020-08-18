package grondag.canvas.render;

import net.minecraft.client.render.RenderLayer;

import grondag.canvas.Configurator;
import grondag.canvas.pipeline.CanvasFrameBufferHacks;
import grondag.canvas.shader.EntityShader;
import grondag.canvas.shader.GlProgram;

public enum RenderLayerHandler {
	INSTANCE;

	private static boolean enableShaderDraw = false;

	public static void enableShaderDraw(boolean enable) {
		enableShaderDraw = enable;
	}

	public static void startDrawing(RenderLayer renderLayer) {
		if (enableShaderDraw) {
			startShaderDraw(renderLayer);
		}
	}

	public static void endDrawing(RenderLayer renderLayer) {
		if (enableShaderDraw) {
			endShaderDraw(renderLayer);
		}
	}

	private static void startShaderDraw(RenderLayer renderLayer) {
		//		final AccessMultiPhaseParameters params = ((MultiPhaseExt) renderLayer).canvas_phases();

		// RenderLayer elements...

		//texture - primary texture binding
		// for non-item/block can inspect here to handle material info
		// for item/block controls mipped or non-mipped
		// can also be non-textured - for non-textured probably best to stay with fixed pipeline for now
		//		final boolean hasTexture = params.getTexture() != RenderPhase.NO_TEXTURE;

		//transparency
		// shader probably doesn't care but would be useful to expose different types in material builder
		//		final boolean hasTranslucent = params.getTransparency() !=  RenderPhase.NO_TRANSPARENCY;


		//diffuseLighting
		// when active, lighting will need to be applied in the shader
		// light setup is different for in-world (and varies by dimension) and GUI contexts.
		// see DiffuseLighting and the calls it makes to RenderSystem for details
		//		final boolean enableDiffuse = params.getDiffuseLighting() == RenderPhase.ENABLE_DIFFUSE_LIGHTING;

		//shadeModel
		// not applicable in core profile but still seems to disable interpolation in 2.1
		// the default is flat
		// should work as-is?
		// need to handle if/when consolidating non-terrain passes
		//		final boolean isFlat = params.getShadeModel() == RenderPhase.SHADE_MODEL;

		//alpha (AKA cutout)
		// still works so don't need to handle until we want to consolidate non-terrain render passes
		// has 50% and 10% cutout variants

		//depthTest
		// currently no need to handle in shader

		//cull
		// currently no need to handle in shader

		//lightmap
		// true when lightmap texture/application is enabled
		//		final boolean enableLightmap = params.getLightmap() == RenderPhase.ENABLE_LIGHTMAP;

		//overlay
		// overlay texture is either white flashing (TNT) or a red color (mob damage)
		// should replace it entirely with a shader flag
		//		final boolean hasOverlay = params.getOverlay() == Overlay.ENABLE_OVERLAY_COLOR;

		//fog
		// Handled by existing hooks?

		//layering (decals)
		// works by polygonOffset or matrix scaling so should work without special handling
		// unless we try to consolidate draw pass for it with non-decal pass
		// would be useful to expose in material

		//target
		// sets framebuffer target
		// should work as-is but useful to expose in material, probably not directly because can vary based on config

		//texturing
		// sets up outline, glint or default texturing
		// these probably won't work as-is with shaders because they use texture env settings
		// so may be best to leave them for now

		//writeMaskState
		// can be color, depth or both - leave for now

		//lineWidth
		// default is FULL_LINE_WIDTH, set to Optional.empty() for line drawing
		// unknown if/how interacts with shader - leave for now

		if (Configurator.enableBloom) CanvasFrameBufferHacks.startEmissiveCapture(false);
		EntityShader.DEFAULT_SOLID.activate();
	}

	private static void endShaderDraw(RenderLayer renderLayer) {
		GlProgram.deactivate();
		if (Configurator.enableBloom) CanvasFrameBufferHacks.endEmissiveCapture();
	}
}
