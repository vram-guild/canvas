package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.render.GuiLighting;

@Mixin(GuiLighting.class)
public class MixinGuiLighting {

    private static boolean skip = false;
    
    @Overwrite
    public static void disable() {
        if(skip) {
            skip = false;
        } else {
            GlStateManager.disableLighting();
            GlStateManager.disableLight(0);
            GlStateManager.disableLight(1);
            GlStateManager.disableColorMaterial();
        }
    }
    
    @Overwrite
    public static void enableForItems() {
        skip = true;
     }
}
