package grondag.canvas.buffer.encoding.vanilla;

import grondag.canvas.buffer.encoding.VertexEncoder;

public class VanillaEncoders {
	public static final VertexEncoder VANILLA_BLOCK = new VanillaBlockEncoder(0);
	public static final VertexEncoder VANILLA_ITEM = new VanillaItemEncoder(1);
	public static final VertexEncoder VANILLA_TERRAIN = new VanillaTerrainEncoder(2);
}
