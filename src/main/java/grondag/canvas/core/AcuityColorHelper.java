package grondag.acuity.core;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AcuityColorHelper
{
    /**
     * Multiplies two colors component-wise and returns result
     */
    public static int multiplyColor(int color1, int color2)
    {
        if(color1 == 0xFFFFFFFF)
            return color2;
        else if(color2 == 0xFFFFFFFF)
            return color1;
        else
        {
            int red = Math.round((color1 & 0xFF) * (color2 & 0xFF) / 255f);
            int green = Math.round(((color1 >> 8) & 0xFF) * ((color2 >> 8) & 0xFF) / 255f);
            int blue = Math.round(((color1 >> 16) & 0xFF) * ((color2 >> 16) & 0xFF) / 255f);
            int alpha = Math.round(((color1 >> 24) & 0xFF) * ((color2 >> 24) & 0xFF) / 255f);
            return (alpha << 24) | (blue << 16) | (green << 8) | red;
        }
    }

    
    /**
     * Multiplies RGB by given factor and swaps the R and B components, leaving alpha intact.<br>
     * In game code/data, colors are generally in ARGB order (left to right = high to low).<br>
     * OpenGL wants them in ABGR order (left to right = high to low).<br>
     */
    public static int shadeColorAndSwapRedBlue(int colorARGB, float shade)
    {
        int blue = colorARGB & 0xFF;
        int green = (colorARGB >> 8) & 0xFF;
        int red = (colorARGB >> 16) & 0xFF;
        
        if(shade !=  1.0f)
        {
            blue = Math.round(blue * shade);
            green = Math.round(green * shade);
            red = Math.round(red * shade);
        }
    
        return (colorARGB & 0xFF000000) | (blue << 16) | (green << 8) | red;
    }
    
    /**
     * 
     */
    public static int swapRedBlue(int color)
    {
        return (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
    }

}
