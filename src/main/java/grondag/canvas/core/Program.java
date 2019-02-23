package grondag.acuity.core;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Acuity;
import grondag.acuity.Configurator;
import grondag.acuity.api.IUniform;
import grondag.acuity.api.IUniform.IUniform1f;
import grondag.acuity.api.IUniform.IUniform1i;
import grondag.acuity.api.IUniform.IUniform2f;
import grondag.acuity.api.IUniform.IUniform2i;
import grondag.acuity.api.IUniform.IUniform3f;
import grondag.acuity.api.IUniform.IUniform3i;
import grondag.acuity.api.IUniform.IUniform4f;
import grondag.acuity.api.IUniform.IUniform4i;
import grondag.acuity.api.IUniform.IUniformMatrix4f;
import grondag.acuity.opengl.OpenGlHelperExt;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.TextureFormat;
import grondag.acuity.api.UniformUpdateFrequency;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Program
{
    private static @Nullable Program activeProgram;
    
    public static void deactivate()
    {
        activeProgram = null;
        OpenGlHelperExt.glUseProgramFast(0);
    }
    
    private int progID = -1;
    private boolean isErrored = false;

    public final PipelineVertexShader vertexShader;
    public final PipelineFragmentShader fragmentShader;
    public final TextureFormat textureFormat;
    public final boolean isSolidLayer;
 
    private final ObjectArrayList<Uniform<?>> uniforms = new ObjectArrayList<>();
    private final ObjectArrayList<Uniform<?>> renderTickUpdates = new ObjectArrayList<>();
    private final ObjectArrayList<Uniform<?>> gameTickUpdates = new ObjectArrayList<>();
    
    protected int dirtyCount = 0;
    protected final Uniform<?>[] dirtyUniforms = new Uniform[32];
    
    /**
     * Tracks last matrix version to avoid unnecessary uploads.
     */
    private int lastViewMatrixVersion = 0;
    
    public abstract class Uniform<T extends IUniform>
    {
        protected static final int FLAG_NEEDS_UPLOAD = 1;
        protected static final int FLAG_NEEDS_INITIALIZATION = 2;
        
        private final String name;
        protected int flags = 0;
        protected int unifID = -1;
        protected final Consumer<T> initializer;
        protected final UniformUpdateFrequency frequency;
        
        protected Uniform(String name, Consumer<T> initializer, UniformUpdateFrequency frequency)
        {
            this.name = name;
            this.initializer = initializer;
            this.frequency = frequency;
        }
        
        public final void setDirty()
        {
            final int flags = this.flags;
            if(flags == 0)
                dirtyUniforms[dirtyCount++] = this;
            
            this.flags = flags | FLAG_NEEDS_UPLOAD;
        }
        
        protected final void markForInitialization()
        {
            final int flags = this.flags;
            
            if(flags == 0)
                dirtyUniforms[dirtyCount++] = this;
            
            this.flags = flags | FLAG_NEEDS_INITIALIZATION;
        }
        
        private final void load(int programID)
        {
            this.unifID = OpenGlHelper.glGetUniformLocation(programID, name);
            if(this.unifID == -1)
            {
                Acuity.INSTANCE.getLog().debug(I18n.translateToLocalFormatted("misc.debug_missing_uniform", name, Program.this.vertexShader.fileName, Program.this.fragmentShader.fileName));
                this.flags = 0;
            }
            else
            {
                // never add view uniforms to dirty list - have special handling
                if(this == modelViewUniform || this == modelViewProjectionUniform)
                    this.flags = 0;
                else
                {
                    // dirty count will be reset to 0 before uniforms are loaded
                    dirtyUniforms[dirtyCount++] = this;
                    this.flags = FLAG_NEEDS_INITIALIZATION | FLAG_NEEDS_UPLOAD;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        protected final void upload()
        {
            if(this.flags == 0)
                return;
            
            if((this.flags & FLAG_NEEDS_INITIALIZATION) == FLAG_NEEDS_INITIALIZATION)
                this.initializer.accept((T) this);
            
            if((this.flags & FLAG_NEEDS_UPLOAD) == FLAG_NEEDS_UPLOAD)
                this.uploadInner();
            
            this.flags = 0;
        }
        
        protected abstract void uploadInner();
    }
    
    protected abstract class UniformFloat<T extends IUniform> extends Uniform<T>
    {
        protected final FloatBuffer uniformFloatBuffer;
        
        protected UniformFloat(String name, Consumer<T> initializer, UniformUpdateFrequency frequency, int size)
        {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = BufferUtils.createFloatBuffer(size);
        }
    }
    
    public class Uniform1f extends UniformFloat<IUniform1f> implements IUniform1f
    {
        protected Uniform1f(String name, Consumer<IUniform1f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 1);
        }

        @Override
        public final void set(float value)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != value)
            {
                this.uniformFloatBuffer.put(0, value);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform1(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    public class Uniform2f extends UniformFloat<IUniform2f> implements IUniform2f
    {
        protected Uniform2f(String name, Consumer<IUniform2f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 2);
        }

        @Override
        public final void set(float v0, float v1)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != v0)
            {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(1) != v1)
            {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform2(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    public class Uniform3f extends UniformFloat<IUniform3f> implements IUniform3f
    {
        protected Uniform3f(String name, Consumer<IUniform3f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 3);
        }

        @Override
        public final void set(float v0, float v1, float v2)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != v0)
            {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(1) != v1)
            {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(2) != v2)
            {
                this.uniformFloatBuffer.put(2, v2);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform3(this.unifID, this.uniformFloatBuffer);
       }
    }
    
    public class Uniform4f extends UniformFloat<IUniform4f> implements IUniform4f
    {
        protected Uniform4f(String name, Consumer<IUniform4f> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 4);
        }

        @Override
        public final void set(float v0, float v1, float v2, float v3)
        {
            if(this.unifID == -1) return;
            if(this.uniformFloatBuffer.get(0) != v0)
            {
                this.uniformFloatBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(1) != v1)
            {
                this.uniformFloatBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(2) != v2)
            {
                this.uniformFloatBuffer.put(2, v2);
                this.setDirty();
            }
            if(this.uniformFloatBuffer.get(3) != v3)
            {
                this.uniformFloatBuffer.put(3, v3);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform4(this.unifID, this.uniformFloatBuffer);
        }
    }
    
    private <T extends Uniform<?>> T addUniform(T toAdd)
    {
        this.uniforms.add(toAdd);
        if(toAdd.frequency == UniformUpdateFrequency.PER_FRAME)
            this.renderTickUpdates.add(toAdd);
        else if(toAdd.frequency == UniformUpdateFrequency.PER_TICK)
            this.gameTickUpdates.add(toAdd);
        return toAdd;
    }
    
    public IUniform1f uniform1f(String name, UniformUpdateFrequency frequency, Consumer<IUniform1f> initializer)
    {
        return addUniform(new Uniform1f(name, initializer, frequency));
    }
    
    public IUniform2f uniform2f(String name, UniformUpdateFrequency frequency, Consumer<IUniform2f> initializer)
    {
        return addUniform(new Uniform2f(name, initializer, frequency));
    }
    
    public IUniform3f uniform3f(String name, UniformUpdateFrequency frequency, Consumer<IUniform3f> initializer)
    {
        return addUniform(new Uniform3f(name, initializer, frequency));
    }
    
    public IUniform4f uniform4f(String name, UniformUpdateFrequency frequency, Consumer<IUniform4f> initializer)
    {
        return addUniform(new Uniform4f(name, initializer, frequency));
    }
    
    protected abstract class UniformInt<T extends IUniform> extends Uniform<T>
    {
        protected final IntBuffer uniformIntBuffer;
        
        protected UniformInt(String name, Consumer<T> initializer, UniformUpdateFrequency frequency, int size)
        {
            super(name, initializer, frequency);
            this.uniformIntBuffer = BufferUtils.createIntBuffer(size);
        }
    }
    
    public class Uniform1i extends UniformInt<IUniform1i> implements IUniform1i
    {
        protected Uniform1i(String name, Consumer<IUniform1i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 1);
        }

        @Override
        public final void set(int value)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != value)
            {
                this.uniformIntBuffer.put(0, value);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform1(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform2i extends UniformInt<IUniform2i> implements IUniform2i
    {
        protected Uniform2i(String name, Consumer<IUniform2i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 2);
        }

        @Override
        public final void set(int v0, int v1)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != v0)
            {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(1) != v1)
            {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform2(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform3i extends UniformInt<IUniform3i> implements IUniform3i
    {
        protected Uniform3i(String name, Consumer<IUniform3i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 3);
        }

        @Override
        public final void set(int v0, int v1, int v2)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != v0)
            {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(1) != v1)
            {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(2) != v2)
            {
                this.uniformIntBuffer.put(2, v2);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform3(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public class Uniform4i extends UniformInt<IUniform4i> implements IUniform4i
    {
        protected Uniform4i(String name, Consumer<IUniform4i> initializer, UniformUpdateFrequency frequency)
        {
            super(name, initializer, frequency, 4);
        }

        @Override
        public final void set(int v0, int v1, int v2, int v3)
        {
            if(this.unifID == -1) return;
            if(this.uniformIntBuffer.get(0) != v0)
            {
                this.uniformIntBuffer.put(0, v0);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(1) != v1)
            {
                this.uniformIntBuffer.put(1, v1);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(2) != v2)
            {
                this.uniformIntBuffer.put(2, v2);
                this.setDirty();
            }
            if(this.uniformIntBuffer.get(3) != v3)
            {
                this.uniformIntBuffer.put(3, v3);
                this.setDirty();
            }
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelper.glUniform4(this.unifID, this.uniformIntBuffer);
        }
    }
    
    public IUniform1i uniform1i(String name, UniformUpdateFrequency frequency, Consumer<IUniform1i> initializer)
    {
        return addUniform(new Uniform1i(name, initializer, frequency));
    }
    
    public IUniform2i uniform2i(String name, UniformUpdateFrequency frequency, Consumer<IUniform2i> initializer)
    {
        return addUniform(new Uniform2i(name, initializer, frequency));
    }
    
    public IUniform3i uniform3i(String name, UniformUpdateFrequency frequency, Consumer<IUniform3i> initializer)
    {
        return addUniform(new Uniform3i(name, initializer, frequency));
    }
    
    public IUniform4i uniform4i(String name, UniformUpdateFrequency frequency, Consumer<IUniform4i> initializer)
    {
        return addUniform(new Uniform4i(name, initializer, frequency));
    }
    
    public Program(PipelineVertexShader vertexShader, PipelineFragmentShader fragmentShader, TextureFormat textureFormat, boolean isSolidLayer)
    {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.textureFormat = textureFormat;
        this.isSolidLayer = isSolidLayer;
    }
 
    /**
     * Call after render / resource refresh to force shader reload.
     */
    public final void forceReload()
    {
        this.load();
    }
    
    /**
     * Handle these directly because may update each activation.
     */
    private final void updateModelUniforms()
    {
        if(lastViewMatrixVersion != PipelineManager.viewMatrixVersionCounter)
        {
            updateModelUniformsInner();
            lastViewMatrixVersion = PipelineManager.viewMatrixVersionCounter;
        }
    }
    
    @SuppressWarnings("null")
    private final void updateModelUniformsInner()
    {
        if(this.modelViewUniform != null);
            this.modelViewUniform.uploadInner();
    
        if(this.modelViewProjectionUniform != null);
            this.modelViewProjectionUniform.uploadInner();
    }
    
    public final void activate()
    {
        if(this.isErrored)
            return;
        
        if(activeProgram != this)
        {
            activeProgram = this;
            OpenGlHelperExt.glUseProgramFast(this.progID);
    
            final int count = this.dirtyCount;
            if(count != 0)
            {            
                for(int i = 0; i < count; i++)
                {
                    this.dirtyUniforms[i].upload();
                }
                this.dirtyCount = 0;
            }
        }
        
        this.updateModelUniforms();
    }
    
    public class UniformMatrix4f extends Uniform<IUniformMatrix4f> implements IUniformMatrix4f
    {
        protected final FloatBuffer uniformFloatBuffer;
        protected final long bufferAddress;
        
        protected final float[] lastValue = new float[16];
        
        protected UniformMatrix4f(String name, Consumer<IUniformMatrix4f> initializer, UniformUpdateFrequency frequency)
        {
            this(name, initializer, frequency, BufferUtils.createFloatBuffer(16));
        }

        /**
         * Use when have a shared direct buffer
         */
        protected UniformMatrix4f(String name, Consumer<IUniformMatrix4f> initializer, UniformUpdateFrequency frequency, FloatBuffer uniformFloatBuffer)
        {
            super(name, initializer, frequency);
            this.uniformFloatBuffer = uniformFloatBuffer;
            this.bufferAddress = MemoryUtil.getAddress(this.uniformFloatBuffer);
        }
        
        @Override
        public final void set(Matrix4f matrix)
        {
            this.set(matrix.m00, matrix.m01, matrix.m02, matrix.m03, matrix.m10, matrix.m11, matrix.m12, matrix.m13, matrix.m20, matrix.m21, matrix.m22, matrix.m23, matrix.m30, matrix.m31, matrix.m32, matrix.m33);
        }
        
        @Override
        public final void set(float... elements)
        {
            if(this.unifID == -1) return;
            if(elements.length != 16)
                throw new ArrayIndexOutOfBoundsException("Matrix arrays must have 16 elements");
            
            if(lastValue[0] == elements[0]
                 && lastValue[1] == elements[1]
                 && lastValue[2] == elements[2]
                 && lastValue[3] == elements[3]
                 && lastValue[4] == elements[4]
                 && lastValue[5] == elements[5]
                 && lastValue[6] == elements[6]
                 && lastValue[7] == elements[7]
                 && lastValue[8] == elements[8]
                 && lastValue[9] == elements[9]
                 && lastValue[10] == elements[10]
                 && lastValue[11] == elements[11]
                 && lastValue[12] == elements[12]
                 && lastValue[13] == elements[13]
                 && lastValue[14] == elements[14]
                 && lastValue[15] == elements[15]) 
                return;
            
            // avoid NIO overhead
            if(OpenGlHelperExt.isFastNioCopyEnabled())
                OpenGlHelperExt.fastMatrix4fBufferCopy(elements, bufferAddress);
            else
            {
                this.uniformFloatBuffer.put(elements, 0, 16);
                this.uniformFloatBuffer.position(0);
            }
           
            this.setDirty();
        }
        
        @Override
        protected void uploadInner()
        {
            OpenGlHelperExt.glUniformMatrix4Fast(this.unifID, true, this.uniformFloatBuffer, this.bufferAddress);
        }
    }
    
    public UniformMatrix4f uniformMatrix4f(String name, UniformUpdateFrequency frequency, Consumer<IUniformMatrix4f> initializer)
    {
        return addUniform(new UniformMatrix4f(name, initializer, frequency));
    }
    
    public UniformMatrix4f uniformMatrix4f(String name, UniformUpdateFrequency frequency, FloatBuffer floatBuffer, Consumer<IUniformMatrix4f> initializer)
    {
        return addUniform(new UniformMatrix4f(name, initializer, frequency, floatBuffer));
    }
    
    private final void load()
    {
        this.isErrored = true;
        
        // prevent accumulation of uniforms in programs that aren't activated after multiple reloads
        this.dirtyCount = 0;
        try
        {
            if(this.progID > 0)
                OpenGlHelper.glDeleteProgram(progID);
            
            this.progID = OpenGlHelper.glCreateProgram();
            
            this.isErrored = this.progID > 0 && !loadInner();
        }
        catch(Exception e)
        {
            if(this.progID > 0)
                OpenGlHelper.glDeleteProgram(progID);
            
            Acuity.INSTANCE.getLog().error(I18n.translateToLocal("misc.error_program_link_failure"), e);
            this.progID = -1;
        }
        
        if(!this.isErrored)
        {   
            final int limit = uniforms.size();
            for(int i = 0; i < limit; i++)
                uniforms.get(i).load(progID);
        }
        
    }
    
    /**
     * Return true on success
     */
    private final boolean loadInner()
    {
        final int programID = this.progID;
        if(programID <= 0)
            return false;
        
        final int vertId = vertexShader.glId();
        if(vertId <= 0)
            return false;
        
        final int fragId = fragmentShader.glId();
        if(fragId <= 0)
            return false;
        
        OpenGlHelper.glAttachShader(programID, vertId);
        OpenGlHelper.glAttachShader(programID, fragId);
        
        Configurator.lightingModel.vertexFormat(this.textureFormat).bindProgramAttributes(programID);
        
        OpenGlHelper.glLinkProgram(programID);
        if(OpenGlHelper.glGetProgrami(programID, OpenGlHelper.GL_LINK_STATUS) == GL11.GL_FALSE)
        {
            Acuity.INSTANCE.getLog().error(OpenGlHelperExt.getProgramInfoLog(programID));
            return false;
        }

        
        return true;
    }

    public final void onRenderTick()
    {
        final int limit = renderTickUpdates.size();
        for(int i = 0; i < limit; i++)
        {
            renderTickUpdates.get(i).markForInitialization();
        }
    }

    public final void onGameTick()
    {
        final int limit = gameTickUpdates.size();
        for(int i = 0; i < limit; i++)
        {
            gameTickUpdates.get(i).markForInitialization();
        }
    }
    
    @Nullable
    public UniformMatrix4f modelViewUniform;
    @Nullable
    public UniformMatrix4f modelViewProjectionUniform;
    @Nullable
    public UniformMatrix4f projectionMatrixUniform;
    
    
    @SuppressWarnings("null")
    public final void setupModelViewUniforms()
    {
        if(containsUniformSpec("mat4", "u_modelView"))
        {
            this.modelViewUniform = this.uniformMatrix4f("u_modelView", UniformUpdateFrequency.ON_LOAD, 
                    PipelineManager.modelViewMatrixBuffer, u -> 
            {
                this.modelViewUniform.setDirty();
            });
        }

        if(containsUniformSpec("mat4", "u_modelViewProjection"))
        {
            this.modelViewProjectionUniform = this.uniformMatrix4f("u_modelViewProjection", UniformUpdateFrequency.ON_LOAD,
                    PipelineManager.modelViewProjectionMatrixBuffer, u -> 
            {
                this.modelViewProjectionUniform.setDirty();
            });
        }
        
        if(containsUniformSpec("mat4", "u_projection"))
        {
            // on load because handled directly
            this.projectionMatrixUniform = this.uniformMatrix4f("u_projection", UniformUpdateFrequency.ON_LOAD, 
                    PipelineManager.projectionMatrixBuffer, u -> 
            {
                this.projectionMatrixUniform.setDirty();
            });
        }
        
        
    }
    
    public boolean containsUniformSpec(String type, String name)
    {
        String regex = "(?m)^uniform\\s+" + type + "\\s+" + name + "\\s*;";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(this.vertexShader.getSource()).find() 
                || pattern.matcher(this.fragmentShader.getSource()).find(); 
    }
}