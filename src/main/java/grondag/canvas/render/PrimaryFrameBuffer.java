package grondag.canvas.render;

import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.TextureUtil;

public class PrimaryFrameBuffer extends Framebuffer {
	public int[] extraColorAttachments;
	public float[][] clearColors;

	public PrimaryFrameBuffer(int width, int height, boolean getError) {
		super(width, height, true, getError);
	}

	@Override
	public void delete() {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		endRead();
		endWrite();

		if (depthAttachment > -1) {
			TextureUtil.deleteId(depthAttachment);
			depthAttachment = -1;
		}

		if (colorAttachment > -1) {
			TextureUtil.deleteId(colorAttachment);
			colorAttachment = -1;
		}

		if (fbo > -1) {
			GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, 0);
			GlStateManager.deleteFramebuffers(fbo);
			fbo = -1;
		}
	}

	@Override
	public void initFbo(int width, int height, boolean getError) {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		viewportWidth = width;
		viewportHeight = height;
		textureWidth = width;
		textureHeight = height;
		fbo = GlStateManager.genFramebuffers();
		colorAttachment = TextureUtil.generateId();

		depthAttachment = TextureUtil.generateId();
		GlStateManager.bindTexture(depthAttachment);
		GlStateManager.texParameter(3553, 10241, 9728);
		GlStateManager.texParameter(3553, 10240, 9728);
		GlStateManager.texParameter(3553, 10242, 10496);
		GlStateManager.texParameter(3553, 10243, 10496);
		GlStateManager.texParameter(3553, 34892, 0);
		GlStateManager.texImage2D(3553, 0, 6402, textureWidth, textureHeight, 0, 6402, 5126, (IntBuffer ) null);

		setTexFilter(9728);
		GlStateManager.bindTexture(colorAttachment);
		GlStateManager.texImage2D(3553, 0, 32856, textureWidth, textureHeight, 0, 6408, 5121, (IntBuffer) null);
		GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fbo);
		GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, 3553, colorAttachment, 0);

		if (useDepthAttachment) {
			GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.DEPTH_ATTACHMENT, 3553, depthAttachment, 0);
		}

		checkFramebufferStatus();
		clear(getError);
		endRead();
	}
}
