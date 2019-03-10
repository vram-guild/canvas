package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import grondag.canvas.core.PipelineManager;
import net.minecraft.client.render.LightmapTextureManager;

@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager {
    @ModifyArg(method = "update", index = 2, at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/texture/NativeImage;setPixelRGBA(III)V"))
    private int onSetPixelRGBA(int i, int j, int color) {
        if(i == 15 && j == 15) {
            PipelineManager.INSTANCE.updateEmissiveColor(color);
        }
        return color;
    }
}
