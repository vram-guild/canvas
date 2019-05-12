package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.canvas.light.GuiLightingHelper;
import net.minecraft.client.render.GuiLighting;

@Mixin(GuiLighting.class)
public class MixinGuiLighting {

    @Inject(method = "disable", at = @At("HEAD"), cancellable = false, require = 1)
    private static void onDisable(CallbackInfo ci) {
        GuiLightingHelper.notifyStatus(false);
    }
    
    @Inject(method = "enable", at = @At("HEAD"), cancellable = false, require = 1)
    private static void onEnableForItems(CallbackInfo ci) {
        GuiLightingHelper.notifyStatus(true);
    }
}
