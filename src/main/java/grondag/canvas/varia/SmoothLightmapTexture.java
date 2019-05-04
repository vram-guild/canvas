package grondag.canvas.varia;

import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.Canvas;
import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.QuadViewImpl;
import grondag.fermion.structures.SimpleUnorderedArrayList;
import io.netty.util.internal.ThreadLocalRandom;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SmoothLightmapTexture implements AutoCloseable {
    private static SmoothLightmapTexture instance;
    
    public static SmoothLightmapTexture instance() {
        SmoothLightmapTexture result = instance;
        if(result == null) {
            result = new SmoothLightmapTexture();
            instance = result;
        }
        return result;
    }
    
    private final NativeImageBackedTexture texture;
    private final NativeImage image;
    private final Identifier textureIdentifier;
    private final MinecraftClient client;

    private final Long2ObjectOpenHashMap<ShadeMap> maps = new Long2ObjectOpenHashMap<>();
    
    private final SimpleUnorderedArrayList<ShadeMap> loadlist = new SimpleUnorderedArrayList<ShadeMap>();
    
    private final int[][] randomBits = new int[512][512];
    
    private boolean needsInitialized = true;
    
    private SmoothLightmapTexture() {
        this.client = MinecraftClient.getInstance();
        this.texture = new NativeImageBackedTexture(512, 512, false);
        this.textureIdentifier = this.client.getTextureManager().registerDynamicTexture("light_map", this.texture);
        this.image = this.texture.getImage();
    }

    public void forceReload() {
        maps.clear();
        loadlist.clear();
        
    }
    
    @Override
    public void close() {
        this.texture.close();
    }

    public void disable() {
        if(!Configurator.enableSmoothLightmaps) {
            return;
        }
        
        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        GlStateManager.disableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    public void enable() {
        if(!Configurator.enableSmoothLightmaps) {
            return;
        }
        
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
        if(!Configurator.enableSmoothLightmaps) {
            return;
        }
        
        final boolean hasLoad = !loadlist.isEmpty();
        
        if (hasLoad || needsInitialized) {
            final NativeImage image = this.image;
            
            if(needsInitialized) {
                final Random r = ThreadLocalRandom.current();
                for(int u = 0; u < 512; u++) {
                    for(int v = 0; v < 512; v++) {
                        final int p = r.nextInt(256) << 24;
                        randomBits[u][v] = p;
                        image.setPixelRGBA(u, v, p);
                    }
                }
                needsInitialized = false;
            }
            
            if(!loadlist.isEmpty()) {
                synchronized(this) {
                    final int limit = loadlist.size();
                    for(int i = 0; i < limit; i++) {
                        ShadeMap map = loadlist.get(i);
                        assert map != null;
                        final int u = map.u;
                        final int v = map.v;
                        image.setPixelRGBA(u, v, randomBits[u][v] | map.b0 | (map.s0 << 8));
                        image.setPixelRGBA(u + 1, v, randomBits[u + 1][v] | map.b1 | (map.s1 << 8));
                        image.setPixelRGBA(u + 1, v + 1, randomBits[u + 1][v + 1] | map.b2 | (map.s2 << 8));
                        image.setPixelRGBA(u, v + 1, randomBits[u][v + 1] | map.b3 | (map.s3 << 8));
                    }
                    loadlist.clear();
                }
            }
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
    
    static long lightKey(QuadViewImpl q) {
        int l = q.lightmap(0);
        long b0 = l & 0xFF;
        long s0 = ((l >> 16) & 0xFF);
        
        l = q.lightmap(1);
        long b1 = (l & 0xFF);
        long s1 = ((l >> 16) & 0xFF);
        
        l = q.lightmap(2);
        long b2 = (l & 0xFF);
        long s2 = ((l >> 16) & 0xFF);
        
        l = q.lightmap(3);
        long b3 = (l & 0xFF);
        long s3 = ((l >> 16) & 0xFF);
        
        long result = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24) | (s0 << 32) | (s1 << 40) | (s2 << 48) | (s3 << 56);
//        Canvas.LOG.info(String.format("lightKey key = %d", result));
//        Canvas.LOG.info(String.format("lightKey %d, %d, %d, %d    %d, %d, %d, %d", b0, b1, b2, b3, s0, s1, s2, s3));
        return result;
    }
    
    public class ShadeMap {
        final int b0;
        final int b1;
        final int b2;
        final int b3;
        final int s0;
        final int s1;
        final int s2;
        final int s3;
        
        final int u;
        final int v;
        
        ShadeMap(long lightKey, QuadViewImpl q, float[] ao) {
            final int index = maps.size();
//            b0 = (int) (lightKey & 0xFFL);
//            b1 = (int) ((lightKey >>> 8) & 0xFFL);
//            b2 = (int) ((lightKey >>> 16) & 0xFFL);
//            b3 = (int) ((lightKey >>> 24) & 0xFFL);
//            s0 = (int) ((lightKey >>> 32) & 0xFFL);
//            s1 = (int) ((lightKey >>> 40) & 0xFFL);
//            s2 = (int) ((lightKey >>> 48) & 0xFFL);
//            s3 = (int) ((lightKey >>> 56) & 0xFFL);
//            b0 = 0xFF;
//            b1 = 0XFF;
//            b2 = 0;
//            b3 = 0;
//            s0 = 0;
//            s1 = 0;
//            s2 = 0;
//            s3 = 0xFF;
            
            int l = q.lightmap(0);
            b0 = l & 0xFF;
            s0 = (l >> 16) & 0xFF;
            
            l = q.lightmap(1);
            b1 = l & 0xFF;
            s1 = (l >> 16) & 0xFF;
            
            l = q.lightmap(2);
            b2 = l & 0xFF;
            s2 = (l >> 16) & 0xFF;
            
            l = q.lightmap(3);
            b3 = l & 0xFF;
            s3 = (l >> 16) & 0xFF;
            
            this.u = (index & 0xFF) * 2;
            this.v = (index >> 8) * 2;
            
//            Canvas.LOG.info(String.format("shadeMap key = %d", lightKey));
//            Canvas.LOG.info(String.format("shadeMap %d, %d, %d, %d    %d, %d, %d, %d", b0, b1, b2, b3, s0, s1, s2, s3));
        }

        public int lightCoord(int vertexIndex) {
            switch(vertexIndex) {
                case 0:
                    return u | (v << 16);
                case 1:
                    return (u + 1) | (v << 16);
                case 2:
                    return (u + 1) | ((v + 1)  << 16);
                case 3:
                    return u | ((v + 1)<< 16);
            }
            return 0;
        }
    }
    
    // PERF use primitive instead?
    public ShadeMap shadeMap(QuadViewImpl q, float[] ao) {
        long key = lightKey(q);
//        int key = Math.round(a * 255) & 0xFF; 
//        key |= (Math.round(b * 255) & 0xFF) << 8;
//        key |= (Math.round(c * 255) & 0xFF) << 16;
//        key |= (Math.round(d * 255) & 0xFF) << 24;
        
        ShadeMap result = maps.get(key);
        if(result == null) {
            synchronized(this) {
                result = maps.get(key);
                if(result == null) {
                    result = new ShadeMap(key, q, ao);
                    maps.put(key, result);
                    assert result != null;
                    loadlist.add(result);
                    //TODO: remove
//                    Canvas.LOG.info(String.format("Added %f, %f, %f, %f  count = %d", a, b, c, d, maps.size()));
                }
            }
        } else {
//            Canvas.LOG.info(String.format("GET key = %d", key));
//            Canvas.LOG.info(String.format("GET %d, %d, %d, %d    %d, %d, %d, %d", result.b0, result.b1, result.b2, result.b3, result.s0, result.s1, result.s2, result.s3));

            int l = q.lightmap(0);
            assert result.b0 == (l & 0xFF);
            assert result.s0 == ((l >> 16) & 0xFF);
            
            l = q.lightmap(1);
            assert result.b1 == (l & 0xFF);
            assert result.s1 == ((l >> 16) & 0xFF);
            
            l = q.lightmap(2);
            assert result.b2 == (l & 0xFF);
            assert result.s2 == ((l >> 16) & 0xFF);
            
            l = q.lightmap(3);
            assert result.b3 == (l & 0xFF);
            assert result.s3 == ((l >> 16) & 0xFF);
        }

        return result;
    }

    public ShadeMap shadeMap(QuadViewImpl q) {
        return shadeMap(q, null);
    }
}