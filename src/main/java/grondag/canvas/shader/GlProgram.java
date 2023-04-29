/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.shader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.resources.language.I18n;

import grondag.canvas.CanvasMod;
import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.config.Configurator;
import grondag.canvas.shader.data.ShaderUniforms;
import grondag.canvas.shader.data.UniformRefreshFrequency;
import grondag.canvas.varia.GFX;

public class GlProgram {
	static {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: GlProgram static init");
		}
	}

	private static GlProgram activeProgram;
	private final String name;
	private final Shader vertexShader;
	private final Shader fragmentShader;
	public final CanvasVertexFormat vertexFormat;
	public final ProgramType programType;
	private final ObjectArrayList<Uniform<?>> uniforms = new ObjectArrayList<>();
	private final ObjectArrayList<Uniform<?>> activeUniforms = new ObjectArrayList<>();
	private final ObjectArrayList<Uniform<?>> renderTickUpdates = new ObjectArrayList<>();
	private final ObjectArrayList<Uniform<?>> gameTickUpdates = new ObjectArrayList<>();

	protected boolean hasDirty = false;
	private int progID = -1;
	private boolean isErrored = false;
	private boolean needsLoad = true;

	GlProgram(String name, Shader vertexShader, Shader fragmentShader, CanvasVertexFormat format, ProgramType programType) {
		this.name = name;
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.programType = programType;
		vertexFormat = format;
		ShaderUniforms.COMMON_UNIFORM_SETUP.accept(this);
	}

	public static void deactivate() {
		if (activeProgram != null) {
			activeProgram = null;
			GFX.useProgram(0);
		}
	}

	public static GlProgram activeProgram() {
		return activeProgram;
	}

	public int programId() {
		return progID;
	}

	public Uniform1f uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
		return new Uniform1f(name, initializer, frequency);
	}

	public Uniform2f uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
		return new Uniform2f(name, initializer, frequency);
	}

	public Uniform3f uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
		return new Uniform3f(name, initializer, frequency);
	}

	public Uniform4f uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
		return new Uniform4f(name, initializer, frequency);
	}

	public UniformArrayf uniformArrayf(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayf> initializer, int size) {
		return new UniformArrayf(name, initializer, frequency, size);
	}

	public UniformArray4f uniformArray4f(String name, UniformRefreshFrequency frequency, Consumer<UniformArray4f> initializer, int size) {
		return new UniformArray4f(name, initializer, frequency, size);
	}

	public Uniform1i uniformSampler(String type, String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		return new UniformSampler(type, name, initializer, frequency);
	}

	public Uniform1i uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		return new Uniform1i(name, initializer, frequency);
	}

	public Uniform2i uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
		return new Uniform2i(name, initializer, frequency);
	}

	public Uniform3i uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
		return new Uniform3i(name, initializer, frequency);
	}

	public Uniform4i uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
		return new Uniform4i(name, initializer, frequency);
	}

	public UniformArrayi uniformArrayi(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayi> initializer, int size) {
		return new UniformArrayi(name, initializer, frequency, size);
	}

	public Uniform1ui uniform1ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform1ui> initializer) {
		return new Uniform1ui(name, initializer, frequency);
	}

	public Uniform2ui uniform2ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform2ui> initializer) {
		return new Uniform2ui(name, initializer, frequency);
	}

	public Uniform3ui uniform3ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform3ui> initializer) {
		return new Uniform3ui(name, initializer, frequency);
	}

	public Uniform4ui uniform4ui(String name, UniformRefreshFrequency frequency, Consumer<Uniform4ui> initializer) {
		return new Uniform4ui(name, initializer, frequency);
	}

	public UniformArrayui uniformArrayui(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayui> initializer, int size) {
		return new UniformArrayui(name, initializer, frequency, size);
	}

	protected void removeUniform(Uniform<?> uniform) {
		assert uniforms.contains(uniform);
		uniform.unload();
		uniforms.remove(uniform);
	}

	public final void activate() {
		final boolean created = needsLoad;

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

			// Label needs to be set after binding the program
			if (created) GFX.objectLabel(GFX.GL_PROGRAM, programId(), "PRO " + name);
		}
	}

	private void activateInner() {
		if (isErrored) {
			return;
		}

		GFX.useProgram(progID);

		if (!GFX.checkError()) {
			isErrored = true;
			CanvasMod.LOG.warn(String.format("Unable to activate program with shaders %s and %s.  Program was disabled.", vertexShader.getShaderSourceId(), fragmentShader.getShaderSourceId()));
			return;
		}

		if (hasDirty) {
			final int count = activeUniforms.size();

			for (int i = 0; i < count; i++) {
				activeUniforms.get(i).upload();
			}

			hasDirty = false;
		}
	}

	public UniformMatrix4fArray uniformMatrix4fArray(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4fArray> initializer) {
		return new UniformMatrix4fArray(name, frequency, initializer);
	}

	public UniformMatrix3f uniformMatrix3f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix3f> initializer) {
		return new UniformMatrix3f(name, initializer, frequency);
	}

	public UniformMatrix4f uniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
		return new UniformMatrix4f(name, frequency, initializer);
	}

	public UniformMatrix3f uniformMatrix3f(String name, UniformRefreshFrequency frequency, FloatBuffer floatBuffer, Consumer<UniformMatrix3f> initializer) {
		return new UniformMatrix3f(name, initializer, frequency);
	}

	private void findActiveUniforms() {
		activeUniforms.clear();
		renderTickUpdates.clear();
		gameTickUpdates.clear();

		final int limit = uniforms.size();

		for (int i = 0; i < limit; ++i) {
			final Uniform<?> u = uniforms.get(i);

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
				GFX.deleteProgram(progID);
			}

			progID = GFX.createProgram();

			isErrored = progID > 0 && !loadInner();
		} catch (final Exception e) {
			if (progID > 0) {
				GFX.deleteProgram(progID);
			}

			CanvasMod.LOG.error(I18n.get("error.canvas.program_link_failure"), e);
			progID = -1;
		}

		if (!isErrored) {
			findActiveUniforms();
			final int limit = activeUniforms.size();

			for (int i = 0; i < limit; i++) {
				activeUniforms.get(i).load(progID);
			}

			GlProgramManager.INSTANCE.add(this);
		}
	}

	public final void unload() {
		for (final Uniform<?> u : uniforms) {
			u.unload();
		}

		if (progID > 0) {
			GFX.deleteProgram(progID);
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

		GFX.linkProgram(programID);

		if (GFX.getProgramInfo(programID, GFX.GL_LINK_STATUS) == GFX.GL_FALSE) {
			CanvasMod.LOG.error(GFX.getProgramInfoLog(programID));
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

	public boolean containsUniformSpec(Uniform<?> uniform) {
		return containsUniformSpec(uniform.searchString(), uniform.uniformName);
	}

	public boolean containsUniformSpec(String type, String name) {
		return vertexShader.containsUniformSpec(type, name)
				|| fragmentShader.containsUniformSpec(type, name);
	}

	public abstract class Uniform<T extends Uniform<T>> {
		protected static final int FLAG_NEEDS_UPLOAD = 1;
		protected static final int FLAG_NEEDS_INITIALIZATION = 2;
		protected final Consumer<T> initializer;
		protected final UniformRefreshFrequency frequency;
		private final String uniformName;
		protected int flags = 0;
		protected int unifID = -1;

		protected Uniform(String name, Consumer<T> initializer, UniformRefreshFrequency frequency) {
			this.uniformName = name;
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

		public void unload() {
			this.unifID = -1;
		}

		private void load(int programID) {
			this.unifID = GFX.getUniformLocation(programID, uniformName);

			if (this.unifID == -1) {
				if (Configurator.logMissingUniforms) {
					CanvasMod.LOG.info(I18n.get("debug.canvas.missing_uniform", uniformName, vertexShader.getShaderSourceId().toString(), fragmentShader.getShaderSourceId().toString()));
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
			if (flags == 0 || unifID == -1) {
				return;
			}

			if ((flags & FLAG_NEEDS_INITIALIZATION) == FLAG_NEEDS_INITIALIZATION) {
				initializer.accept((T) this);
			}

			if ((flags & FLAG_NEEDS_UPLOAD) == FLAG_NEEDS_UPLOAD) {
				// make sure any error is ours
				GFX.getError();
				uploadInner();

				if (!GFX.checkError()) {
					CanvasMod.LOG.info(I18n.get("debug.canvas.missing_uniform", uniformName, vertexShader.getShaderSourceId().toString(), fragmentShader.getShaderSourceId().toString()));
					unifID = -1;
				}
			}

			flags = 0;
		}

		protected abstract void uploadInner();

		public abstract String searchString();
	}

	protected abstract class UniformFloat<T extends Uniform<T>> extends Uniform<T> {
		protected FloatBuffer uniformFloatBuffer;

		protected UniformFloat(String name, Consumer<T> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency);
			this.uniformFloatBuffer = BufferUtils.createFloatBuffer(size);
		}
	}

	public class Uniform1f extends UniformFloat<Uniform1f> {
		protected Uniform1f(String name, Consumer<Uniform1f> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 1);
		}

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
			GFX.uniform1fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "float";
		}
	}

	public class Uniform2f extends UniformFloat<Uniform2f> {
		protected Uniform2f(String name, Consumer<Uniform2f> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 2);
		}

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
			GFX.uniform2fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "vec2";
		}
	}

	public class Uniform3f extends UniformFloat<Uniform3f> {
		protected Uniform3f(String name, Consumer<Uniform3f> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 3);
		}

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
			GFX.uniform3fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "vec3";
		}
	}

	public class Uniform4f extends UniformFloat<Uniform4f> {
		protected Uniform4f(String name, Consumer<Uniform4f> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 4);
		}

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
			GFX.uniform4fv(unifID, uniformFloatBuffer);
		}

		@Override
		public String searchString() {
			return "vec4";
		}
	}

	public class UniformArrayf extends UniformFloat<UniformArrayf> {
		@Nullable protected FloatBuffer externalFloatBuffer;

		protected UniformArrayf(String name, Consumer<UniformArrayf> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size);
		}

		public void setExternal(FloatBuffer externalFloatBuffer) {
			this.externalFloatBuffer = externalFloatBuffer;
			setDirty();
		}

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
				GFX.uniform1fv(unifID, uniformFloatBuffer);
			} else {
				GFX.uniform1fv(unifID, externalFloatBuffer);
				externalFloatBuffer = null;
			}
		}

		@Override
		public String searchString() {
			return "float\\s*\\[\\s*[0-9]+\\s*]";
		}
	}

	public class UniformArray4f extends UniformFloat<UniformArray4f> {
		protected UniformArray4f(String name, Consumer<UniformArray4f> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size * 4);
		}

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
				GFX.uniform4fv(unifID, uniformFloatBuffer);
				uniformFloatBuffer = null;
			}
		}

		@Override
		public String searchString() {
			return "vec4\\s*\\[\\s*[0-9]+\\s*]";
		}

		public void set(float[] v) {
			uniformFloatBuffer.put(v, 0, v.length);
		}
	}

	protected abstract class UniformInt<T extends Uniform<T>> extends Uniform<T> {
		protected final IntBuffer uniformIntBuffer;

		protected UniformInt(String name, Consumer<T> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency);
			this.uniformIntBuffer = BufferUtils.createIntBuffer(size);
		}
	}

	public class Uniform1i extends UniformInt<Uniform1i> {
		protected Uniform1i(String name, Consumer<Uniform1i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 1);
		}

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
			GFX.uniform1iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "int";
		}
	}

	public class UniformSampler extends Uniform1i {
		private final String type;

		public UniformSampler(String type, String name, Consumer<Uniform1i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency);
			this.type = type;
		}

		@Override
		public String searchString() {
			return type;
		}
	}

	public class Uniform2i extends UniformInt<Uniform2i> {
		protected Uniform2i(String name, Consumer<Uniform2i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 2);
		}

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
			GFX.uniform2iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "ivec2";
		}
	}

	public class Uniform3i extends UniformInt<Uniform3i> {
		protected Uniform3i(String name, Consumer<Uniform3i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 3);
		}

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
			GFX.uniform3iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "ivec3";
		}
	}

	public class Uniform4i extends UniformInt<Uniform4i> {
		protected Uniform4i(String name, Consumer<Uniform4i> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 4);
		}

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
			GFX.uniform4iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "ivec4";
		}
	}

	public class UniformArrayi extends UniformInt<UniformArrayi> {
		protected UniformArrayi(String name, Consumer<UniformArrayi> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size);
		}

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
			GFX.uniform1iv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "int\\s*\\[\\s*[0-9]+\\s*]";
		}
	}

	public class Uniform1ui extends UniformInt<Uniform1ui> {
		protected Uniform1ui(String name, Consumer<Uniform1ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 1);
		}

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
			GFX.uniform1uiv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "uint";
		}
	}

	public class Uniform2ui extends UniformInt<Uniform2ui> {
		protected Uniform2ui(String name, Consumer<Uniform2ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 2);
		}

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
			GFX.uniform2uiv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "uvec2";
		}
	}

	public class Uniform3ui extends UniformInt<Uniform3ui> {
		protected Uniform3ui(String name, Consumer<Uniform3ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 3);
		}

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
			GFX.uniform3uiv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "uvec3";
		}
	}

	public class Uniform4ui extends UniformInt<Uniform4ui> {
		protected Uniform4ui(String name, Consumer<Uniform4ui> initializer, UniformRefreshFrequency frequency) {
			super(name, initializer, frequency, 4);
		}

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
			GFX.uniform4uiv(unifID, uniformIntBuffer);
		}

		@Override
		public String searchString() {
			return "uvec4";
		}
	}

	public class UniformArrayui extends UniformInt<UniformArrayui> {
		private @Nullable IntBuffer externalBuffer;

		protected UniformArrayui(String name, Consumer<UniformArrayui> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size);
		}

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
			GFX.uniform1uiv(unifID, source);
		}

		@Override
		public String searchString() {
			return "uint\\s*\\[\\s*[0-9]+\\s*]";
		}

		public void setExternal(IntBuffer buff) {
			if (unifID == -1) {
				return;
			}

			externalBuffer = buff;
			setDirty();
		}
	}

	public class UniformMatrix4fArray extends Uniform<UniformMatrix4fArray> {
		protected FloatBuffer uniformFloatBuffer;

		/**
		 * Requires a shared direct buffer.
		 */
		protected UniformMatrix4fArray(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4fArray> initializer) {
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
				GFX.uniformMatrix4fv(unifID, false, uniformFloatBuffer);
			}
		}

		@Override
		public String searchString() {
			return "mat4\\s*\\[\\s*[0-9]+\\s*]";
		}
	}

	public class UniformMatrix4f extends Uniform<UniformMatrix4f> {
		protected FloatBuffer uniformFloatBuffer = BufferUtils.createFloatBuffer(16);

		protected UniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
			super(name, initializer, frequency);
		}

		public final void set(Matrix4f matrix) {
			if (unifID == -1) {
				return;
			}

			matrix.get(0, uniformFloatBuffer);

			setDirty();
		}

		@Override
		protected void uploadInner() {
			if (uniformFloatBuffer != null) {
				GFX.uniformMatrix4fv(unifID, false, uniformFloatBuffer);
			}
		}

		@Override
		public String searchString() {
			return "mat4";
		}
	}

	public class UniformMatrix3f extends Uniform<UniformMatrix3f> {
		protected final FloatBuffer uniformFloatBuffer;
		protected final long bufferAddress;
		protected final Matrix3f lastValue = new Matrix3f();

		protected UniformMatrix3f(String name, Consumer<UniformMatrix3f> initializer, UniformRefreshFrequency frequency) {
			this(name, initializer, frequency, BufferUtils.createFloatBuffer(9));
		}

		/**
		 * Use when have a shared direct buffer.
		 */
		protected UniformMatrix3f(String name, Consumer<UniformMatrix3f> initializer, UniformRefreshFrequency frequency, FloatBuffer uniformFloatBuffer) {
			super(name, initializer, frequency);
			this.uniformFloatBuffer = uniformFloatBuffer;
			bufferAddress = MemoryUtil.memAddress(this.uniformFloatBuffer);
		}

		public final void set(Matrix3f matrix) {
			if (unifID == -1) {
				return;
			}

			if (matrix == null || matrix.equals(lastValue)) {
				return;
			}

			lastValue.set(matrix);

			matrix.get(uniformFloatBuffer);

			setDirty();
		}

		@Override
		protected void uploadInner() {
			GFX.uniformMatrix3fv(unifID, false, uniformFloatBuffer);
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
