package grondag.canvas.pipeline;

import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureUtil;

public class Bloom {

	// TODO: tear down
	static int mainFbo = -1;
	static int mainColor;
	static int full  = -1;
	static int fullBlur;
	static int half;
	static int halfBlur;
	static int quarter;
	static int quarterBlur;

	static int h;
	static int w;

	static int[] captureBuffers = new int[2];

	public static void startBloom() {
		sync();
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, ARBFramebufferObject.GL_COLOR_ATTACHMENT1, GL21.GL_TEXTURE_2D, full, 0);
		GL21.glDrawBuffers(captureBuffers);
	}

	public static void endBloom() {
		GL21.glDrawBuffers(mainColor);
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();


		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}

	private static void copySmaller() {
		drawFrame();
	}

	// PERF: reuse buffer
	private static void drawFrame() {
		final BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		bufferBuilder.begin(GL21.GL_QUADS, VertexFormats.POSITION_TEXTURE);
		bufferBuilder.vertex(0, 0, 500).texture(0, 0).next();
		bufferBuilder.vertex(w, 0, 500).texture(1, 0).next();
		bufferBuilder.vertex(w, h, 500).texture(1, 1).next();
		bufferBuilder.vertex(0, h, 500).texture(0, 1).next();
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
	}
	private static void sync() {
		final Framebuffer fbo = MinecraftClient.getInstance().getFramebuffer();
		final int main = fbo.fbo;

		if (main != mainFbo) {
			tearDown();
			mainFbo = main;
			full = TextureUtil.generateId();
			fullBlur = TextureUtil.generateId();
			half = TextureUtil.generateId();
			halfBlur = TextureUtil.generateId();
			quarter = TextureUtil.generateId();
			quarterBlur = TextureUtil.generateId();

			mainColor = fbo.colorAttachment;
			captureBuffers[0] = mainColor;
			captureBuffers[1] = full;

			w = fbo.textureWidth;
			h = fbo.textureHeight;

			GlStateManager.bindTexture(full);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w, h, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
			GlStateManager.bindTexture(fullBlur);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w, h, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);

			GlStateManager.bindTexture(half);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 2, h / 2, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
			GlStateManager.bindTexture(halfBlur);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 2, h / 2, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);

			GlStateManager.bindTexture(quarter);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 4, h / 4, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);
			GlStateManager.bindTexture(quarterBlur);
			GlStateManager.texImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA8, w / 4, h / 4, 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, (IntBuffer)null);

			GlStateManager.bindTexture(0);
		}
	}

	private static void tearDown() {
		if (full != -1) {
			TextureUtil.deleteId(full);
			TextureUtil.deleteId(fullBlur);
			TextureUtil.deleteId(half);
			TextureUtil.deleteId(halfBlur);
			TextureUtil.deleteId(quarter);
			TextureUtil.deleteId(quarterBlur);
			full = -1;
		}
	}
}
