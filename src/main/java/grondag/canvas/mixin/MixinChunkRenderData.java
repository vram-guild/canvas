package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.chunk.ChunkBuilder.ChunkData;

import grondag.canvas.CanvasMod;


@Mixin(ChunkData.class)
public class MixinChunkRenderData {
	private static boolean shouldWarn = true;

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void onNew(CallbackInfo ci) {
		if (shouldWarn) {
			CanvasMod.LOG.warn("[Canvas] ChunkData instantiated unexpectedly. This probably indicates a mod incompatibility.");
			shouldWarn = false;
		}
	}
}
