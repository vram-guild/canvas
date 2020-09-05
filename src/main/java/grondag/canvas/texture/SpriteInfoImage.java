package grondag.canvas.texture;

import java.nio.FloatBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.texture.Sprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.varia.CanvasGlHelper;

@Environment(EnvType.CLIENT)
public final class SpriteInfoImage implements AutoCloseable {
	public final int size;
	private long pointer;
	private final int sizeBytes;
	private FloatBuffer floatBuffer;

	// FIX: make texture square to reduce chance of overrun/driver strangeness

	public SpriteInfoImage(ObjectArrayList<Sprite> spriteIndex, int spriteCount, int size) {
		this.size = size;

		// 16 because 4 floats per vector, 4 because 4 samples per sprite
		sizeBytes = size * 16 * 4;
		pointer = MemoryUtil.nmemAlloc(sizeBytes);
		floatBuffer = MemoryUtil.memFloatBuffer(pointer, sizeBytes / 4);

		for (int i = 0;  i < spriteCount; ++i) {
			final Sprite s = spriteIndex.get(i);
			setPixel(i, s.getMinU(),s.getMinV(), s.getMaxU() - s.getMinU(), s.getMaxV() - s.getMinV());
		}
	}

	@Override
	public void close() {
		if (pointer != 0L) {
			floatBuffer = null;
			MemoryUtil.nmemFree(pointer);
		}

		pointer = 0L;
	}

	private void setPixel(int n, float x, float y, float z, float w) {
		assert n <= size;
		assert pointer != 0L : "Image not allocated.";
		n *= 16;
		floatBuffer.put(n, x);
		floatBuffer.put(n + 1, y);
		floatBuffer.put(n + 2, z);
		floatBuffer.put(n + 3, w);
	}

	public void upload() {
		assert pointer != 0L : "Image not allocated.";
		GL21.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL21.GL_RGBA16, 4, size, 0, GL21.GL_RGBA, GL21.GL_FLOAT, pointer);
		assert CanvasGlHelper.checkError();
	}
}
