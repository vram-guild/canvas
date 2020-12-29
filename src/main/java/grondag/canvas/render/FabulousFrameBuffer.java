package grondag.canvas.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import grondag.canvas.pipeline.PipelineManager;

public class FabulousFrameBuffer extends Framebuffer {
	public FabulousFrameBuffer(int fboId, int colorId, int depthId) {
		super(PipelineManager.width(), PipelineManager.height(), true, MinecraftClient.IS_SYSTEM_MAC);
		setClearColor(0.0F, 0.0F, 0.0F, 0.0F);

		fbo = fboId;
		colorAttachment = colorId;
		depthAttachment = depthId;

		checkFramebufferStatus();
		endRead();
	}

	@Override
	public void delete() {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		endRead();
		endWrite();

		// nothing to do here - pipeline will clean up
	}

	@Override
	public void initFbo(int width, int height, boolean getError) {
		RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
		viewportWidth = width;
		viewportHeight = height;
		textureWidth = width;
		textureHeight = height;

		// rest is handled in init that accepts IDs from pipeline
	}

	@Override
	public void clear(boolean getError) {
		// NOOP - should be done in pipeline buffers
		assert false : "Unmanaged frambuffer clear";
	}
}
