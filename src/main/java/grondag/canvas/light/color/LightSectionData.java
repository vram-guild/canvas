package grondag.canvas.light.color;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.TextureUtil;

import net.minecraft.core.BlockPos;

import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

public class LightSectionData {
	public static class Format {
		public static int target = GFX.GL_TEXTURE_3D;
		public static int pixelBytes = 2;
		public static int internalFormat = GFX.GL_RGBA4;
		public static int pixelFormat = GFX.GL_RGBA;
		public static int pixelDataType = GFX.GL_UNSIGNED_SHORT_4_4_4_4;
	}

	public static class Const {
		public static final int WIDTH = 16 * 2;
		private static final int SIZE3D = WIDTH * WIDTH * WIDTH;

		public static final int WIDTH_SHIFT = (int) (Math.log(WIDTH) / Math.log(2));
		public static final int WIDTH_MASK = WIDTH - 1;
	}

	public static enum Elem {
		R(0xF000, 12, 0),
		G(0x0F00, 8, 1),
		B(0x00F0, 4, 2),
		A(0x000F, 0, 3);

		public final int mask;
		public final int shift;
		public final int pos;

		Elem(int mask, int shift, int pos) {
			this.mask = mask;
			this.shift = shift;
			this.pos = pos;
		}

		public int of(short light) {
			return (light >> shift) & 0xF;
		}

		public short replace(short source, short elemLight) {
			return (short) ((source & ~mask) | (elemLight << shift));
		}

		public static short encode(int r, int g, int b, int a) {
			return (short) ((r << R.shift) | (g << G.shift) | (b << B.shift) | (a << A.shift));
		}

		public static String text(short light) {
			return "(" + R.of(light) + "," + G.of(light) + "," + B.of(light) + ")";
		}
	}

	public static class Encoding {
		public static short encodeLight(int r, int g, int b, boolean isLightSource, boolean isOccluding) {
			return Elem.encode(r, g, b, (isLightSource ? 0b1 : 0) | (isOccluding ? 0b10 : 0));
		}

		public static short encodeLight(int pureLight, boolean isLightSource, boolean isOccluding) {
			return (short) (pureLight | (isLightSource ? 0b1 : 0) | (isOccluding ? 0b10 : 0));
		}

		public static boolean isLightSource(short light) {
			return (light & 0b1) != 0;
		}

		public static boolean isOccluding(short light) {
			return (light & 0b10) != 0;
		}

		public static short pure(short light) {
			return (short) (light & 0xfff0);
		}
	}

	// placeholder
	private int sectionBlockOffsetX = -16;
	private int sectionBlockOffsetY = 100;
	private int sectionBlockOffsetZ = -16;

	private ByteBuffer buffer;
	private int glTexId;
	private boolean closed = false;

	public LightSectionData() {
		glTexId = TextureUtil.generateTextureId();

		CanvasTextureState.bindTexture(Format.target, glTexId);
		GFX.objectLabel(GFX.GL_TEXTURE, glTexId, "IMG light_section_volume");

		GFX.texParameter(Format.target, GFX.GL_TEXTURE_MIN_FILTER, GFX.GL_LINEAR);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_MAG_FILTER, GFX.GL_LINEAR);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_WRAP_S, GFX.GL_CLAMP_TO_EDGE);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_WRAP_T, GFX.GL_CLAMP_TO_EDGE);
		GFX.texParameter(Format.target, GFX.GL_TEXTURE_WRAP_R, GFX.GL_CLAMP_TO_EDGE); // ?

		buffer = MemoryUtil.memAlloc(Format.pixelBytes * Const.SIZE3D);

		// clear manually ?
		while (buffer.position() < Format.pixelBytes * Const.SIZE3D) {
			buffer.putShort((short) 0);
		}

		// allocate
		GFX.texImage3D(Format.target, 0, Format.internalFormat, Const.WIDTH, Const.WIDTH, Const.WIDTH, 0, Format.pixelFormat, Format.pixelDataType, null);
	}

	public void upload() {
		CanvasTextureState.bindTexture(Format.target, glTexId);

		// Gotta clean up some states, otherwise will cause memory access violation
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_PIXELS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_SKIP_ROWS, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ROW_LENGTH, 0);
		GFX.pixelStore(GFX.GL_UNPACK_ALIGNMENT, 2);

		// Importantly, reset the pointer without flip
		buffer.position(0);

		GFX.glTexSubImage3D(Format.target, 0, 0, 0, 0, Const.WIDTH, Const.WIDTH, Const.WIDTH, Format.pixelFormat, Format.pixelDataType, buffer);
	}

	public void draw(short rgba) {
		buffer.putShort(rgba);
	}

	public void close() {
		TextureUtil.releaseTextureId(glTexId);
		MemoryUtil.memFree(buffer);
		glTexId = -1;
		closed = true;
	}

	public short get(int index) {
		return buffer.getShort(index);
	}

	public void put(int index, short light) {
		buffer.putShort(index, light);
	}

	public int indexify(BlockPos pos) {
		return indexify(pos.getX(), pos.getY(), pos.getZ());
	}

	public int indexify(int x, int y, int z) {
		final int localX = x - sectionBlockOffsetX;
		final int localY = y - sectionBlockOffsetY;
		final int localZ = z - sectionBlockOffsetZ;

		// x and z are swapped because opengl
		return ((localZ << (Const.WIDTH_SHIFT * 2)) | (localY << Const.WIDTH_SHIFT) | localX) * Format.pixelBytes;
	}

	public void reverseIndexify(int index, int[] result) {
		assert result.length == 3;

		index = index / Format.pixelBytes;

		// x and z are swapped because opengl
		result[0] = (index & Const.WIDTH_MASK) + sectionBlockOffsetX;
		result[1] = ((index >> Const.WIDTH_SHIFT) & Const.WIDTH_MASK) + sectionBlockOffsetY;
		result[2] = ((index >> Const.WIDTH_SHIFT * 2) & Const.WIDTH_MASK) + sectionBlockOffsetZ;
	}

	public boolean withinExtents(BlockPos pos) {
		return withinExtents(pos.getX(), pos.getY(), pos.getZ());
	}

	public boolean withinExtents(int x, int y, int z) {
		return (x >= sectionBlockOffsetX && x < sectionBlockOffsetX + Const.WIDTH)
				&& (y >= sectionBlockOffsetY && y < sectionBlockOffsetY + Const.WIDTH)
				&& (z >= sectionBlockOffsetZ && z < sectionBlockOffsetZ + Const.WIDTH);
	}

	public int getTexId() {
		if (closed) {
			throw new IllegalStateException("Trying to access a deleted Light Section Data!");
		}

		return glTexId;
	}

	public boolean isClosed() {
		return closed;
	}
}
