package grondag.canvas.draw;

import grondag.canvas.apiimpl.RenderMaterialImpl;
import grondag.canvas.material.MaterialVertexFormats;

public class VanillaDrawHandler extends DrawHandler {
	public static final VanillaDrawHandler INSTANCE = new VanillaDrawHandler();

	VanillaDrawHandler() {
		super(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS, RenderMaterialImpl.byIndex(0));
	}

	@Override
	protected void activate() {
		// TODO Auto-generated method stub

	}
}
