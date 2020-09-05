package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.mixinterface.FogStateExt;
import grondag.canvas.varia.FogStateExtHolder;

@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$FogState")
public abstract class MixinFogState implements FogStateExt {
	@Shadow public int mode;

	@Override
	public int getMode() {
		return mode;
	}

	@Inject(method = "<init>()V", require = 1, at = @At("RETURN"))
	private void onConstructed(CallbackInfo ci) {
		FogStateExtHolder.INSTANCE = (this);
	}
}
