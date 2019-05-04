package grondag.canvas.varia;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.World;

public class WorldDataManager {
    public static int LENGTH = 32;
    public static int  WORLD_DIM_LIGHT_0 = 0;
    public static int  WORLD_DIM_LIGHT_LEN = 16;
    public static int  WORLD_GAMMA = 16;
    public static int  WORLD_SKY_DARKNESS = 17;
    public static int  WORLD_EFFECT_MODIFIER = 18;
    public static int  WORLD_FLICKER = 19;
    public static int  WORLD_AMBIENT = 20;
    
    static float[] UNIFORM_DATA = new float[LENGTH];
    
    public static float[] uniformData() {
        return UNIFORM_DATA;
    }
    
    static float lastFlicker;
    
    public static void updateLight(float tick, float flicker) {
        UNIFORM_DATA[WORLD_FLICKER] = flicker * 0.1F + 1.5F;
        
        final MinecraftClient client = MinecraftClient.getInstance();
        final World world = client.world;
        final GameRenderer gameRenderer = client.gameRenderer;
        UNIFORM_DATA[WORLD_SKY_DARKNESS] = gameRenderer.getSkyDarkness(tick);
        UNIFORM_DATA[WORLD_GAMMA] = (float)client.options.gamma;
        
        if (world != null) {
            System.arraycopy(world.dimension.getLightLevelToBrightness(), 0, UNIFORM_DATA, WORLD_DIM_LIGHT_0, WORLD_DIM_LIGHT_LEN);
            UNIFORM_DATA[WORLD_AMBIENT] = world.getAmbientLight(1.0F);
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
