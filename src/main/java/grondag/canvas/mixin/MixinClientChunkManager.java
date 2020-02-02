package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.mixinterface.WorldChunkExt;

@Environment(EnvType.CLIENT)
@Mixin(ClientChunkManager.class)
public class MixinClientChunkManager {

	@Inject(method = "loadChunkFromPacket", at = { @At(value = "RETURN") }, cancellable = false)
	private void onLoadChunkFromPacket(CallbackInfoReturnable<WorldChunk> ci) {
		final WorldChunk chunk = ci.getReturnValue();

		if(chunk != null) {
			((WorldChunkExt) chunk).canvas_clearColorCache();
		}
	}
}
