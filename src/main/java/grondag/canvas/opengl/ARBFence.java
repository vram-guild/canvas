package grondag.acuity.opengl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.IntBuffer;

import org.lwjgl.BufferChecks;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;

import grondag.acuity.Acuity;
import net.minecraft.client.renderer.GLAllocation;

class ARBFence extends Fence
{
    static private long testFenceFunctionPointer = -1;
    static private MethodHandle testFenceHandle = null;
    
    static private long setFenceFunctionPointer = -1;
    static private MethodHandle setFenceHandle = null;
    
    static private long deleteFenceFunctionPointer = -1;
    static private MethodHandle deleteFenceHandle = null;
    
    public static boolean initialize()
    {
        ContextCapabilities caps = GLContext.getCapabilities();
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try
        {
            Field pointer = ContextCapabilities.class.getDeclaredField("glFenceSync");
            pointer.setAccessible(true);
            setFenceFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(setFenceFunctionPointer);
            Method m = GL32.class.getDeclaredMethod("nglFenceSync", int.class, int.class, long.class);
            m.setAccessible(true);
            setFenceHandle = lookup.unreflect(m);
            
            pointer = ContextCapabilities.class.getDeclaredField("glGetSynciv");
            pointer.setAccessible(true);
            testFenceFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(testFenceFunctionPointer);
            m = GL32.class.getDeclaredMethod("nglGetSynciv", long.class, int.class, int.class, long.class, long.class, long.class);
            m.setAccessible(true);
            testFenceHandle = lookup.unreflect(m);
            
            pointer = ContextCapabilities.class.getDeclaredField("glDeleteSync");
            pointer.setAccessible(true);
            deleteFenceFunctionPointer = pointer.getLong(caps);
            BufferChecks.checkFunctionAddress(deleteFenceFunctionPointer);
            m = GL32.class.getDeclaredMethod("nglDeleteSync", long.class, long.class);
            m.setAccessible(true);
            deleteFenceHandle = lookup.unreflect(m);
            
            return true;
            
        }
        catch(Exception e)
          {
              return false;
          }
    }
    
    private long fencePointer = -1;
    
    private final IntBuffer resultBuffer;
    private final IntBuffer lengthBuffer;
    private final long resultBufferAddress;
    private final long lengthBufferAddress;
    
    ARBFence()
    {
        resultBuffer = GLAllocation.createDirectIntBuffer(1);
        lengthBuffer = GLAllocation.createDirectIntBuffer(1);
        resultBufferAddress = MemoryUtil.getAddress(resultBuffer);
        lengthBufferAddress = MemoryUtil.getAddress(lengthBuffer);
    }
    
    static boolean needsErrorNotice = true;
    
    @Override
    public final boolean isReached()
    {
        try
        {
            testFenceHandle.invokeExact(fencePointer, GL32.GL_SYNC_STATUS, 1, lengthBufferAddress, resultBufferAddress, testFenceFunctionPointer);
            return resultBuffer.get(0) == GL32.GL_SIGNALED;
        }
        catch (Throwable e)
        {
            if(needsErrorNotice)
                Acuity.INSTANCE.getLog().error("GL Synch inoperable due to unexpected error. Fences will be ignored and rendering stangeness or crashes may ensue.", e);
            
            return true;
        }
    }

    @Override
    public final void set()
    {
        try
        {
            if(fencePointer != -1)
                deleteFenceHandle.invokeExact(fencePointer, deleteFenceFunctionPointer);
            
            fencePointer = (long) setFenceHandle.invokeExact(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0, setFenceFunctionPointer);
        }
        catch (Throwable e)
        {
            if(needsErrorNotice)
                Acuity.INSTANCE.getLog().error("GL Synch inoperable due to unexpected error. Fences will be ignored and rendering stangeness or crashes may ensue.", e);
        }
    }
    
    @Override
    public void deleteGlResources()
    {
        if(fencePointer != -1) try
        {
            deleteFenceHandle.invokeExact(fencePointer, deleteFenceFunctionPointer);
            fencePointer = -1;
        }
        catch (Throwable e)
        {
        }
    }

    static Fence createARBFence()
    {
        return new ARBFence();
    }
}