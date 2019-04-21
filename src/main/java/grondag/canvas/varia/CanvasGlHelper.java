/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.varia;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.Canvas;
import grondag.canvas.Configurator;
import net.minecraft.client.resource.language.I18n;

public class CanvasGlHelper {
    static private final MethodHandles.Lookup lookup = MethodHandles.lookup();

    static boolean useVboArb;
    static private boolean vaoEnabled = false;
    static private boolean useVaoArb = false;

    public static void init() {
        initFastNioCopy();

        GLCapabilities caps = GL.getCapabilities();
        useVboArb = !caps.OpenGL15 && caps.GL_ARB_vertex_buffer_object;
        vaoEnabled = caps.GL_ARB_vertex_array_object || caps.OpenGL30;
        useVaoArb = !caps.OpenGL30 && caps.GL_ARB_vertex_array_object;
    }

    static private int attributeEnabledCount = 0;
    static private int vaoEnabledCount = 0;
    
    /**
     * Disables all generic vertex attributes and resets tracking state. Use after
     * calling {@link #enableAttributesVao(int)}
     */
    public static void disableAttributesVao() {
        for (int i = 1; i <= vaoEnabledCount; i++) {
            GL20.glDisableVertexAttribArray(i);
        }
        vaoEnabledCount = 0;
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
        vaoEnabledCount = enabledCount;
    }

    /**
     * Enables the given number of generic vertex attributes if not already enabled.
     * Using 1-based numbering for attribute slots because GL (on my machine at
     * least) not liking slot 0.
     */
    public static void enableAttributes(int enabledCount) {
        if (enabledCount > attributeEnabledCount) {
            while (enabledCount > attributeEnabledCount)
                GL20.glEnableVertexAttribArray(++attributeEnabledCount);
        } else if (enabledCount < attributeEnabledCount) {
            while (enabledCount < attributeEnabledCount)
                GL20.glDisableVertexAttribArray(attributeEnabledCount--);
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
            Canvas.LOG.error(I18n.translate("misc.warn_slow_gl_call", "fastNioCopy"), e);
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
}
