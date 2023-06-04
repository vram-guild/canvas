package grondag.canvas.light.color;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.CanvasMod;
import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class LightDataTexture {

	public static class Format {
		public static int target = GFX.GL_TEXTURE_3D;
		public static int pixelBytes = 2;
		public static int internalFormat = GFX.GL_RGBA4;
		public static int pixelFormat = GFX.GL_RGBA;
		public static int pixelDataType = GFX.GL_UNSIGNED_SHORT_4_4_4_4;
	}

	private int glTexId = -1;
	final int size;

	LightDataTexture(int size) {
		this.size = size;

		glTexId = TextureUtil.generateTextureId();

		CanvasTextureState.bindTexture(Format.target, glTexId);
		GFX.objectLabel(GFX.GL_TEXTURE, glTexId, "IMG light_section_volume");

		GFX.texParameter(Format.target, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_LINEAR);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_LINEAR);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_WRAP_S, GFX.GL_CLAMP_TO_EDGE);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_WRAP_T, GFX.GL_CLAMP_TO_EDGE);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_WRAP_R, GFX.GL_CLAMP_TO_EDGE);

		// allocate
		GFX.texImage3D(Format.target, 0, Format.internalFormat, size, size, size, 0, Format.pixelFormat, Format.pixelDataType, null);

		ByteBuffer clearer = MemoryUtil.memAlloc(size * size * size * Format.pixelBytes);

		while (clearer.position() < clearer.limit()) {
			clearer.putShort((short) 0);
		}

		// clear??
		upload(0, 0, 0, clearer);

		clearer.position(0);
		MemoryUtil.memFree(clearer);
	}

	public void close() {
		TextureUtil.releaseTextureId(glTexId);
		glTexId = -1;
	}

	public void upload(int x, int y, int z, ByteBuffer buffer) {
		upload(x, y, z, LightRegionData.Const.WIDTH, buffer);
	}

	public void upload(int x, int y, int z, int regionSize, ByteBuffer buffer) {
		if (glTexId == -1) {
			throw new IllegalStateException("Uploading to a deleted texture!");
		}

		RenderSystem.assertOnRenderThread();

		CanvasTextureState.bindTexture(LightDataTexture.Format.target, glTexId);

		// Gotta clean up some states, otherwise will cause memory access violation
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, 2);

		// Importantly, reset the pointer without flip
		buffer.position(0);

		GFX.glTexSubImage3D(Format.target, 0, x, y, z, regionSize, regionSize, regionSize, Format.pixelFormat, Format.pixelDataType, buffer);
	}

	public int getTexId() {
		if (glTexId == -1) {
			throw new IllegalStateException("Trying to access a deleted Light Data texture!");
		}

		return glTexId;
	}
}
