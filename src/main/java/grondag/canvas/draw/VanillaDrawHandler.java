package grondag.canvas.draw;

import net.minecraft.client.render.RenderLayer;

import grondag.canvas.apiimpl.MaterialConditionImpl;
import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.material.MaterialVertexFormats;

public class VanillaDrawHandler extends DrawHandler {
	VanillaDrawHandler(MaterialVertexFormat format, MaterialShaderImpl shader, MaterialConditionImpl condition, RenderLayer renderLayer) {
		super(format, shader, condition, renderLayer);
	}

	@Override
	protected void setupInner() {
		renderLayer.startDrawing();
	}

	@Override
	protected void teardownInner() {
		renderLayer.endDrawing();
	}

	public static final DrawHandler SOLID = new VanillaDrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS, RenderLayer.getSolid());
	public static final DrawHandler CUTOUT = new VanillaDrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS, RenderLayer.getCutout());
	public static final DrawHandler CUTOUT_MIPPED = new VanillaDrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS, RenderLayer.getCutoutMipped());
	public static final DrawHandler TRANSLUCENT = new VanillaDrawHandler(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, null, MaterialConditionImpl.ALWAYS, RenderLayer.getTranslucent());
}
