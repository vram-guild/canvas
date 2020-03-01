package grondag.canvas.draw;


import java.util.IdentityHashMap;

import net.minecraft.client.render.RenderLayer;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;

import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.material.MaterialVertexFormat;

public class DrawHandlers {
	private static int nextHandlerIndex = 0;

	public final int index = nextHandlerIndex++;

	private  static final IdentityHashMap<RenderLayer, DrawHandler> LAYER_MAP = new IdentityHashMap<>();

	private static final DrawHandler[] BLEND_MAP = new DrawHandler[5];

	static {
		LAYER_MAP.put(VanillaDrawHandler.SOLID.renderLayer, VanillaDrawHandler.SOLID);
		LAYER_MAP.put(VanillaDrawHandler.CUTOUT.renderLayer, VanillaDrawHandler.CUTOUT);
		LAYER_MAP.put(VanillaDrawHandler.CUTOUT_MIPPED.renderLayer, VanillaDrawHandler.CUTOUT_MIPPED);
		LAYER_MAP.put(VanillaDrawHandler.TRANSLUCENT.renderLayer, VanillaDrawHandler.TRANSLUCENT);

		BLEND_MAP[BlendMode.DEFAULT.ordinal()] = VanillaDrawHandler.SOLID;
		BLEND_MAP[BlendMode.SOLID.ordinal()] = VanillaDrawHandler.SOLID;
		BLEND_MAP[BlendMode.CUTOUT.ordinal()] = VanillaDrawHandler.CUTOUT;
		BLEND_MAP[BlendMode.CUTOUT_MIPPED.ordinal()] = VanillaDrawHandler.CUTOUT_MIPPED;
		BLEND_MAP[BlendMode.TRANSLUCENT.ordinal()] = VanillaDrawHandler.TRANSLUCENT;
	}

	public static DrawHandler get(MaterialContext context, MaterialVertexFormat format, Value mat) {
		return BLEND_MAP[mat.blendMode(0).ordinal()];
	}

	public static DrawHandler get(MaterialContext context, RenderLayer layer) {
		final DrawHandler result = LAYER_MAP.get(layer);
		return result == null ? VanillaDrawHandler.SOLID : result;
	}
}
