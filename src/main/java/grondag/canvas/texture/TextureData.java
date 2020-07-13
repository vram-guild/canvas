package grondag.canvas.texture;

import org.lwjgl.opengl.GL21;

public class TextureData {

	// bindings MC uses during world rendering
	public static final int MC_SPRITE_ATLAS = GL21.GL_TEXTURE0;
	public static final int MC_OVELAY = GL21.GL_TEXTURE1;
	public static final int MC_LIGHTMAP = GL21.GL_TEXTURE2;


	public static final int HD_LIGHTMAP = GL21.GL_TEXTURE4;
	public static final int DITHER = GL21.GL_TEXTURE5;
	public static final int SPRITE_INFO = GL21.GL_TEXTURE6;
}
