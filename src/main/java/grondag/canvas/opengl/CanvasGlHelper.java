package grondag.canvas.opengl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

import com.mojang.blaze3d.platform.GLX;

import grondag.boson.org.joml.Matrix4f;
import grondag.canvas.Canvas;
import grondag.canvas.Configurator;
import net.minecraft.client.resource.language.I18n;

public class CanvasGlHelper {
    static private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    static boolean useVboArb;
    static private boolean asynchBufferMapEnabled;
    static private boolean vaoEnabled = false;
    static private boolean useVaoArb = false;

    public static void init() {
        initFastNioCopy();

        GLCapabilities caps = GL.getCapabilities();
        useVboArb = !caps.OpenGL15 && caps.GL_ARB_vertex_buffer_object;
        vaoEnabled = caps.GL_ARB_vertex_array_object || caps.OpenGL30;
        useVaoArb = !caps.OpenGL30 && caps.GL_ARB_vertex_array_object;
        asynchBufferMapEnabled = caps.glFlushMappedBufferRange != 0 && caps.glMapBufferRange != 0;
    }

    static private int attributeEnabledCount = 0;

    /**
     * Disables all generic vertex attributes and resets tracking state. Use after
     * calling {@link #enableAttributesVao(int)}
     */
    public static void resetAttributes() {
        for (int i = 0; i < 6; i++) {
            GL20.glDisableVertexAttribArray(i);
        }
        attributeEnabledCount = 0;
    }

    /**
     * Like {@link CanvasGlHelper#enableAttributes(int)} but enables all attributes
     * regardless of prior state. Tracking state for
     * {@link CanvasGlHelper#enableAttributes(int)} remains unchanged. Used to
     * initialize VAO state
     */
    public static void enableAttributesVao(int enabledCount) {
        for (int i = 1; i <= enabledCount; i++) {
            GL20.glEnableVertexAttribArray(i);
        }
    }

    /**
     * Enables the given number of generic vertex attributes if not already enabled.
     * Using 1-based numbering for attribute slots because GL (on my machine at
     * least) not liking slot 0.
     */
    public static void enableAttributes(int enabledCount) {
        if (enabledCount > attributeEnabledCount) {
            while (enabledCount > attributeEnabledCount)
                GL20.glEnableVertexAttribArray(1 + attributeEnabledCount++);
        } else if (enabledCount < attributeEnabledCount) {
            while (enabledCount < attributeEnabledCount)
                GL20.glDisableVertexAttribArray(--attributeEnabledCount + 1);
        }
    }

    public static String getProgramInfoLog(int obj) {
        return GLX.glGetProgramInfoLog(obj, GLX.glGetProgrami(obj, GL20.GL_INFO_LOG_LENGTH));
    }

    public static String getShaderInfoLog(int obj) {
        return GLX.glGetProgramInfoLog(obj, GLX.glGetShaderi(obj, GL20.GL_INFO_LOG_LENGTH));
    }

    static private MethodHandle nioCopyFromArray = null;
    static private MethodHandle nioCopyFromIntArray = null;
    static private boolean fastNioCopy = true;
    static private long nioFloatArrayBaseOffset;
    static private boolean nioFloatNeedsFlip;
    static private MethodHandle fastMatrixBufferCopyHandler;

    private static void initFastNioCopy() {
        try {
            Class<?> clazz = Class.forName("java.nio.Bits");
            Method nioCopyFromArray = clazz.getDeclaredMethod("copyFromArray", Object.class, long.class, long.class,
                    long.class, long.class);
            nioCopyFromArray.setAccessible(true);
            CanvasGlHelper.nioCopyFromArray = lookup.unreflect(nioCopyFromArray);

            Method nioCopyFromIntArray = clazz.getDeclaredMethod("copyFromIntArray", Object.class, long.class,
                    long.class, long.class);
            nioCopyFromIntArray.setAccessible(true);
            CanvasGlHelper.nioCopyFromIntArray = lookup.unreflect(nioCopyFromIntArray);

            clazz = Class.forName("java.nio.DirectFloatBufferU");
            Field f = clazz.getDeclaredField("arrayBaseOffset");
            f.setAccessible(true);
            nioFloatArrayBaseOffset = f.getLong(null);

            FloatBuffer testBuffer = BufferUtils.createFloatBuffer(16);
            nioFloatNeedsFlip = testBuffer.order() != ByteOrder.nativeOrder();

            fastNioCopy = true;

            if (fastNioCopy) {
                Method handlerMethod;
                if (nioFloatNeedsFlip)
                    handlerMethod = CanvasGlHelper.class.getDeclaredMethod("fastMatrix4fBufferCopyFlipped",
                            float[].class, long.class);
                else
                    handlerMethod = CanvasGlHelper.class.getDeclaredMethod("fastMatrix4fBufferCopyStraight",
                            float[].class, long.class);

                fastMatrixBufferCopyHandler = lookup.unreflect(handlerMethod);
            }
        } catch (Exception e) {
            fastNioCopy = false;
            Canvas.INSTANCE.getLog().error(I18n.translate("misc.warn_slow_gl_call", "fastNioCopy"), e);
        }
    }

    public static final boolean isFastNioCopyEnabled() {
        return fastNioCopy;
    }

    public static final void fastMatrix4fBufferCopy(float[] elements, long bufferAddress) {
        try {
            fastMatrixBufferCopyHandler.invokeExact(elements, bufferAddress);
        } catch (Throwable e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static final void fastMatrix4fBufferCopyFlipped(float[] elements, long bufferAddress) throws Throwable {
        nioCopyFromIntArray.invokeExact((Object) elements, 0l, bufferAddress, 64l);
    }

    public static final void fastMatrix4fBufferCopyStraight(float[] elements, long bufferAddress) throws Throwable {
        nioCopyFromArray.invokeExact((Object) elements, nioFloatArrayBaseOffset, 0l, bufferAddress, 64l);
    }

    public static boolean isVaoEnabled() {
        return vaoEnabled && Configurator.enable_vao;
    }

    public static boolean areAsynchMappedBuffersSupported() {
        return asynchBufferMapEnabled;
    }

    /**
     * Assumes buffer is bound and starting offset is 0. Maps whole buffer. (Size
     * should be size of buffer.) If writeFlag true, buffer mapped for writing. If
     * false, mapped for reading.
     * 
     * TODO: map partial buffer range
     */
    public static ByteBuffer mapBufferAsynch(ByteBuffer priorMapped, int bufferSize, boolean writeFlag) {
        ByteBuffer result;

        if (!writeFlag)
            // TODO: not right and probably never used
            result = GL30.glMapBufferRange(GLX.GL_ARRAY_BUFFER, 0L, (long) bufferSize,
                    writeFlag ? GL15.GL_WRITE_ONLY : GL15.GL_READ_ONLY, priorMapped);
        else
            result = GL30.glMapBufferRange(GLX.GL_ARRAY_BUFFER, 0L, (long) bufferSize,
                    GL30.GL_MAP_FLUSH_EXPLICIT_BIT | GL30.GL_MAP_UNSYNCHRONIZED_BIT | GL30.GL_MAP_WRITE_BIT,
                    priorMapped);

        if (result != null)
            result.order(ByteOrder.nativeOrder());

        return result;
    }

    public static void flushBuffer(long offset, long length) {
        GL30.glFlushMappedBufferRange(GLX.GL_ARRAY_BUFFER, offset, length);
    }

    public static void unmapBuffer() {
        GL15.glUnmapBuffer(GLX.GL_ARRAY_BUFFER);
    }

    public static void glBufferData(int target, int size, int usage) {
        if (useVboArb)
            ARBVertexBufferObject.glBufferDataARB(target, size, usage);
        else
            GL15.glBufferData(target, size, usage);
    }
    
    public static void glGenVertexArrays(IntBuffer arrays) {
        if(useVaoArb)
            ARBVertexArrayObject.glGenVertexArrays(arrays);
        else
            GL30.glGenVertexArrays(arrays);
    }

    public static void glBindVertexArray(int vaoBufferId) {
        if(useVaoArb)
            ARBVertexArrayObject.glBindVertexArray(vaoBufferId);
        else
            GL30.glBindVertexArray(vaoBufferId);
    }
    
    private static final float[] LOAD_ARRAY = new float[16];
    
    /**
     * NOT THREAD SAFE
     */
    public static void loadTransposeQuickly(FloatBuffer source, Matrix4f dest)
    {
        final float[] load = LOAD_ARRAY;
        source.get(load, 0, 16);
        dest._m00(load[0]);
        dest._m10(load[1]);
        dest._m20(load[2]);
        dest._m30(load[3]);
        dest._m01(load[4]);
        dest._m11(load[5]);
        dest._m21(load[6]);
        dest._m31(load[7]);
        dest._m02(load[8]);
        dest._m12(load[9]);
        dest._m22(load[10]);
        dest._m32(load[11]);
        dest._m03(load[12]);
        dest._m13(load[13]);
        dest._m23(load[14]);
        dest._m33(load[15]);
    }
}
