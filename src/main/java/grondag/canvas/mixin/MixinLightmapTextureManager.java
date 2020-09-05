package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.render.LightmapTextureManager;

import grondag.canvas.varia.WorldDataManager;

@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager {
	@ModifyArg(method = "update", index = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/texture/NativeImage;setPixelColor(III)V"))
	private int onSetPixelRgba(int i, int j, int color) {
		if(i == 15 && j == 15) {
			WorldDataManager.updateEmissiveColor(color);
		}

		return color;
	}
}
