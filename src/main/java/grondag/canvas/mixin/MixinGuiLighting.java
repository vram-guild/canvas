package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.render.GuiLighting;

@Mixin(GuiLighting.class)
public class MixinGuiLighting {

    private static boolean skip = false;
    
    @Inject(method = "disable", at = @At("HEAD"), cancellable = true, require = 1)
    private static void onDisable(CallbackInfo ci) {
        if(skip) {
            skip = false;
        } else {
            GlStateManager.disableLighting();
            GlStateManager.disableLight(0);
            GlStateManager.disableLight(1);
            GlStateManager.disableColorMaterial();
        }
        ci.cancel();
    }
    
    @Inject(method = "enableForItems", at = @At("HEAD"), cancellable = true, require = 1)
    private static void enableForItems(CallbackInfo ci) {
        skip = true;
        ci.cancel();
     }
}
