/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.shader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTGPUShader4;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.Matrix3f;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.config.Configurator;
import grondag.canvas.mixinterface.Matrix3fExt;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.frex.api.material.Uniform;
import grondag.frex.api.material.Uniform.Uniform1f;
import grondag.frex.api.material.Uniform.Uniform1i;
import grondag.frex.api.material.Uniform.Uniform1ui;
import grondag.frex.api.material.Uniform.Uniform2f;
import grondag.frex.api.material.Uniform.Uniform2i;
import grondag.frex.api.material.Uniform.Uniform2ui;
import grondag.frex.api.material.Uniform.Uniform3f;
import grondag.frex.api.material.Uniform.Uniform3i;
import grondag.frex.api.material.Uniform.Uniform3ui;
import grondag.frex.api.material.Uniform.Uniform4f;
import grondag.frex.api.material.Uniform.Uniform4i;
import grondag.frex.api.material.Uniform.Uniform4ui;
import grondag.frex.api.material.Uniform.UniformArray4f;
import grondag.frex.api.material.Uniform.UniformArrayf;
import grondag.frex.api.material.Uniform.UniformArrayi;
import grondag.frex.api.material.Uniform.UniformArrayui;
import grondag.frex.api.material.Uniform.UniformMatrix3f;
import grondag.frex.api.material.UniformRefreshFrequency;

public class GlProgram {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: GlProgram static init");
		}
	}

	private static GlProgram activeProgram;
	private final Shader vertexShader;
	private final Shader fragmentShader;
	public final CanvasVertexFormat vertexFormat;
	public final ProgramType programType;
	private final ObjectArrayList<UniformImpl<?>> uniforms = new ObjectArrayList<>();
	private final ObjectArrayList<UniformImpl<?>> activeUniforms = new ObjectArrayList<>();
	private final ObjectArrayList<UniformImpl<?>> renderTickUpdates = new ObjectArrayList<>();
	private final ObjectArrayList<UniformImpl<?>> gameTickUpdates = new ObjectArrayList<>();

	protected boolean hasDirty = false;
	private int progID = -1;
	private boolean isErrored = false;
	private boolean needsLoad = true;

	GlProgram(Shader vertexShader, Shader fragmentShader, CanvasVertexFormat format, ProgramType programType) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.programType = programType;
		vertexFormat = format;
		ShaderData.COMMON_UNIFORM_SETUP.accept(this);
	}

	public static void deactivate() {
		if (activeProgram != null) {
			activeProgram = null;
			GL21.glUseProgram(0);
		}
	}

	public static GlProgram activeProgram() {
		return activeProgram;
	}

	public int programId() {
		return progID;
	}

	public Uniform1f uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
		return new Uniform1fImpl(name, initializer, frequency);
	}

	public Uniform2f uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
		return new Uniform2fImpl(name, initializer, frequency);
	}

	public Uniform3f uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
		return new Uniform3fImpl(name, initializer, frequency);
	}

	public Uniform4f uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
		return new Uniform4fImpl(name, initializer, frequency);
	}

	public UniformArrayf uniformArrayf(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayf> initializer, int size) {
		return new UniformArrayfImpl(name, initializer, frequency, size);
	}

	public UniformArray4f uniformArray4f(String name, UniformRefreshFrequency frequency, Consumer<UniformArray4f> initializer, int size) {
		return new UniformArray4fImpl(name, initializer, frequency, size);
	}

	public Uniform1i uniformSampler2d(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		return new UniformSamplerImpl(name, initializer, frequency);
	}

	public Uniform1i uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		return new Uniform1iImpl(name, initializer, frequency);
	}

	public Uniform2i uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
		return new Uniform2iImpl(name, initializer, frequency);
	}

	public Uniform3i uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
		return new Uniform3iImpl(name, initializer, frequency);
	}

	public Uniform4i uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
		return new Uniform4iImpl(name, initializer, frequency);
	}

	public UniformArrayi uniformArrayi(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayi> initializer, int size) {
		return new UniformArrayiImpl(name, initializer, frequency, size);
	}

	public Uniform1ui uniform1ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform1ui> initializer) {
		return new Uniform1uiImpl(name, initializer, frequency);
	}

	public Uniform2ui uniform2ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform2ui> initializer) {
		return new Uniform2uiImpl(name, initializer, frequency);
	}

	public Uniform3ui uniform3ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform3ui> initializer) {
		return new Uniform3uiImpl(name, initializer, frequency);
	}

	public Uniform4ui uniform4ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform4ui> initializer) {
		return new Uniform4uiImpl(name, initializer, frequency);
	}

	public UniformArrayui uniformArrayui(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayui> initializer, int size) {
		return new UniformArrayuiImpl(name, initializer, frequency, size);
	}

	public final void activate() {
		if (needsLoad) {
			load();
			needsLoad = false;
		}

		if (isErrored) {
			return;
		}

		if (activeProgram != this) {
			activeProgram = this;
			activateInner();
		}
	}

	private void activateInner() {
		if (isErrored) {
			return;
		}

		GL21.glUseProgram(progID);

		if (hasDirty) {
			final int count = activeUniforms.size();

			for (int i = 0; i < count; i++) {
				activeUniforms.get(i).upload();
			}

			hasDirty = false;
		}
	}

	public UniformMatrix4fArrayImpl uniformMatrix4fArray(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4fArrayImpl> initializer) {
		return new UniformMatrix4fArrayImpl(name, frequency, initializer);
	}

	public UniformMatrix3fImpl uniformMatrix3f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix3f> initializer) {
		return new UniformMatrix3fImpl(name, initializer, frequency);
	}

	public UniformMatrix3fImpl uniformMatrix3f(String name, UniformRefreshFrequency frequency, FloatBuffer floatBuffer, Consumer<UniformMatrix3f> initializer) {
		return new UniformMatrix3fImpl(name, initializer, frequency);
	}

	private void loadUniforms() {
		activeUniforms.clear();
		renderTickUpdates.clear();
		gameTickUpdates.clear();

		final int limit = uniforms.size();

		for (int i = 0; i < limit; ++i) {
			final UniformImpl<?> u = uniforms.get(i);

			if (containsUniformSpec(u)) {
				activeUniforms.add(u);

				if (u.frequency == UniformRefreshFrequency.PER_FRAME) {
					renderTickUpdates.add(u);
				} else if (u.frequency == UniformRefreshFrequency.PER_TICK) {
					gameTickUpdates.add(u);
				}
			}
		}
	}

	public void load() {
		isErrored = true;

		// prevent accumulation of uniforms in programs that aren't activated after
		// multiple reloads
		hasDirty = false;

		try {
			if (progID > 0) {
				GL21.glDeleteProgram(progID);
			}

			progID = GL21.glCreateProgram();

			isErrored = progID > 0 && !loadInner();
		} catch (final Exception e) {
			if (progID > 0) {
				GL21.glDeleteProgram(progID);
			}

			CanvasMod.LOG.error(I18n.translate("error.canvas.program_link_failure"), e);
			progID = -1;
		}

		if (!isErrored) {
			loadUniforms();
			final int limit = activeUniforms.size();

			for (int i = 0; i < limit; i++) {
				activeUniforms.get(i).load(progID);
			}

			GlProgramManager.INSTANCE.add(this);
		}
	}

	public final void unload() {
		if (progID > 0) {
			GL21.glDeleteProgram(progID);
			progID = -1;
			GlProgramManager.INSTANCE.remove(this);
		}
	}

	/**
	 * Return true on success.
	 */
	private boolean loadInner() {
		final int programID = progID;

		if (programID <= 0) {
			return false;
		}

		if (!vertexShader.attach(programID) || !fragmentShader.attach(programID)) {
			return false;
		}

		vertexFormat.bindProgramAttributes(programID);

		GL21.glLinkProgram(programID);

		if (GL21.glGetProgrami(programID, GL21.GL_LINK_STATUS) == GL11.GL_FALSE) {
			CanvasMod.LOG.error(CanvasGlHelper.getProgramInfoLog(programID));
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

	public boolean containsUniformSpec(UniformImpl<?> uniform) {
		final String type = uniform.searchString();
		final String name = uniform.name;

		return vertexShader.containsUniformSpec(type, name)
				|| fragmentShader.containsUniformSpec(type, name);
	}

	public abstract class UniformImpl<T extends Uniform> {
		protected static final int FLAG_NEEDS_UPLOAD = 1;
		protected static final int FLAG_NEEDS_INITIALIZATION = 2;
		protected final Consumer<T> initializer;
		protected final UniformRefreshFrequency frequency;
		private final String name;
		protected int flags = 0;
		protected int unifID = -1;

		protected UniformImpl(String name, Consumer<T> initializer, UniformRefreshFrequency frequency) {
			this.name = name;
			this.initializer = initializer;
			this.frequency = frequency;
			uniforms.add(this);
		}

		public final void setDirty() {
			hasDirty = true;
			flags |= FLAG_NEEDS_UPLOAD;
		}

		protected final void markForInitialization() {
			hasDirty = true;
			flags |= FLAG_NEEDS_INITIALIZATION;
		}

		private void load(int programID) {
			this.unifID = GL21.glGetUniformLocation(programID, name);

			if (this.unifID == -1) {
				if (Configurator.logMissingUniforms) {
					CanvasMod.LOG.info(I18n.translate("debug.canvas.missing_uniform", name, vertexShader.getShaderSourceId().toString(), fragmentShader.getShaderSourceId().toString()));
				}

				this.flags = 0;
			} else {
				// dirty flag will be reset before uniforms are loaded
				hasDirty = true;
				this.flags = FLAG_NEEDS_INITIALIZATION | FLAG_NEEDS_UPLOAD;
			}
		}

		@SuppressWarnings("unchecked")
		public final void upload() {
			if (flags == 0) {
				return;
			}

			if ((flags & FLAG_NEEDS_INITIALIZATION) == FLAG_NEEDS_INITIALIZATION) {
				initializer.accept((T) this);
				assert CanvasGlHelper.checkError();
			}

			if ((flags & FLAG_NEEDS_UPLOAD) == FLAG_NEEDS_UPLOAD && unifID != -1) {
				uploadInner();
				assert CanvasGlHelper.checkError();
			}

			flags = 0;
		}

		protected abstract void uploadInner();

		public abstract String searchString();
	}

	protected abstract class UniformFloat<T extends Uniform> extends UniformImpl<T> {
		protected FloatBuffer uniformFloatBuffer;

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
			if (unifID == -1) {
				return;
			}

			if (uniformFloatBuffer.get(0) != value) {
				uniformFloatBuffer.put(0, value);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform1fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "float";
		}
	}

	public class Uniform2fImpl extends UniformFloat<Uniform2f> implements Uniform2f {
		protected Uniform2fImpl(String name, Consumer<Uniform2f> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 2);
		}

		@Override
		public final void set(float v0, float v1) {
			if (unifID == -1) {
				return;
			}

			if (uniformFloatBuffer.get(0) != v0) {
				uniformFloatBuffer.put(0, v0);
				setDirty();
			}

			if (uniformFloatBuffer.get(1) != v1) {
				uniformFloatBuffer.put(1, v1);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform2fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "vec2";
		}
	}

	public class Uniform3fImpl extends UniformFloat<Uniform3f> implements Uniform3f {
		protected Uniform3fImpl(String name, Consumer<Uniform3f> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 3);
		}

		@Override
		public final void set(float v0, float v1, float v2) {
			if (unifID == -1) {
				return;
			}

			if (uniformFloatBuffer.get(0) != v0) {
				uniformFloatBuffer.put(0, v0);
				setDirty();
			}

			if (uniformFloatBuffer.get(1) != v1) {
				uniformFloatBuffer.put(1, v1);
				setDirty();
			}

			if (uniformFloatBuffer.get(2) != v2) {
				uniformFloatBuffer.put(2, v2);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform3fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "vec3";
		}
	}

	public class Uniform4fImpl extends UniformFloat<Uniform4f> implements Uniform4f {
		protected Uniform4fImpl(String name, Consumer<Uniform4f> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 4);
		}

		@Override
		public final void set(float v0, float v1, float v2, float v3) {
			if (unifID == -1) {
				return;
			}

			if (uniformFloatBuffer.get(0) != v0) {
				uniformFloatBuffer.put(0, v0);
				setDirty();
			}

			if (uniformFloatBuffer.get(1) != v1) {
				uniformFloatBuffer.put(1, v1);
				setDirty();
			}

			if (uniformFloatBuffer.get(2) != v2) {
				uniformFloatBuffer.put(2, v2);
				setDirty();
			}

			if (uniformFloatBuffer.get(3) != v3) {
				uniformFloatBuffer.put(3, v3);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform4fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "vec4";
		}
	}

	public class UniformArrayfImpl extends UniformFloat<UniformArrayf> implements UniformArrayf {
		@Nullable protected FloatBuffer externalFloatBuffer;

		protected UniformArrayfImpl(String name, Consumer<UniformArrayf> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size);
		}

		@Override
		public void setExternal(FloatBuffer externalFloatBuffer) {
			this.externalFloatBuffer = externalFloatBuffer;
			setDirty();
		}

		@Override
		public final void set(float[] data) {
			if (unifID == -1) {
				return;
			}

			final int limit = data.length;

			for (int i = 0; i < limit; i++) {
				if (uniformFloatBuffer.get(i) != data[i]) {
					uniformFloatBuffer.put(i, data[i]);
					setDirty();
				}
			}
		}

		@Override
		protected void uploadInner() {
			if (externalFloatBuffer == null) {
				GL21.glUniform1fv(unifID, uniformFloatBuffer);
			} else {
				GL21.glUniform1fv(unifID, externalFloatBuffer);
				externalFloatBuffer = null;
			}
		}

		@Override
		public String searchString() {
			return "float\\s*\\[\\s*[0-9]+\\s*]";
		}
	}

	public class UniformArray4fImpl extends UniformFloat<UniformArray4f> implements UniformArray4f {
		protected UniformArray4fImpl(String name, Consumer<UniformArray4f> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size * 4);
		}

		@Override
		public void setExternal(FloatBuffer externalFloatBuffer) {
			if (unifID == -1) {
				return;
			}

			uniformFloatBuffer = externalFloatBuffer;
			setDirty();
		}

		@Override
		protected void uploadInner() {
			if (uniformFloatBuffer != null) {
				GL21.glUniform4fv(unifID, uniformFloatBuffer);
				uniformFloatBuffer = null;
			}
		}

		@Override
		public String searchString() {
			return "vec4\\s*\\[\\s*[0-9]+\\s*]";
		}

		@Override
		public void set(float[] v) {
			uniformFloatBuffer.put(v, 0, v.length);
		}
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
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != value) {
				uniformIntBuffer.put(0, value);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform1iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "int";
		}
	}

	public class UniformSamplerImpl extends Uniform1iImpl {
		protected UniformSamplerImpl(String name, Consumer<Uniform1i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency);
		}

		@Override
		public String searchString() {
			return "sampler2D";
		}
	}

	public class Uniform2iImpl extends UniformInt<Uniform2i> implements Uniform2i {
		protected Uniform2iImpl(String name, Consumer<Uniform2i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 2);
		}

		@Override
		public final void set(int v0, int v1) {
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != v0) {
				uniformIntBuffer.put(0, v0);
				setDirty();
			}

			if (uniformIntBuffer.get(1) != v1) {
				uniformIntBuffer.put(1, v1);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform2iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "ivec2";
		}
	}

	public class Uniform3iImpl extends UniformInt<Uniform3i> implements Uniform3i {
		protected Uniform3iImpl(String name, Consumer<Uniform3i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 3);
		}

		@Override
		public final void set(int v0, int v1, int v2) {
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != v0) {
				uniformIntBuffer.put(0, v0);
				setDirty();
			}

			if (uniformIntBuffer.get(1) != v1) {
				uniformIntBuffer.put(1, v1);
				setDirty();
			}

			if (uniformIntBuffer.get(2) != v2) {
				uniformIntBuffer.put(2, v2);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform3iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "ivec3";
		}
	}

	public class Uniform4iImpl extends UniformInt<Uniform4i> implements Uniform4i {
		protected Uniform4iImpl(String name, Consumer<Uniform4i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 4);
		}

		@Override
		public final void set(int v0, int v1, int v2, int v3) {
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != v0) {
				uniformIntBuffer.put(0, v0);
				setDirty();
			}

			if (uniformIntBuffer.get(1) != v1) {
				uniformIntBuffer.put(1, v1);
				setDirty();
			}

			if (uniformIntBuffer.get(2) != v2) {
				uniformIntBuffer.put(2, v2);
				setDirty();
			}

			if (uniformIntBuffer.get(3) != v3) {
				uniformIntBuffer.put(3, v3);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform4iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "ivec4";
		}
	}

	public class UniformArrayiImpl extends UniformInt<UniformArrayi> implements UniformArrayi {
		protected UniformArrayiImpl(String name, Consumer<UniformArrayi> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size);
		}

		@Override
		public final void set(int[] data) {
			if (unifID == -1) {
				return;
			}

			final int limit = data.length;

			for (int i = 0; i < limit; i++) {
				if (uniformIntBuffer.get(i) != data[i]) {
					uniformIntBuffer.put(i, data[i]);
					setDirty();
				}
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform1iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "int\\s*\\[\\s*[0-9]+\\s*]";
		}
	}

	public class Uniform1uiImpl extends UniformInt<Uniform1ui> implements Uniform1ui {
		protected Uniform1uiImpl(String name, Consumer<Uniform1ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 1);
		}

		@Override
		public final void set(int value) {
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != value) {
				uniformIntBuffer.put(0, value);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			if (MinecraftClient.IS_SYSTEM_MAC) {
				EXTGPUShader4.glUniform1uivEXT(unifID, uniformIntBuffer);
			} else {
				GL30.glUniform1uiv(unifID, uniformIntBuffer);
			}
		}

		@Override
		public String searchString() {
			return "uint";
		}
	}

	public class Uniform2uiImpl extends UniformInt<Uniform2ui> implements Uniform2ui {
		protected Uniform2uiImpl(String name, Consumer<Uniform2ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 2);
		}

		@Override
		public final void set(int v0, int v1) {
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != v0) {
				uniformIntBuffer.put(0, v0);
				setDirty();
			}

			if (uniformIntBuffer.get(1) != v1) {
				uniformIntBuffer.put(1, v1);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			if (MinecraftClient.IS_SYSTEM_MAC) {
				EXTGPUShader4.glUniform2uivEXT(unifID, uniformIntBuffer);
			} else {
				GL30.glUniform2uiv(unifID, uniformIntBuffer);
			}
		}

		@Override
		public String searchString() {
			return "uvec2";
		}
	}

	public class Uniform3uiImpl extends UniformInt<Uniform3ui> implements Uniform3ui {
		protected Uniform3uiImpl(String name, Consumer<Uniform3ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 3);
		}

		@Override
		public final void set(int v0, int v1, int v2) {
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != v0) {
				uniformIntBuffer.put(0, v0);
				setDirty();
			}

			if (uniformIntBuffer.get(1) != v1) {
				uniformIntBuffer.put(1, v1);
				setDirty();
			}

			if (uniformIntBuffer.get(2) != v2) {
				uniformIntBuffer.put(2, v2);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			if (MinecraftClient.IS_SYSTEM_MAC) {
				EXTGPUShader4.glUniform3uivEXT(unifID, uniformIntBuffer);
			} else {
				GL30.glUniform3uiv(unifID, uniformIntBuffer);
			}
		}

		@Override
		public String searchString() {
			return "uvec3";
		}
	}

	public class Uniform4uiImpl extends UniformInt<Uniform4ui> implements Uniform4ui {
		protected Uniform4uiImpl(String name, Consumer<Uniform4ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 4);
		}

		@Override
		public final void set(int v0, int v1, int v2, int v3) {
			if (unifID == -1) {
				return;
			}

			if (uniformIntBuffer.get(0) != v0) {
				uniformIntBuffer.put(0, v0);
				setDirty();
			}

			if (uniformIntBuffer.get(1) != v1) {
				uniformIntBuffer.put(1, v1);
				setDirty();
			}

			if (uniformIntBuffer.get(2) != v2) {
				uniformIntBuffer.put(2, v2);
				setDirty();
			}

			if (uniformIntBuffer.get(3) != v3) {
				uniformIntBuffer.put(3, v3);
				setDirty();
			}
		}

		@Override
		protected void uploadInner() {
			if (MinecraftClient.IS_SYSTEM_MAC) {
				EXTGPUShader4.glUniform4uivEXT(unifID, uniformIntBuffer);
			} else {
				GL30.glUniform4uiv(unifID, uniformIntBuffer);
			}
		}

		@Override
		public String searchString() {
			return "uvec4";
		}
	}

	public class UniformArrayuiImpl extends UniformInt<UniformArrayui> implements UniformArrayui {
		private @Nullable IntBuffer externalBuffer;

		protected UniformArrayuiImpl(String name, Consumer<UniformArrayui> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size);
		}

		@Override
		public final void set(int[] data) {
			if (unifID == -1) {
				return;
			}

			final int limit = data.length;

			for (int i = 0; i < limit; i++) {
				if (uniformIntBuffer.get(i) != data[i]) {
					uniformIntBuffer.put(i, data[i]);
					setDirty();
				}
			}
		}

		@Override
		protected void uploadInner() {
			final IntBuffer source = externalBuffer == null ? uniformIntBuffer : externalBuffer;
			externalBuffer = null;

			if (MinecraftClient.IS_SYSTEM_MAC) {
				EXTGPUShader4.glUniform1uivEXT(unifID, source);
			} else {
				GL30.glUniform1uiv(unifID, source);
			}
		}

		@Override
		public String searchString() {
			return "uint\\s*\\[\\s*[0-9]+\\s*]";
		}

		@Override
		public void setExternal(IntBuffer buff) {
			if (unifID == -1) {
				return;
			}

			externalBuffer = buff;
			setDirty();
		}
	}

	public class UniformMatrix4fArrayImpl extends UniformImpl<UniformMatrix4fArrayImpl> implements Uniform {
		protected FloatBuffer uniformFloatBuffer;

		/**
		 * Requires a shared direct buffer.
		 */
		protected UniformMatrix4fArrayImpl(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4fArrayImpl> initializer) {
			super(name, initializer, frequency);
		}

		public final void set(FloatBuffer uniformFloatBuffer) {
			if (unifID == -1) {
				return;
			}

			this.uniformFloatBuffer = uniformFloatBuffer;
			setDirty();
		}

		@Override
		protected void uploadInner() {
			if (uniformFloatBuffer != null) {
				GL21.glUniformMatrix4fv(unifID, false, uniformFloatBuffer);
			}
		}

		@Override
		public String searchString() {
			return "mat4\\s*\\[\\s*[0-9]+\\s*]";
		}
	}

	public class UniformMatrix3fImpl extends UniformImpl<UniformMatrix3f> implements UniformMatrix3f {
		protected final FloatBuffer uniformFloatBuffer;
		protected final long bufferAddress;
		protected final Matrix3f lastValue = new Matrix3f();

		protected UniformMatrix3fImpl(String name, Consumer<UniformMatrix3f> initializer, UniformRefreshFrequency frequency) {
			this(name, initializer, frequency, BufferUtils.createFloatBuffer(9));
		}

		/**
		 * Use when have a shared direct buffer.
		 */
		protected UniformMatrix3fImpl(String name, Consumer<UniformMatrix3f> initializer, UniformRefreshFrequency frequency, FloatBuffer uniformFloatBuffer) {
			super(name, initializer, frequency);
			this.uniformFloatBuffer = uniformFloatBuffer;
			bufferAddress = MemoryUtil.memAddress(this.uniformFloatBuffer);
		}

		@Override
		public final void set(Matrix3f matrix) {
			if (unifID == -1) {
				return;
			}

			if (matrix == null || matrix.equals(lastValue)) {
				return;
			}

			final Matrix3fExt m = (Matrix3fExt) (Object) matrix;
			((Matrix3fExt) (Object) lastValue).set(m);

			m.writeToBuffer(uniformFloatBuffer);

			setDirty();
		}

		@Override
		protected void uploadInner() {
			GL21.glUniformMatrix3fv(unifID, false, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "mat3";
		}
	}

	public void forceReload() {
		fragmentShader.forceReload();
		vertexShader.forceReload();
		needsLoad = true;
	}
}
