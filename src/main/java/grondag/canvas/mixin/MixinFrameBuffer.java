package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.gl.Framebuffer;

import grondag.canvas.mixinterface.FrameBufferExt;

@Mixin(Framebuffer.class)
public abstract class MixinFrameBuffer implements FrameBufferExt {
	@Shadow private int colorAttachment;

	@Override
	public int canvas_colorAttachment() {
		return colorAttachment;
	}
}
