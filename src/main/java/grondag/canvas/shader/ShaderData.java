package grondag.canvas.shader;

import net.minecraft.util.Identifier;

public class ShaderData {
	public static final Identifier DEFAULT_VERTEX_SOURCE = new Identifier("canvas", "shaders/default.vert");
	public static final Identifier DEFAULT_FRAGMENT_SOURCE = new Identifier("canvas", "shaders/default.frag");
	public static final Identifier WATER_VERTEX_SOURCE = new Identifier("canvas", "shaders/water.vert");
	public static final Identifier WATER_FRAGMENT_SOURCE = new Identifier("canvas", "shaders/water.frag");
	public static final Identifier LAVA_VERTEX_SOURCE = new Identifier("canvas", "shaders/lava.vert");
	public static final Identifier LAVA_FRAGMENT_SOURCE = new Identifier("canvas", "shaders/lava.frag");

	public static final Identifier STD_VERTEX_LIB = new Identifier("canvas", "shaders/std_vertex_lib.glsl");
	public static final Identifier HD_VERTEX_LIB = new Identifier("canvas", "shaders/hd_vertex_lib.glsl");
	public static final Identifier VANILLA_VERTEX_LIB = new Identifier("canvas", "shaders/vanilla_vertex_lib.glsl");

	public static final Identifier STD_FRAGMENT_LIB = new Identifier("canvas", "shaders/std_fragment_lib.glsl");
	public static final Identifier HD_FRAGMENT_LIB = new Identifier("canvas", "shaders/hd_fragment_lib.glsl");
	public static final Identifier VANILLA_FRAGMENT_LIB = new Identifier("canvas", "shaders/vanilla_fragment_lib.glsl");
}
