package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;

import grondag.canvas.render.RenderLayerHandler;

@Mixin(BufferRenderer.class)
public abstract class MixinBufferRenderer {
	@Redirect(method = "Lnet/minecraft/client/render/BufferRenderer;draw(Ljava/nio/ByteBuffer;ILnet/minecraft/client/render/VertexFormat;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexFormat;startDrawing(J)V"))
	private static void onFormatStart(VertexFormat format, long address) {
		RenderLayerHandler.onFormatStart(format, address);
	}

	@Redirect(method = "Lnet/minecraft/client/render/BufferRenderer;draw(Ljava/nio/ByteBuffer;ILnet/minecraft/client/render/VertexFormat;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexFormat;endDrawing()V"))
	private static void onFormatEnd(VertexFormat format) {
		RenderLayerHandler.onFormatEnd(format);
	}
}
