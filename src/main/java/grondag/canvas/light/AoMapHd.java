package grondag.canvas.light;

import static grondag.canvas.light.LightmapHd.lightIndex;

import net.minecraft.util.math.MathHelper;

final class AoMapHd {
    
    static void computeAo(int[] light, long key, int index) {
        
      final float topLeft = LightKey.topLeftAo(key) / 255f;
      final float topRight = LightKey.topRightAo(key) / 255f;
      final float bottomRight = LightKey.bottomRightAo(key) / 255f;
      final float bottomLeft = LightKey.bottomLeftAo(key) / 255f;

      
      for(int u = 0; u < LightmapSizer.paddedSize; u++) {
          for(int v = 0; v < LightmapSizer.paddedSize; v++) {
              float uDist = (float)u / LightmapSizer.aoSize;
              float vDist = (float)v / LightmapSizer.aoSize;
              
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
