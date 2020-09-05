package grondag.canvas.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.hud.DebugHud;

import grondag.canvas.Configurator;
import grondag.canvas.buffer.GlBufferAllocator;
import grondag.canvas.buffer.TransferBufferAllocator;
import grondag.canvas.light.LightmapHd;

@Mixin(DebugHud.class)
public class MixinDebugHud {
	@Inject(method = "getLeftText", at = @At("RETURN"), cancellable = false, require = 1)
	private void onGetBufferBuilders(CallbackInfoReturnable<List<String>> ci) {
		final List<String> list = ci.getReturnValue();

		if(Configurator.hdLightmaps()) {
			list.add("HD Lightmap Occupancy: " + LightmapHd.occupancyReport());
		}

		list.add(TransferBufferAllocator.debugString());
		list.add(GlBufferAllocator.debugString());
	}
}
