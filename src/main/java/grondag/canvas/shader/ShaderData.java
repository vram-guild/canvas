package grondag.canvas.shader;

import net.minecraft.util.Identifier;

public class ShaderData {
	public static final Identifier DEFAULT_VERTEX_SOURCE = new Identifier("canvas:shaders/material/default.vert");
	public static final Identifier DEFAULT_FRAGMENT_SOURCE = new Identifier("canvas:shaders/material/default.frag");

	public static final Identifier API_TARGET = new Identifier("canvas:apitarget");
	public static final Identifier HD_VERTEX = new Identifier("canvas:shaders/internal/hd/hd_vertex.glsl");
	public static final Identifier VANILLA_VERTEX = new Identifier("canvas:shaders/internal/vanilla/vanilla_vertex.glsl");

	public static final Identifier HD_FRAGMENT = new Identifier("canvas:shaders/internal/hd/hd_fragment.glsl");
	public static final Identifier VANILLA_FRAGMENT = new Identifier("canvas:shaders/internal/vanilla/vanilla_fragment.glsl");
}
