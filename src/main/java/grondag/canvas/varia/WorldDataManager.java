package grondag.canvas.varia;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.World;

//TODO: still need this?  How much of it?
public class WorldDataManager {
    public static int LENGTH = 8;
    public static int  WORLD_EFFECT_MODIFIER = 0;
    
    static float[] UNIFORM_DATA = new float[LENGTH];
    
    public static float[] uniformData() {
        return UNIFORM_DATA;
    }
    
    static float lastFlicker;
    
    public static void updateLight(float tick, float flicker) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final World world = client.world;
        final GameRenderer gameRenderer = client.gameRenderer;
        
        if (world != null) {
            final float fluidModifier = client.player.method_3140();
            if (client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                UNIFORM_DATA[WORLD_EFFECT_MODIFIER] = gameRenderer.getNightVisionStrength(client.player, tick);
            } else if (fluidModifier > 0.0F && client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
                UNIFORM_DATA[WORLD_EFFECT_MODIFIER] = fluidModifier;
            } else {
                UNIFORM_DATA[WORLD_EFFECT_MODIFIER] = 0.0F;
            }
        }
    }
}
