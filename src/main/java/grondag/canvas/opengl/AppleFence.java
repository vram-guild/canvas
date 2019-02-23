package grondag.acuity.opengl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.lwjgl.BufferChecks;
import org.lwjgl.opengl.APPLEFence;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import grondag.acuity.Acuity;
import net.minecraft.util.text.translation.I18n;

class AppleFence extends Fence
{
    static private long testFenceFunctionPointer = -1;
    static private MethodHandle testFenceHandle = null;
    
    static private long setFenceFunctionPointer = -1;
    static private MethodHandle setFenceHandle = null;
    
//    static private long genFenceFunctionPointer = -1;
//    static private MethodHandle genFenceHandle = null;
//    
//    static private long deleteFenceFunctionPointer = -1;
//    static private MethodHandle deleteFenceHandle = null;
    
    
    public static boolean initialize()
    {
        
        ContextCapabilities caps = GLContext.getCapabilities();
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try
        {
            Field pointer = ContextCapabilities.class.getDeclaredField("glSetFenceAPPLE");
            pointer.setAccessible(true);
            setFenceFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(setFenceFunctionPointer);
            Method m = APPLEFence.class.getDeclaredMethod("nglSetFenceAPPLE", int.class, long.class);
            m.setAccessible(true);
            setFenceHandle = lookup.unreflect(m);
            
            pointer = ContextCapabilities.class.getDeclaredField("glTestFenceAPPLE");
            pointer.setAccessible(true);
            testFenceFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(testFenceFunctionPointer);
            m = APPLEFence.class.getDeclaredMethod("nglTestFenceAPPLE", int.class, long.class);
            m.setAccessible(true);
            testFenceHandle = lookup.unreflect(m);
            
//            pointer = ContextCapabilities.class.getDeclaredField("glGenFencesAPPLE");
//            pointer.setAccessible(true);
//            genFenceFunctionPointer = pointer.getLong(caps);
//            BufferChecks.checkFunctionAddress(genFenceFunctionPointer);
//            m = APPLEFence.class.getDeclaredMethod("nglGenFencesAPPLE", int.class, long.class, long.class);
//            m.setAccessible(true);
//            genFenceHandle = lookup.unreflect(m);
//            
//            pointer = ContextCapabilities.class.getDeclaredField("glDeleteFencesAPPLE");
//            pointer.setAccessible(true);
//            deleteFenceFunctionPointer = pointer.getLong(caps);
//            BufferChecks.checkFunctionAddress(deleteFenceFunctionPointer);
//            m = APPLEFence.class.getDeclaredMethod("nglDeleteFencesAPPLE", int.class, long.class, long.class);
//            m.setAccessible(true);
//            deleteFenceHandle = lookup.unreflect(m);
            
            return true;
            
        }
        catch(Exception e)
          {
              return false;
          }
    }
    
    int fencePointer;
    
    AppleFence()
    {
        fencePointer = APPLEFence.glGenFencesAPPLE();
    }
    
    @Override
    public final boolean isReached()
    {
        if(testFenceFunctionPointer == -1)
            return APPLEFence.glTestFenceAPPLE(fencePointer);
        
        try
        {
            return (boolean) testFenceHandle.invokeExact(fencePointer, testFenceFunctionPointer);
        }
        catch (Throwable e)
        {
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glTestFenceAPPLE"), e);
            testFenceFunctionPointer = -1;
            return APPLEFence.glTestFenceAPPLE(fencePointer);
        }
        
        
    }

    @Override
    public final void set()
    {
        if(setFenceFunctionPointer == -1)
        {
            APPLEFence.glSetFenceAPPLE(fencePointer);
            return;
        }
        
        try
        {
            setFenceHandle.invokeExact(fencePointer, setFenceFunctionPointer);
            return;
        }
        catch (Throwable e)
        {
            Acuity.INSTANCE.getLog().error(I18n.translateToLocalFormatted("misc.warn_slow_gl_call", "glSetFenceAPPLE"), e);
            setFenceFunctionPointer = -1;
            APPLEFence.glSetFenceAPPLE(fencePointer);
        }
        
    }

    @Override
    protected void finalize() throws Throwable
    {
       APPLEFence.glDeleteFencesAPPLE(fencePointer);
       fencePointer = -1;
    }

    static Fence createAppleFence()
    {
        return new AppleFence();
    }
}