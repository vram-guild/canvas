package grondag.acuity.opengl;

import java.util.function.Supplier;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

public class OpenGlFenceExt
{
    private static boolean isFenceEnabled = true;
    
    private static Supplier<Fence> supplier;
    
    public static Fence create()
    {
        return supplier.get();
    }
    
    public static void initialize()
    {
        ContextCapabilities caps = GLContext.getCapabilities();
        if(caps.OpenGL32 || caps.GL_ARB_sync)
        {
            isFenceEnabled = ARBFence.initialize();
            supplier = ARBFence::createARBFence;
        }
        else if(caps.GL_APPLE_fence)
        {
            isFenceEnabled = AppleFence.initialize();
            supplier = AppleFence::createAppleFence;
        }
        else
        {
            isFenceEnabled = false;
        }
    }
    
    public static boolean isFenceEnabled()
    {
        return isFenceEnabled;
    }

}
