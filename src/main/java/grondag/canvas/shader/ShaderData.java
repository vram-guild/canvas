package grondag.canvas.shader;

import net.minecraft.util.Identifier;

public class ShaderData {
	public static final Identifier DEFAULT_VERTEX_SOURCE = new Identifier("canvas:shaders/material/default.vert");
	public static final Identifier DEFAULT_FRAGMENT_SOURCE = new Identifier("canvas:shaders/material/default.frag");

	public static final Identifier API_TARGET = new Identifier("canvas:apitarget");

	public static final Identifier HD_VERTEX = new Identifier("canvas:shaders/internal/hd/hd.vert");
	public static final Identifier HD_FRAGMENT = new Identifier("canvas:shaders/internal/hd/hd.frag");

	public static final Identifier VANILLA_VERTEX = new Identifier("canvas:shaders/internal/vanilla/vanilla.vert");
	public static final Identifier VANILLA_FRAGMENT = new Identifier("canvas:shaders/internal/vanilla/vanilla.frag");

	public static final Identifier COPY_VERTEX = new Identifier("canvas:shaders/internal/process/copy.vert");
	public static final Identifier COPY_FRAGMENT = new Identifier("canvas:shaders/internal/process/copy.frag");

	public static final Identifier BLUR_VERTEX = new Identifier("canvas:shaders/internal/process/blur.vert");
	public static final Identifier BLUR_FRAGMENT = new Identifier("canvas:shaders/internal/process/blur.frag");

	public static final Identifier BLOOM_VERTEX = new Identifier("canvas:shaders/internal/process/bloom.vert");
	public static final Identifier BLOOM_FRAGMENT = new Identifier("canvas:shaders/internal/process/bloom.frag");

	public static final Identifier BLOOM_SAMPLE_FRAGMENT = new Identifier("canvas:shaders/internal/process/bloomsample.frag");
}
