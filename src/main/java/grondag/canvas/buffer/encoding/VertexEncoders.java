package grondag.canvas.buffer.encoding;


import grondag.canvas.apiimpl.RenderMaterialImpl.Value;
import grondag.canvas.material.MaterialContext;

public class VertexEncoders {
	public static final int MAX_ENCODERS = 8;

	public static final VanillaBlockEncoder VANILLA_BLOCK = new VanillaBlockEncoder();
	public static final VanillaItemEncoder VANILLA_ITEM = new VanillaItemEncoder();
	public static final VanillaTerrainEncoder VANILLA_TERRAIN = new VanillaTerrainEncoder();

	public static VertexEncoder get(MaterialContext context, Value mat) {
		switch (context) {
		case BLOCK:
			return VANILLA_BLOCK;

		case ITEM_HELD:
		case ITEM_GUI:
		case ITEM_GROUND:
		case ITEM_FIXED:
			return VANILLA_ITEM;
		case TERRAIN:
		default:
			return VANILLA_TERRAIN;
		}
	}

	//	public static VertexEncoder get(MaterialContext context, RenderLayer layer) {
	//		switch (context) {
	//		case BLOCK:
	//			return VANILLA_BLOCK;
	//		case ITEM_HELD:
	//		case ITEM_GUI:
	//		case ITEM_GROUND:
	//		case ITEM_FIXED:
	//			return VANILLA_ITEM;
	//		case TERRAIN:
	//		default:
	//			return VANILLA_TERRAIN;
	//		}
	//	}
}
