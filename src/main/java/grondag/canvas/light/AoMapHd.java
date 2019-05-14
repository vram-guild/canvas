package grondag.canvas.light;

import static grondag.canvas.light.LightmapHd.AO_SIZE;
import static grondag.canvas.light.LightmapHd.PADDED_SIZE;
import static grondag.canvas.light.LightmapHd.lightIndex;

import net.minecraft.util.math.MathHelper;

final class AoMapHd {
    
    static void computeAo(int[] light, long key, int index) {
        
      final float topLeft = LightKey.topLeftAo(key) / 255f;
      final float topRight = LightKey.topRightAo(key) / 255f;
      final float bottomRight = LightKey.bottomRightAo(key) / 255f;
      final float bottomLeft = LightKey.bottomLeftAo(key) / 255f;

      
      for(int u = 0; u < PADDED_SIZE; u++) {
          for(int v = 0; v < PADDED_SIZE; v++) {
              float uDist = (float)u / AO_SIZE;
              float vDist = (float)v / AO_SIZE;
              
              float tl = (1 - uDist) * (1 - vDist) * topLeft;
              float tr = uDist * (1 - vDist) * topRight;
              float br = uDist * vDist * bottomRight;
              float bl = (1 - uDist) * vDist * bottomLeft;
              light[lightIndex(u, v)] = output(tl + tr + br + bl);
          }
      }
    }
    
    static int output(float in) {
        return MathHelper.clamp(Math.round(in * 255), 0, 255);
    }
}
