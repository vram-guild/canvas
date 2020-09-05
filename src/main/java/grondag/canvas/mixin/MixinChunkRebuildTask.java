package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.CanvasMod;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk$RebuildTask")
public abstract class MixinChunkRebuildTask {
	private static boolean shouldWarn = true;

	@Inject(at = @At("RETURN"), method = "<init>*")
	private void onNew(CallbackInfo ci) {
		if (shouldWarn) {
			CanvasMod.LOG.warn("[Canvas] ChunkBuilder.BuiltChunk.RebuildTask instantiated unexpectedly. This probably indicates a mod incompatibility.");
			shouldWarn = false;
		}
	}
}
