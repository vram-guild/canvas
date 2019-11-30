package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.SystemUtil;

import grondag.canvas.Configurator;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@ModifyArg (method = "renderCenter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;updateChunks(J)V"))
	long hookChunkBudget(long nanos) {
		final long val = SystemUtil.getMeasuringTimeNano() + Configurator.minChunkBudgetNanos;
		return Math.max(nanos, val);
	}
}
