package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.thread.ReentrantThreadExecutor;

import grondag.canvas.Configurator;
import grondag.canvas.mixinterface.MinecraftClientExt;
import grondag.canvas.render.CanvasWorldRenderer;
import grondag.canvas.varia.CanvasGlHelper;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable> implements MinecraftClientExt {
	@Shadow ItemColors itemColors;

	protected MixinMinecraftClient(String dummy) {
		super(dummy);
	}

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void hookInit(CallbackInfo info) {
		CanvasGlHelper.init();
	}

	@Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"), method = "render", require = 1, allow = 1)
	private void onYield() {
		if (!Configurator.greedyRenderThread) {
			Thread.yield();
		}
	}

	@Redirect(method = "<init>*", at = @At(value = "NEW", target = "(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/render/BufferBuilderStorage;)Lnet/minecraft/client/render/WorldRenderer;"))
	private WorldRenderer onWorldRendererNew(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		return new CanvasWorldRenderer(client, bufferBuilders);
	}

	@Override
	public ItemColors canvas_itemColors() {
		return itemColors;
	}
}
