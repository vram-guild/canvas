package grondag.canvas.varia;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.canvas.Configurator;
import grondag.canvas.apiimpl.util.SimpleImage;
import grondag.canvas.apiimpl.util.SimpleTexture;
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
        this.texture = new SimpleTexture(new SimpleImage(1, GL11.GL_LUMINANCE, LightmapHD.TEX_SIZE, LightmapHD.TEX_SIZE, false), GL21.GL_LUMINANCE8);
        this.client.getTextureManager().registerTexture(textureIdentifier, this.texture);
        this.image = this.texture.getImage();
    }

    public void forceReload() {
        LightmapHD.forceReload();
    }

    private static final ConcurrentLinkedQueue<LightmapHD> updates = new ConcurrentLinkedQueue<>();
    
    public synchronized void setDirty(LightmapHD lightmap) {
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

        GlStateManager.activeTexture(GLX.GL_TEXTURE2);
        this.client.getTextureManager().bindTexture(this.textureIdentifier);
        
        //UGLY: clean up and use ReliableImage methods instead
        
        final int mode = Configurator.enableLightmapDebug ? GL11.GL_NEAREST : GL11.GL_LINEAR;
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D,  GL11.GL_TEXTURE_MAG_FILTER, mode);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    int tickCounter = 0;
    public void onRenderTick() {
        if(tickCounter++ < 10 || !Configurator.enableHdLightmaps || updates.isEmpty()) {
            return;
        }
        
        tickCounter = 0;
        
        final SimpleImage image = this.image;
        int uMin = Integer.MAX_VALUE;
        int vMin = Integer.MAX_VALUE;
        int uMax = Integer.MIN_VALUE;
        int vMax = Integer.MIN_VALUE;

        LightmapHD map = updates.poll();
        while(map != null) {
            final int uMap = map.uMinImg;
            final int vMap = map.vMinImg;
            
            for(int u = 0; u < LightmapHD.PADDED_SIZE; u++) {
                for(int v = 0; v < LightmapHD.PADDED_SIZE; v++) {
                    image.setLuminance(uMap + u, vMap + v, (byte)map.pixel(u,v));
                }
            }
            
            uMin = Math.min(uMin, uMap);
            vMin = Math.min(vMin, vMap);
            uMax = Math.max(uMax, uMap + LightmapHD.PADDED_SIZE);
            vMax = Math.max(vMax, vMap + LightmapHD.PADDED_SIZE);
            
            map = updates.poll();
        }
        
        
        if(uMin == Integer.MAX_VALUE) {
            return;
        }
        
        this.texture.uploadPartial(uMin, vMin, uMax - uMin, vMax - vMin);
    }
}