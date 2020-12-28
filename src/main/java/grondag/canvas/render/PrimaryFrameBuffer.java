package grondag.canvas.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.Framebuffer;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineManager;

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

		// WIP: need to signal pipeline to close anything?
	}

	@Override
	public void initFbo(int width, int height, boolean getError) {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		viewportWidth = width;
		viewportHeight = height;
		textureWidth = width;
		textureHeight = height;

		// UGLY - throwing away what seems to be a spurious INVALID_VALUE error here
		GlStateManager.getError();

		PipelineManager.init(width, height);

		fbo = Pipeline.getFramebuffer("default").glId();
		colorAttachment = Pipeline.getImage("default_main").glId();
		depthAttachment = Pipeline.getImage("default_depth").glId();

		checkFramebufferStatus();
		clear(getError);
		endRead();
	}
}
