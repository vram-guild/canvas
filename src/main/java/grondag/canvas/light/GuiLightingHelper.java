package grondag.canvas.light;

import com.mojang.blaze3d.platform.GlStateManager;


public abstract class GuiLightingHelper {
    private static boolean enabled = false;
    
    public static void suspend() {
        if(enabled) {
            GlStateManager.disableLighting();
            //GlStateManager.disableLight(0);
            //GlStateManager.disableLight(1);
            GlStateManager.disableColorMaterial();
        }
    }
    
    public static void resume() {
        if(enabled) {
            GlStateManager.enableLighting();
            //GlStateManager.enableLight(0);
            //GlStateManager.enableLight(1);
            GlStateManager.enableColorMaterial();
        }
    }
    
    public static void notifyStatus(boolean enabledIn) {
        enabled = enabledIn;
    }
}
