package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.ChunkPos;

import grondag.canvas.CanvasMod;

@Mixin(SectionOcclusionGraph.class)
public class MixinSectionOcclusionGraph {

	@Unique
	private static boolean shouldWarnOnChunkLoaded = true;

	@Inject(at = @At("HEAD"), method = "onChunkLoaded", cancellable = true)
	private void onOnChunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
		if (shouldWarnOnChunkLoaded) {
			CanvasMod.LOG.warn("[Canvas] SectionOcclusionGraph.onChunkLoaded() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnChunkLoaded = false;
		}
	}

	@Unique
	private static boolean shouldWarnGetRelativeFrom = true;

	@Inject(at = @At("HEAD"), method = "getRelativeFrom", cancellable = true)
	private void onGetRelativeFrom(CallbackInfoReturnable<SectionRenderDispatcher.RenderSection> ci) {
		if (shouldWarnGetRelativeFrom) {
			CanvasMod.LOG.warn("[Canvas] SectionOcclusionGraph.getRelativeFrom() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.setReturnValue(null);
			shouldWarnGetRelativeFrom = false;
		}
	}

	@Unique
	private static boolean shouldWarnOnUpdateRenderChunks = true;

	@Inject(at = @At("HEAD"), method = "runUpdates", cancellable = true)
	private void onUpdateRenderChunks(CallbackInfo ci) {
		if (shouldWarnOnUpdateRenderChunks) {
			CanvasMod.LOG.warn("[Canvas] SectionOcclusionGraph.runUpdates() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnUpdateRenderChunks = false;
		}
	}
}
