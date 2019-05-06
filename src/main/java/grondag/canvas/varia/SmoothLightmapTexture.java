package grondag.canvas.varia;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.QuadViewImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

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

//    private final Long2ObjectOpenHashMap<LightMap> maps = new Long2ObjectOpenHashMap<>();
//
//    private final SimpleUnorderedArrayList<LightMap> loadlist = new SimpleUnorderedArrayList<LightMap>();

    private SmoothLightmapTexture() {
        this.client = MinecraftClient.getInstance();
        this.texture = new NativeImageBackedTexture(512, 512, false);
        this.textureIdentifier = this.client.getTextureManager().registerDynamicTexture("light_map", this.texture);
        this.image = this.texture.getImage();
    }

    public void forceReload() {
//        maps.clear();
//        loadlist.clear();
        LightmapHD.forceReload();
    }

    @Override
    public void close() {
        this.texture.close();
    }

    public void disable() {
        //UGLY doesn't belong here
        DitherTexture.instance().disable();
        if(!Configurator.enableSmoothLightmaps) {
            return;
        }

        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        GlStateManager.disableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    public void enable() {
        //UGLY doesn't belong here
        DitherTexture.instance().enable();
        if(!Configurator.enableSmoothLightmaps) {
            return;
        }

        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        this.client.getTextureManager().bindTexture(this.textureIdentifier);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D,  GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    
    World world;
    GameRenderer gameRenderer;
    float worldAmbient;
    float fluidModifier;
    float effectModifier;
    float skyDarkness;
    
    private boolean prepareForRefresh(float tick) {
        world = client.world;
        if (world == null) {
            return false;
        }
        gameRenderer = client.gameRenderer;
        skyDarkness = gameRenderer.getSkyDarkness(tick);
        worldAmbient = world.getAmbientLight(1.0F);
        fluidModifier = this.client.player.method_3140();
        if (this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            effectModifier = gameRenderer.getNightVisionStrength(client.player, tick);
        } else if (fluidModifier > 0.0F && this.client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
            effectModifier = fluidModifier;
        } else {
            effectModifier = 0.0F;
        }
        return true;
    }
    
    boolean isDirty = false;
    public void tick() {
        isDirty = true;
    }
    
    public void update(float tick, float flickerIn) {
        //UGLY doesn't belong here
        DitherTexture.instance().tick();
        
        if(!isDirty || !Configurator.enableSmoothLightmaps || !prepareForRefresh(tick)) {
            return;
        }

        isDirty = false;
        
        final NativeImage image = this.image;
        LightmapHD.forEach( map -> {
            //PERF - update a pallette vs every pixel
            final int uMin = map.uMinImg;
            final int vMin = map.vMinImg;
            for(int u = 0; u < 4; u++) {
                for(int v = 0; v < 4; v++) {
                    image.setPixelRGBA(uMin + u, vMin + v, update(hack(map.sky[u][v]), hack(map.block[u][v]), flickerIn));
                }
            }
        });
        this.texture.upload();
    }

    static final int hack(float b) {
        int result = Math.round(b * 17);
        return Math.max(0, Math.min(255, result));
    }
    //TODO: remove all of this below
    
    float prevFlicker;
    
    // commented lightmap texture manager code
    @SuppressWarnings("unused")
    private void updateWorldlight(float tick) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final World world = client.world;
        final GameRenderer gameRenderer = client.gameRenderer;
        if (world != null) {
            final float worldAmbient = world.getAmbientLight(1.0F);
            final float fluidModifier = this.client.player.method_3140();
            final float effectModifier;
            if (this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                effectModifier = gameRenderer.getNightVisionStrength(client.player, tick);
            } else if (fluidModifier > 0.0F && this.client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
                effectModifier = fluidModifier;
            } else {
                effectModifier = 0.0F;
            }

            for(int skyCounter = 0; skyCounter < 16; ++skyCounter) {
                for(int blockCounter = 0; blockCounter < 16; ++blockCounter) {
                    
                    final float dimSky = world.getTicksSinceLightning() > 0
                            ? world.dimension.getLightLevelToBrightness()[skyCounter]
                                    : world.dimension.getLightLevelToBrightness()[skyCounter] * (worldAmbient * 0.95F + 0.05F);

                    final float blockRed = world.dimension.getLightLevelToBrightness()[blockCounter] * (this.prevFlicker * 0.1F + 1.5F);
                    final float blockGreen = blockRed * ((blockRed * 0.6F + 0.4F) * 0.6F + 0.4F);
                    final float blockBlue = blockRed * (blockRed * blockRed * 0.6F + 0.4F);
                    
                    float r = dimSky * (worldAmbient * 0.65F + 0.35F) + blockRed;
                    float g = dimSky * (worldAmbient * 0.65F + 0.35F) + blockGreen;
                    float b = dimSky + blockBlue;
                    
                    // all stuff after this operates on combined light
                    
                    r = r * 0.96F + 0.03F;
                    g = g * 0.96F + 0.03F;
                    b = b * 0.96F + 0.03F;
                    
                    if (gameRenderer.getSkyDarkness(tick) > 0.0F) {
                        float gamma = gameRenderer.getSkyDarkness(tick);
                        r = r * (1.0F - gamma) + r * 0.7F * gamma;
                        g = g * (1.0F - gamma) + g * 0.6F * gamma;
                        b = b * (1.0F - gamma) + b * 0.6F * gamma;
                    }

                    // OVERRIDES COMPLETELY
                    if (world.dimension.getType() == DimensionType.THE_END) {
                        r = 0.22F + blockRed * 0.75F;
                        g = 0.28F + blockGreen * 0.75F;
                        b = 0.25F + blockBlue * 0.75F;
                    }

                    if (effectModifier > 0.0F) {
                        float gamma = 1.0F / r;
                        if (gamma > 1.0F / g) {
                            gamma = 1.0F / g;
                        }

                        if (gamma > 1.0F / b) {
                            gamma = 1.0F / b;
                        }

                        r = r * (1.0F - effectModifier) + r * gamma * effectModifier;
                        g = g * (1.0F - effectModifier) + g * gamma * effectModifier;
                        b = b * (1.0F - effectModifier) + b * gamma * effectModifier;
                    }

                    if (r > 1.0F) {
                        r = 1.0F;
                    }

                    if (g > 1.0F) {
                        g = 1.0F;
                    }

                    if (b > 1.0F) {
                        b = 1.0F;
                    }

                    // gamma correction - colors already blended
                    final float gconf = (float)this.client.options.gamma;
                    r = gammaCorrect(gconf, r);
                    g = gammaCorrect(gconf, g);
                    b = gammaCorrect(gconf, b);

                    int int_4 = (int)(r * 255.0F);
                    int int_5 = (int)(g * 255.0F);
                    int int_6 = (int)(b * 255.0F);
                    this.image.setPixelRGBA(blockCounter, skyCounter, -16777216 | int_6 << 16 | int_5 << 8 | int_4);
                }
            }

            this.texture.upload();
            this.client.getProfiler().pop();
        }
    }
    
    static final float gammaCorrect(float gamma, float color) {
        float inv = 1.0F - color;
        inv = 1.0F - inv * inv * inv * inv;
        color = color * (1.0F - gamma) + inv * gamma;
        color = color * 0.96F + 0.03F;

        if (color > 1.0F) {
            color = 1.0F;
        }

        if (color < 0.0F) {
            color = 0.0F;
        }
        return color;
    }

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
        return result;
    }
    
    private float interpolatedLight(float[] light, int b255) {
        final int lowbits = b255 & 0xF;
        final int floor = b255 >> 4;
        if(lowbits == 0) {
            return light[floor];
        } else {
            float frac = (b255 & 0xF) / 15f;
            return light[floor] * (1f - frac) + light[floor + 1] * frac;
        }
    }
    
    private int update(int sky255, int block255, float flicker) {
        
        final float lightning = world.getTicksSinceLightning() > 0 ? 1.0f : (worldAmbient * 0.95F + 0.05F);
        final float dimSky = lightning * interpolatedLight(world.dimension.getLightLevelToBrightness(), sky255);

        final float dimBlock = interpolatedLight(world.dimension.getLightLevelToBrightness(), block255);

        final float blockRed = dimBlock * (flicker * 0.1F + 1.5F);
        final float blockGreen = blockRed * ((blockRed * 0.6F + 0.4F) * 0.6F + 0.4F);
        final float blockBlue = blockRed * (blockRed * blockRed * 0.6F + 0.4F);
        
        float r = dimSky * (worldAmbient * 0.65F + 0.35F) + blockRed;
        float g = dimSky * (worldAmbient * 0.65F + 0.35F) + blockGreen;
        float b = dimSky + blockBlue;
        
        // all stuff after this operates on combined light
        
        r = r * 0.96F + 0.03F;
        g = g * 0.96F + 0.03F;
        b = b * 0.96F + 0.03F;
        
        if (skyDarkness > 0.0F) {
            final float sd = skyDarkness;
            r = r * (1.0F - sd) + r * 0.7F * sd;
            g = g * (1.0F - sd) + g * 0.6F * sd;
            b = b * (1.0F - sd) + b * 0.6F * sd;
        }

        // OVERRIDES COMPLETELY
        if (world.dimension.getType() == DimensionType.THE_END) {
            r = 0.22F + blockRed * 0.75F;
            g = 0.28F + blockGreen * 0.75F;
            b = 0.25F + blockBlue * 0.75F;
        }

        if (effectModifier > 0.0F) {
            float gamma = 1.0F / r;
            if (gamma > 1.0F / g) {
                gamma = 1.0F / g;
            }

            if (gamma > 1.0F / b) {
                gamma = 1.0F / b;
            }

            r = r * (1.0F - effectModifier) + r * gamma * effectModifier;
            g = g * (1.0F - effectModifier) + g * gamma * effectModifier;
            b = b * (1.0F - effectModifier) + b * gamma * effectModifier;
        }

        if (r > 1.0F) {
            r = 1.0F;
        }

        if (g > 1.0F) {
            g = 1.0F;
        }

        if (b > 1.0F) {
            b = 1.0F;
        }

        // gamma correction - colors already blended
        final float gconf = (float)client.options.gamma;
        r = gammaCorrect(gconf, r);
        g = gammaCorrect(gconf, g);
        b = gammaCorrect(gconf, b);

        int red = (int)(r * 255.0F);
        int green = (int)(g * 255.0F);
        int blue = (int)(b * 255.0F);
        return 0xFF000000 | (blue << 16) | (green << 8) | red;
    }

//    public class LightMap {
//        final int b0;
//        final int b1;
//        final int b2;
//        final int b3;
//        final int s0;
//        final int s1;
//        final int s2;
//        final int s3;
//
//        final int u;
//        final int v;
//
//        LightMap(long lightKey, QuadViewImpl q) {
//            final int index = maps.size();
//
//            int l = q.lightmap(0);
//            b0 = l & 0xFF;
//            s0 = (l >> 16) & 0xFF;
//
//            l = q.lightmap(1);
//            b1 = l & 0xFF;
//            s1 = (l >> 16) & 0xFF;
//
//            l = q.lightmap(2);
//            b2 = l & 0xFF;
//            s2 = (l >> 16) & 0xFF;
//
//            l = q.lightmap(3);
//            b3 = l & 0xFF;
//            s3 = (l >> 16) & 0xFF;
//
//            this.u = (index & 0xFF) * 2;
//            this.v = (index >> 8) * 2;
//        }
//
//        //TODO: remove
////        public int lightCoord(int vertexIndex) {
////            final int m = 32;
////            
////            switch(vertexIndex) {
////            case 0:
////                return (u * m + 16) | ((v * m + 16) << 16);
////            case 1:
////                return ((u + 1) * m + 16) | ((v * m + 16) << 16);
////            case 2:
////                return ((u + 1) * m + 16) | (((v + 1) * m + 16) << 16);
////            case 3:
////                return (u * m + 16) | (((v + 1) * m + 16) << 16);
////            }
////            return 0;
////        }
//    }
//
//    public LightMap lightMap(QuadViewImpl q, float[] ao) {
//        long key = lightKey(q);
//
//        LightFaceData lfd = q.lightFaceData;
//        assert lfd != null;
////        final int b0 = (q.lightmap(0) & 0xFFFF);
////        final int b1 = (q.lightmap(1) & 0xFFFF);
////        final int b2 = (q.lightmap(2) & 0xFFFF);
////        final int b3 = (q.lightmap(3) & 0xFFFF);
////        
////        final int tb0 = lfd.weigtedBlockLight(q.w[0]);
////        final int tb1 = lfd.weigtedBlockLight(q.w[1]);
////        final int tb2 = lfd.weigtedBlockLight(q.w[2]);
////        final int tb3 = lfd.weigtedBlockLight(q.w[3]);
//        
////        assert tb0 == b0;
////        assert tb1 == b1;
////        assert tb2 == b2;
////        assert tb3 == b3;
//        
////        final int s0 = ((q.lightmap(0) >>> 16) & 0xFFFF);
////        final int s1 = ((q.lightmap(1) >>> 16) & 0xFFFF);
////        final int s2 = ((q.lightmap(2) >>> 16) & 0xFFFF);
////        final int s3 = ((q.lightmap(3) >>> 16) & 0xFFFF);
//        
////        assert lfd.s0 == s0;
////        assert lfd.s1 == s1;
////        assert lfd.s2 == s2;
////        assert lfd.s3 == s3;
//        
//        if(ao != null) {
//            ShadeFaceData sfd = q.shadeFaceData;
//            assert sfd != null;
//            assert ao[0] == sfd.ao0;
//            assert ao[1] == sfd.ao1;
//            assert ao[2] == sfd.ao2;
//            assert ao[3] == sfd.ao3;
//        }
//        
//        LightMap result = maps.get(key);
//        if(result == null) {
//            synchronized(this) {
//                result = maps.get(key);
//                if(result == null) {
//                    result = new LightMap(key, q);
//                    maps.put(key, result);
//                    assert result != null;
//                    loadlist.add(result);
//                }
//            }
//        }
//        return result;
//    }
//
//    public LightMap shadeMap(QuadViewImpl q) {
//        return lightMap(q, null);
//    }
}