package grondag.canvas.draw;

import grondag.canvas.material.MaterialBufferFormat;

public class VanillaDrawHandler extends DrawHandler {
	static final VanillaDrawHandler INSTANCE = new VanillaDrawHandler();

	@Override
	public MaterialBufferFormat inputFormat() {
		return MaterialBufferFormat.VANILLA_BLOCKS_AND_ITEMS;
	}
}
