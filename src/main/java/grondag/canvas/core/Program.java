package grondag.canvas.core;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.Canvas;
import grondag.canvas.opengl.CanvasGlHelper;
import grondag.frex.api.Uniform;
import grondag.frex.api.Uniform.Uniform1f;
import grondag.frex.api.Uniform.Uniform1i;
import grondag.frex.api.Uniform.Uniform2f;
import grondag.frex.api.Uniform.Uniform2i;
import grondag.frex.api.Uniform.Uniform3f;
import grondag.frex.api.Uniform.Uniform3i;
import grondag.frex.api.Uniform.Uniform4f;
import grondag.frex.api.Uniform.Uniform4i;
import grondag.frex.api.Uniform.UniformMatrix4f;
import grondag.frex.api.UniformRefreshFrequency;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.resource.language.I18n;

public class Program {
    private static Program activeProgram;
    
    public static void deactivate() {
        activeProgram = null;
        GLX.glUseProgram(0);
    }

    private int progID = -1;
    private boolean isErrored = false;

    public final PipelineVertexShader vertexShader;
    public final PipelineFragmentShader fragmentShader;
    public final int spriteDepth;
    public final boolean isSolidLayer;
    public final PipelineVertexFormat pipelineVertexFormat;

    private final ObjectArrayList<UniformImpl<?>> uniforms = new ObjectArrayList<>();
    private final ObjectArrayList<UniformImpl<?>> renderTickUpdates = new ObjectArrayList<>();
    private final ObjectArrayList<UniformImpl<?>> gameTickUpdates = new ObjectArrayList<>();

    protected int dirtyCount = 0;
    protected final UniformImpl<?>[] dirtyUniforms = new UniformImpl[32];

    /**
     * Tracks last matrix version to avoid unnecessary uploads.
     */
    private int lastViewMatrixVersion = 0;

    public abstract class UniformImpl<T extends Uniform> {
        protected static final int FLAG_NEEDS_UPLOAD = 1;
        protected static final int FLAG_NEEDS_INITIALIZATION = 2;

        private final String name;
        protected int flags = 0;
        protected int unifID = -1;
        protected final Consumer<T> initializer;
        protected final UniformRefreshFrequency frequency;

        protected UniformImpl(String name, Consumer<T> initializer, UniformRefreshFrequency frequency) {
            this.name = name;
            this.initializer = initializer;
            this.frequency = frequency;
        }

        public final void setDirty() {
            final int flags = this.flags;
            if (flags == 0)
                dirtyUniforms[dirtyCount++] = this;

            this.flags = flags | FLAG_NEEDS_UPLOAD;
        }

        protected final void markForInitialization() {
            final int flags = this.flags;

            if (flags == 0)
                dirtyUniforms[dirtyCount++] = this;

            this.flags = flags | FLAG_NEEDS_INITIALIZATION;
        }

        private final void load(int programID) {
            this.unifID = GLX.glGetUniformLocation(programID, name);
            if (this.unifID == -1) {
                Canvas.INSTANCE.getLog().debug(I18n.translate("misc.debug_missing_uniform", name,
                        Program.this.vertexShader.shaderSource.toString(), Program.this.fragmentShader.shaderSource.toString()));
                this.flags = 0;
            } else {
                // never add view uniforms to dirty list - have special handling
                if (this == modelViewUniform || this == modelViewProjectionUniform)
                    this.flags = 0;
                else {
                    // dirty count will be reset to 0 before uniforms are loaded
                    dirtyUniforms[dirtyCount++] = this;
                    this.flags = FLAG_NEEDS_INITIALIZATION | FLAG_NEEDS_UPLOAD;
                }
            }
        }

        @SuppressWarnings("unchecked")
        protected final void upload() {
            if (this.flags == 0)
                return;

            if ((this.flags & FLAG_NEEDS_INITIALIZATION) == FLAG_NEEDS_INITIALIZATION)
                this.initializer.accept((T) this);

            if ((this.flags & FLAG_NEEDS_UPLOAD) == FLAG_NEEDS_UPLOAD)
                this.uploadInner();

            this.flags = 0;
        }

        protected abstract void uploadInner();
    }

    protected abstract class UniformFloat<T extends Uniform> extends UniformImpl<T> {
        protected final FloatBuffer uniformFloatBuffer;

        protected UniformFloat(String name, Consumer<T> initializer, UniformRefreshFrequency frequency, int size) {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = BufferUtils.createFloatBuffer(size);
        }
    }

    public class Uniform1fImpl extends UniformFloat<Uniform1f> implements Uniform1f {
        protected Uniform1fImpl(String name, Consumer<Uniform1f> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 1);
        }

        @Override
        public final void set(float value) {
            if (this.unifID == -1)
                return;
            if (this.uniformFloatBuffer.get(0) != value) {
                this.uniformFloatBuffer.put(0, value);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform1(this.unifID, this.uniformFloatBuffer);
        }
    }

    public class Uniform2fImpl extends UniformFloat<Uniform2f> implements Uniform2f {
        protected Uniform2fImpl(String name, Consumer<Uniform2f> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 2);
        }

        @Override
        public final void set(float v0, float v1) {
            if (this.unifID == -1)
                return;
            if (this.uniformFloatBuffer.get(0) != v0) {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if (this.uniformFloatBuffer.get(1) != v1) {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform2(this.unifID, this.uniformFloatBuffer);
        }
    }

    public class Uniform3fImpl extends UniformFloat<Uniform3f> implements Uniform3f {
        protected Uniform3fImpl(String name, Consumer<Uniform3f> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 3);
        }

        @Override
        public final void set(float v0, float v1, float v2) {
            if (this.unifID == -1)
                return;
            if (this.uniformFloatBuffer.get(0) != v0) {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if (this.uniformFloatBuffer.get(1) != v1) {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
            if (this.uniformFloatBuffer.get(2) != v2) {
                this.uniformFloatBuffer.put(2, v2);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform3(this.unifID, this.uniformFloatBuffer);
        }
    }

    public class Uniform4fImpl extends UniformFloat<Uniform4f> implements Uniform4f {
        protected Uniform4fImpl(String name, Consumer<Uniform4f> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 4);
        }

        @Override
        public final void set(float v0, float v1, float v2, float v3) {
            if (this.unifID == -1)
                return;
            if (this.uniformFloatBuffer.get(0) != v0) {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if (this.uniformFloatBuffer.get(1) != v1) {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
            if (this.uniformFloatBuffer.get(2) != v2) {
                this.uniformFloatBuffer.put(2, v2);
                this.setDirty();
            }
            if (this.uniformFloatBuffer.get(3) != v3) {
                this.uniformFloatBuffer.put(3, v3);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform4(this.unifID, this.uniformFloatBuffer);
        }
    }

    private <T extends UniformImpl<?>> T addUniform(T toAdd) {
        this.uniforms.add(toAdd);
        if (toAdd.frequency == UniformRefreshFrequency.PER_FRAME)
            this.renderTickUpdates.add(toAdd);
        else if (toAdd.frequency == UniformRefreshFrequency.PER_TICK)
            this.gameTickUpdates.add(toAdd);
        return toAdd;
    }

    public Uniform1f uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
        return addUniform(new Uniform1fImpl(name, initializer, frequency));
    }

    public Uniform2f uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
        return addUniform(new Uniform2fImpl(name, initializer, frequency));
    }

    public Uniform3f uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
        return addUniform(new Uniform3fImpl(name, initializer, frequency));
    }

    public Uniform4f uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
        return addUniform(new Uniform4fImpl(name, initializer, frequency));
    }

    protected abstract class UniformInt<T extends Uniform> extends UniformImpl<T> {
        protected final IntBuffer uniformIntBuffer;

        protected UniformInt(String name, Consumer<T> initializer, UniformRefreshFrequency frequency, int size) {
            super(name, initializer, frequency);
            this.uniformIntBuffer = BufferUtils.createIntBuffer(size);
        }
    }

    public class Uniform1iImpl extends UniformInt<Uniform1i> implements Uniform1i {
        protected Uniform1iImpl(String name, Consumer<Uniform1i> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 1);
        }

        @Override
        public final void set(int value) {
            if (this.unifID == -1)
                return;
            if (this.uniformIntBuffer.get(0) != value) {
                this.uniformIntBuffer.put(0, value);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform1(this.unifID, this.uniformIntBuffer);
        }
    }

    public class Uniform2iImpl extends UniformInt<Uniform2i> implements Uniform2i {
        protected Uniform2iImpl(String name, Consumer<Uniform2i> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 2);
        }

        @Override
        public final void set(int v0, int v1) {
            if (this.unifID == -1)
                return;
            if (this.uniformIntBuffer.get(0) != v0) {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if (this.uniformIntBuffer.get(1) != v1) {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform2(this.unifID, this.uniformIntBuffer);
        }
    }

    public class Uniform3iImpl extends UniformInt<Uniform3i> implements Uniform3i {
        protected Uniform3iImpl(String name, Consumer<Uniform3i> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 3);
        }

        @Override
        public final void set(int v0, int v1, int v2) {
            if (this.unifID == -1)
                return;
            if (this.uniformIntBuffer.get(0) != v0) {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if (this.uniformIntBuffer.get(1) != v1) {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
            if (this.uniformIntBuffer.get(2) != v2) {
                this.uniformIntBuffer.put(2, v2);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform3(this.unifID, this.uniformIntBuffer);
        }
    }

    public class Uniform4iImpl extends UniformInt<Uniform4i> implements Uniform4i {
        protected Uniform4iImpl(String name, Consumer<Uniform4i> initializer, UniformRefreshFrequency frequency) {
            super(name, initializer, frequency, 4);
        }

        @Override
        public final void set(int v0, int v1, int v2, int v3) {
            if (this.unifID == -1)
                return;
            if (this.uniformIntBuffer.get(0) != v0) {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if (this.uniformIntBuffer.get(1) != v1) {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
            if (this.uniformIntBuffer.get(2) != v2) {
                this.uniformIntBuffer.put(2, v2);
                this.setDirty();
            }
            if (this.uniformIntBuffer.get(3) != v3) {
                this.uniformIntBuffer.put(3, v3);
                this.setDirty();
            }
        }

        @Override
        protected void uploadInner() {
            GLX.glUniform4(this.unifID, this.uniformIntBuffer);
        }
    }

    public Uniform1i uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
        return addUniform(new Uniform1iImpl(name, initializer, frequency));
    }

    public Uniform2i uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
        return addUniform(new Uniform2iImpl(name, initializer, frequency));
    }

    public Uniform3i uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
        return addUniform(new Uniform3iImpl(name, initializer, frequency));
    }

    public Uniform4i uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
        return addUniform(new Uniform4iImpl(name, initializer, frequency));
    }

    public Program(PipelineVertexShader vertexShader, PipelineFragmentShader fragmentShader, int spriteDepth,
            boolean isSolidLayer) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.spriteDepth = spriteDepth;
        this.pipelineVertexFormat = PipelineManager.FORMATS[spriteDepth - 1];
        this.isSolidLayer = isSolidLayer;
    }

    /**
     * Handle these directly because may update each activation.
     */
    private final void updateModelUniforms() {
        if (lastViewMatrixVersion != PipelineManager.viewMatrixVersionCounter) {
            updateModelUniformsInner();
            lastViewMatrixVersion = PipelineManager.viewMatrixVersionCounter;
        }
    }

    private final void updateModelUniformsInner() {
        if (this.modelViewUniform != null) {
            this.modelViewUniform.uploadInner();
        }
        
        if (this.modelViewProjectionUniform != null) {
            this.modelViewProjectionUniform.uploadInner();
        }
    }

    public final void activate() {
        if (this.isErrored)
            return;

        if (activeProgram != this) {
            activeProgram = this;
            GLX.glUseProgram(this.progID);

            final int count = this.dirtyCount;
            if (count != 0) {
                for (int i = 0; i < count; i++) {
                    this.dirtyUniforms[i].upload();
                }
                this.dirtyCount = 0;
            }
        }

        this.updateModelUniforms();
    }

    public class UniformMatrix4fImpl extends UniformImpl<UniformMatrix4f> implements UniformMatrix4f {
        protected final FloatBuffer uniformFloatBuffer;
        protected final long bufferAddress;
        protected final Matrix4f lastValue = new Matrix4f();

        protected UniformMatrix4fImpl(String name, Consumer<UniformMatrix4f> initializer,
                UniformRefreshFrequency frequency) {
            this(name, initializer, frequency, BufferUtils.createFloatBuffer(16));
        }

        /**
         * Use when have a shared direct buffer
         */
        protected UniformMatrix4fImpl(String name, Consumer<UniformMatrix4f> initializer,
                UniformRefreshFrequency frequency, FloatBuffer uniformFloatBuffer) {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = uniformFloatBuffer;
            this.bufferAddress = MemoryUtil.memAddress(this.uniformFloatBuffer);
        }

        @Override
        public final void set(Matrix4f matrix) {
            if (this.unifID == -1)
                return;
            
            if(matrix == null || matrix.equals(lastValue))
                return;
            
            lastValue.set(matrix);
            
            matrix.get(this.uniformFloatBuffer);
            
            this.setDirty();
        }

        @Override
        protected void uploadInner() {
            GLX.glUniformMatrix4(this.unifID, false, this.uniformFloatBuffer);
        }
    }

    public UniformMatrix4fImpl uniformMatrix4f(String name, UniformRefreshFrequency frequency,
            Consumer<UniformMatrix4f> initializer) {
        return addUniform(new UniformMatrix4fImpl(name, initializer, frequency));
    }

    public UniformMatrix4fImpl uniformMatrix4f(String name, UniformRefreshFrequency frequency, FloatBuffer floatBuffer,
            Consumer<UniformMatrix4f> initializer) {
        return addUniform(new UniformMatrix4fImpl(name, initializer, frequency, floatBuffer));
    }

    final void load() {
        this.isErrored = true;

        // prevent accumulation of uniforms in programs that aren't activated after
        // multiple reloads
        this.dirtyCount = 0;
        try {
            if (this.progID > 0)
                GLX.glDeleteProgram(progID);

            this.progID = GLX.glCreateProgram();

            this.isErrored = this.progID > 0 && !loadInner();
        } catch (Exception e) {
            if (this.progID > 0)
                GLX.glDeleteProgram(progID);

            Canvas.INSTANCE.getLog().error(I18n.translate("misc.error_program_link_failure"), e);
            this.progID = -1;
        }

        if (!this.isErrored) {
            final int limit = uniforms.size();
            for (int i = 0; i < limit; i++)
                uniforms.get(i).load(progID);
        }

    }

    /**
     * Return true on success
     */
    private final boolean loadInner() {
        final int programID = this.progID;
        if (programID <= 0)
            return false;

        final int vertId = vertexShader.glId();
        if (vertId <= 0)
            return false;

        final int fragId = fragmentShader.glId();
        if (fragId <= 0)
            return false;

        GLX.glAttachShader(programID, vertId);
        GLX.glAttachShader(programID, fragId);

        pipelineVertexFormat.bindProgramAttributes(programID);

        GLX.glLinkProgram(programID);
        if (GLX.glGetProgrami(programID, GLX.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Canvas.INSTANCE.getLog().error(CanvasGlHelper.getProgramInfoLog(programID));
            return false;
        }

        return true;
    }

    public final void onRenderTick() {
        final int limit = renderTickUpdates.size();
        for (int i = 0; i < limit; i++) {
            renderTickUpdates.get(i).markForInitialization();
        }
    }

    public final void onGameTick() {
        final int limit = gameTickUpdates.size();
        for (int i = 0; i < limit; i++) {
            gameTickUpdates.get(i).markForInitialization();
        }
    }

    public UniformMatrix4fImpl modelViewUniform;
    public UniformMatrix4fImpl modelViewProjectionUniform;
    public UniformMatrix4fImpl projectionMatrixUniform;

    public final void setupModelViewUniforms() {
        if (containsUniformSpec("mat4", "u_modelView")) {
            this.modelViewUniform = this.uniformMatrix4f("u_modelView", UniformRefreshFrequency.ON_LOAD,
                    PipelineManager.modelViewMatrixBuffer, u -> {
                        this.modelViewUniform.setDirty();
                    });
        }

        if (containsUniformSpec("mat4", "u_modelViewProjection")) {
            this.modelViewProjectionUniform = this.uniformMatrix4f("u_modelViewProjection",
                    UniformRefreshFrequency.ON_LOAD, PipelineManager.modelViewProjectionMatrixBuffer, u -> {
                        this.modelViewProjectionUniform.setDirty();
                    });
        }

        if (containsUniformSpec("mat4", "u_projection")) {
            // on load because handled directly
            this.projectionMatrixUniform = this.uniformMatrix4f("u_projection", UniformRefreshFrequency.ON_LOAD,
                    PipelineManager.projectionMatrixBuffer, u -> {
                        this.projectionMatrixUniform.setDirty();
                    });
        }

    }

    public boolean containsUniformSpec(String type, String name) {
        String regex = "(?m)^uniform\\s+" + type + "\\s+" + name + "\\s*;";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(this.vertexShader.getSource()).find()
                || pattern.matcher(this.fragmentShader.getSource()).find();
    }
}