package grondag.canvas.opengl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.Canvas;
import net.minecraft.client.resource.language.I18n;

public class CanvasGlHelper {
    static private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    
    public static void init() {
        initFastNioCopy();
    }
    
    static private int attributeEnabledCount = 0;
    /**
     * Disables all generic vertex attributes and resets tracking state.
     * Use after calling {@link #enableAttributesVao(int)}
     */
    public static void resetAttributes()
    {
        for(int i = 0; i < 6; i++)
        {
            GL20.glDisableVertexAttribArray(i);
        }
        attributeEnabledCount = 0;
    }
    
    /**
     * Like {@link CanvasGlHelper#enableAttributes(int)} but enables all attributes 
     * regardless of prior state. Tracking state for {@link CanvasGlHelper#enableAttributes(int)} remains unchanged.
     * Used to initialize VAO state
     */
    public static void enableAttributesVao(int enabledCount)
    {
        for(int i = 1; i <= enabledCount; i++)
        {
            GL20.glEnableVertexAttribArray(i);
        }
    }
    /**
     * Enables the given number of generic vertex attributes if not already enabled.
     * Using 1-based numbering for attribute slots because GL (on my machine at least) not liking slot 0.
     */
    public static void enableAttributes(int enabledCount)
    {
        if(enabledCount > attributeEnabledCount)
        {
            while(enabledCount > attributeEnabledCount)
                GL20.glEnableVertexAttribArray(1 + attributeEnabledCount++);
        }
        else if(enabledCount < attributeEnabledCount)
        {
            while(enabledCount < attributeEnabledCount)
                GL20.glDisableVertexAttribArray(--attributeEnabledCount + 1);
        }
    }
    
    public static String getProgramInfoLog(int obj)
    {
        return GLX.glGetProgramInfoLog(obj, GLX.glGetProgrami(obj, GL20.GL_INFO_LOG_LENGTH));
    }
    
    public static String getShaderInfoLog(int obj)
    {
        return GLX.glGetProgramInfoLog(obj, GLX.glGetShaderi(obj, GL20.GL_INFO_LOG_LENGTH));
    }
    
    static private MethodHandle nioCopyFromArray = null;
    static private MethodHandle nioCopyFromIntArray = null;
    static private boolean fastNioCopy = true;
    static private long nioFloatArrayBaseOffset;
    static private boolean nioFloatNeedsFlip;
    static private MethodHandle fastMatrixBufferCopyHandler;
    
    private static void initFastNioCopy() {
        try
        {
            Class<?> clazz = Class.forName("java.nio.Bits");
            Method nioCopyFromArray = clazz.getDeclaredMethod("copyFromArray", Object.class, long.class, long.class, long.class, long.class);
            nioCopyFromArray.setAccessible(true);
            CanvasGlHelper.nioCopyFromArray = lookup.unreflect(nioCopyFromArray);
            
            Method nioCopyFromIntArray = clazz.getDeclaredMethod("copyFromIntArray", Object.class, long.class, long.class, long.class);
            nioCopyFromIntArray.setAccessible(true);
            CanvasGlHelper.nioCopyFromIntArray = lookup.unreflect(nioCopyFromIntArray);
            
            clazz = Class.forName("java.nio.DirectFloatBufferU");
            Field f = clazz.getDeclaredField("arrayBaseOffset");
            f.setAccessible(true);
            nioFloatArrayBaseOffset = f.getLong(null);
            
            FloatBuffer testBuffer = BufferUtils.createFloatBuffer(16);
            nioFloatNeedsFlip = testBuffer.order() != ByteOrder.nativeOrder();
            
            fastNioCopy = true;
            
            if(fastNioCopy)
            {
                Method handlerMethod;
                if(nioFloatNeedsFlip)
                    handlerMethod = OpenGlHelperExt.class.getDeclaredMethod("fastMatrix4fBufferCopyFlipped", float[].class, long.class);
                else
                    handlerMethod = OpenGlHelperExt.class.getDeclaredMethod("fastMatrix4fBufferCopyStraight", float[].class, long.class);
                
                fastMatrixBufferCopyHandler = lookup.unreflect(handlerMethod);
            }
        }
        catch(Exception e)
        {
            fastNioCopy = false;
            Canvas.INSTANCE.getLog().error(I18n.translate("misc.warn_slow_gl_call", "fastNioCopy"), e);
        }
    }
    
    public static final boolean isFastNioCopyEnabled()
    {
        return fastNioCopy;
    }
    
    public static final void fastMatrix4fBufferCopy(float[] elements, long bufferAddress)
    {
        try
        {
            fastMatrixBufferCopyHandler.invokeExact(elements, bufferAddress);
        }
        catch (Throwable e)
        {
            throw new UnsupportedOperationException(e); 
        }
    }
    
    public static final void fastMatrix4fBufferCopyFlipped(float[] elements, long bufferAddress) throws Throwable
    {
        nioCopyFromIntArray.invokeExact((Object)elements, 0l, bufferAddress, 64l);
    }
    
    public static final void fastMatrix4fBufferCopyStraight(float[] elements, long bufferAddress) throws Throwable
    {
        nioCopyFromArray.invokeExact((Object)elements, nioFloatArrayBaseOffset, 0l, bufferAddress, 64l);
    }
}
