package grondag.canvas.varia;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.Canvas;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class UtilityTexture implements AutoCloseable {
    private final NativeImageBackedTexture texture;
    private final NativeImage image;
    private final Identifier textureIdentifier;
    private final MinecraftClient client;

    private final Int2ObjectOpenHashMap<ShadeMap> maps = new Int2ObjectOpenHashMap<> ();
    
    private final ObjectArrayList<ShadeMap> loadlist = new ObjectArrayList<ShadeMap>();
    
    private static UtilityTexture instance;
    
    public static UtilityTexture instance() {
        UtilityTexture result = instance;
        if(result == null) {
            result = new UtilityTexture();
            instance = result;
        }
        return result;
    }
        
    private UtilityTexture() {
        this.client = MinecraftClient.getInstance();
        this.texture = new NativeImageBackedTexture(512, 512, false);
        this.textureIdentifier = this.client.getTextureManager().registerDynamicTexture("light_map", this.texture);
        this.image = this.texture.getImage();
    }

    @Override
    public void close() {
        this.texture.close();
    }

    public void disable() {
        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        GlStateManager.disableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    public void enable() {
        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
//        GlStateManager.matrixMode(5890);
//        GlStateManager.loadIdentity();
//        float float_1 = 0.00390625F;
//        GlStateManager.scalef(0.00390625F, 0.00390625F, 0.00390625F);
//        GlStateManager.translatef(8.0F, 8.0F, 8.0F);
//        GlStateManager.matrixMode(5888);
        this.client.getTextureManager().bindTexture(this.textureIdentifier);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D,  GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        // we don't wrap so shouldn't need these
//        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, 10242, 10496);
//        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, 10243, 10496);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    public void tick() {
        if (!loadlist.isEmpty()) {
            final NativeImage image = this.image;
            
            do {
                ShadeMap map = loadlist.pop();
                image.setPixelRGBA(map.u, map.v, map.key & 0xFF);
                image.setPixelRGBA(map.u + 1, map.v, (map.key >>> 8) & 0xFF);
                image.setPixelRGBA(map.u, map.v + 1, (map.key >>> 16) & 0xFF);
                image.setPixelRGBA(map.u + 1, map.v + 1, (map.key >>> 24) & 0xFF);
            } while(!loadlist.isEmpty());

            this.texture.upload();
        }
    }
    
    // commented lightmap texture manager code
//  public void update(float tick) {
//     if (this.isDirty) {
//        World world = this.client.world;
//        if (world != null) {
//           float worldAmbient = world.getAmbientLight(1.0F);
//           float clampedWorldAmbient = worldAmbient * 0.95F + 0.05F;
//           float fluidModifier = this.client.player.method_3140();
//           float effectModifier;
//           if (this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
//              effectModifier = this.worldRenderer.getNightVisionStrength(this.client.player, tick);
//           } else if (fluidModifier > 0.0F && this.client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
//              effectModifier = fluidModifier;
//           } else {
//              effectModifier = 0.0F;
//           }
//
//           for(int skyCounter = 0; skyCounter < 16; ++skyCounter) {
//              for(int blockCounter = 0; blockCounter < 16; ++blockCounter) {
//                 final float dimBlock = world.dimension.getLightLevelToBrightness()[blockCounter] * (this.prevFlicker * 0.1F + 1.5F);
//                 final float dimSky = world.getTicksSinceLightning() > 0
//                         ? world.dimension.getLightLevelToBrightness()[skyCounter]
//                         : world.dimension.getLightLevelToBrightness()[skyCounter] * clampedWorldAmbient;
//
//                 final float skyWithWorld0 = dimSky * (worldAmbient * 0.65F + 0.35F);
//                 final float skyWithWorld1 = dimSky * (worldAmbient * 0.65F + 0.35F);
//                 final float blockSquarish = dimBlock * ((dimBlock * 0.6F + 0.4F) * 0.6F + 0.4F);
//                 final float blockCubish = dimBlock * (dimBlock * dimBlock * 0.6F + 0.4F);
//                 float r = skyWithWorld0 + dimBlock;
//                 float g = skyWithWorld1 + blockSquarish;
//                 float b = dimSky + blockCubish;
//                 r = r * 0.96F + 0.03F;
//                 g = g * 0.96F + 0.03F;
//                 b = b * 0.96F + 0.03F;
//                 float gamma;
//                 if (this.worldRenderer.getSkyDarkness(tick) > 0.0F) {
//                    gamma = this.worldRenderer.getSkyDarkness(tick);
//                    r = r * (1.0F - gamma) + r * 0.7F * gamma;
//                    g = g * (1.0F - gamma) + g * 0.6F * gamma;
//                    b = b * (1.0F - gamma) + b * 0.6F * gamma;
//                 }
//
//                 // OVERRIDES COMPLETELY
//                 if (world.dimension.getType() == DimensionType.THE_END) {
//                    r = 0.22F + dimBlock * 0.75F;
//                    g = 0.28F + blockSquarish * 0.75F;
//                    b = 0.25F + blockCubish * 0.75F;
//                 }
//
//                 if (effectModifier > 0.0F) {
//                    gamma = 1.0F / r;
//                    if (gamma > 1.0F / g) {
//                       gamma = 1.0F / g;
//                    }
//
//                    if (gamma > 1.0F / b) {
//                       gamma = 1.0F / b;
//                    }
//
//                    r = r * (1.0F - effectModifier) + r * gamma * effectModifier;
//                    g = g * (1.0F - effectModifier) + g * gamma * effectModifier;
//                    b = b * (1.0F - effectModifier) + b * gamma * effectModifier;
//                 }
//
//                 if (r > 1.0F) {
//                    r = 1.0F;
//                 }
//
//                 if (g > 1.0F) {
//                    g = 1.0F;
//                 }
//
//                 if (b > 1.0F) {
//                    b = 1.0F;
//                 }
//
//                 // gamma correction - colors already blended
//                 gamma = (float)this.client.options.gamma;
//                 float rInv = 1.0F - r;
//                 float gInv = 1.0F - g;
//                 float bInv = 1.0F - b;
//                 rInv = 1.0F - rInv * rInv * rInv * rInv;
//                 gInv = 1.0F - gInv * gInv * gInv * gInv;
//                 bInv = 1.0F - bInv * bInv * bInv * bInv;
//                 r = r * (1.0F - gamma) + rInv * gamma;
//                 g = g * (1.0F - gamma) + gInv * gamma;
//                 b = b * (1.0F - gamma) + bInv * gamma;
//                 r = r * 0.96F + 0.03F;
//                 g = g * 0.96F + 0.03F;
//                 b = b * 0.96F + 0.03F;
//                 
//                 if (r > 1.0F) {
//                    r = 1.0F;
//                 }
//
//                 if (g > 1.0F) {
//                    g = 1.0F;
//                 }
//
//                 if (b > 1.0F) {
//                    b = 1.0F;
//                 }
//
//                 if (r < 0.0F) {
//                    r = 0.0F;
//                 }
//
//                 if (g < 0.0F) {
//                    g = 0.0F;
//                 }
//
//                 if (b < 0.0F) {
//                    b = 0.0F;
//                 }
//
//                 int int_4 = (int)(r * 255.0F);
//                 int int_5 = (int)(g * 255.0F);
//                 int int_6 = (int)(b * 255.0F);
//                 this.image.setPixelRGBA(blockCounter, skyCounter, -16777216 | int_6 << 16 | int_5 << 8 | int_4);
//              }
//           }
//
//           this.texture.upload();
//           this.isDirty = false;
//           this.client.getProfiler().pop();
//        }
//     }
//  }
    
    public class ShadeMap {
        final int key;
        final int u;
        final int v;
        
        ShadeMap(int key) {
            this.key = key;
            final int index = maps.size();
            this.u = (index & 0xFF) * 2;
            this.v = (index >> 8) * 2;
        }
    }
    
    // PERF use primitive instead?
    public ShadeMap shadeMap(float a, float b, float c, float d) {
        int key = Math.round(a * 255) & 0xFF; 
        key |= (Math.round(b * 255) & 0xFF) << 8;
        key |= (Math.round(c * 255) & 0xFF) << 16;
        key |= (Math.round(d * 255) & 0xFF) << 24;
        
        ShadeMap result = maps.get(key);
        if(result == null) {
            synchronized(maps) {
                result = maps.get(key);
                if(result == null) {
                    result = new ShadeMap(key);
                    maps.put(key, result);
                    loadlist.add(result);
                    Canvas.LOG.info(String.format("Added %f, %f, %f, %f  count = %d", a, b, c, d, maps.size()));
                }
            }
        }
        return result;
    }
}