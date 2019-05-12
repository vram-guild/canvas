package grondag.canvas.light;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.Configurator;
import grondag.canvas.varia.DitherTexture;
import grondag.canvas.varia.SimpleImage;
import grondag.canvas.varia.SimpleTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class LightmapHdTexture implements AutoCloseable {
    private static LightmapHdTexture instance;

    public static LightmapHdTexture instance() {
        LightmapHdTexture result = instance;
        if(result == null) {
            result = new LightmapHdTexture();
            instance = result;
        }
        return result;
    }

    private final SimpleTexture texture;
    private final SimpleImage image;
    private final Identifier textureIdentifier = new Identifier("canvas", "light_map");
    private final MinecraftClient client;

    private LightmapHdTexture() {
        this.client = MinecraftClient.getInstance();
        this.texture = new SimpleTexture(new SimpleImage(1, GL11.GL_RED, LightmapHd.TEX_SIZE, LightmapHd.TEX_SIZE, false), GL11.GL_RED);
        this.client.getTextureManager().registerTexture(textureIdentifier, this.texture);
        this.image = this.texture.getImage();
    }

    public void forceReload() {
        LightmapHd.forceReload();
    }

    private static final ConcurrentLinkedQueue<LightmapHd> updates = new ConcurrentLinkedQueue<>();
    
    public synchronized void setDirty(LightmapHd lightmap) {
        updates.add(lightmap);
    }
    
    @Override
    public void close() {
        this.texture.close();
    }

    public void disable() {
        //UGLY doesn't belong here
        DitherTexture.instance().disable();
        if(!Configurator.enableHdLightmaps) {
            return;
        }

        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        GlStateManager.disableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    public void enable() {
        //UGLY doesn't belong here
        DitherTexture.instance().enable();
        if(!Configurator.enableHdLightmaps) {
            return;
        }

        //UGLY: clean up and use ReliableImage methods instead
        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        this.client.getTextureManager().bindTexture(this.textureIdentifier);
        
        
        final int mode = Configurator.enableLightmapDebug ? GL11.GL_NEAREST : GL11.GL_LINEAR;
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D,  GL11.GL_TEXTURE_MAG_FILTER, mode);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    public void onRenderTick() {
        
        final SimpleImage image = this.image;
        int uMin = Integer.MAX_VALUE;
        int vMin = Integer.MAX_VALUE;
        int uMax = Integer.MIN_VALUE;
        int vMax = Integer.MIN_VALUE;

        LightmapHd map;
        while((map = updates.poll()) != null) {
            final int uMap = map.uMinImg;
            final int vMap = map.vMinImg;
            
            for(int u = 0; u < LightmapHd.PADDED_SIZE; u++) {
                for(int v = 0; v < LightmapHd.PADDED_SIZE; v++) {
                    image.setLuminance(uMap + u, vMap + v, (byte)map.pixel(u,v));
                }
            }
            
            uMin = Math.min(uMin, uMap);
            vMin = Math.min(vMin, vMap);
            uMax = Math.max(uMax, uMap + LightmapHd.PADDED_SIZE);
            vMax = Math.max(vMax, vMap + LightmapHd.PADDED_SIZE);
        }
        
        
        if(uMin == Integer.MAX_VALUE) {
            return;
        }
        
        uMin = (uMin / 4) * 4;
        final int w = ((uMax - uMin + 3) / 4) * 4;
        
        this.texture.uploadPartial(uMin, vMin, w, vMax - vMin);
    }
}