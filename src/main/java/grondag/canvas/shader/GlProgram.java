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

package grondag.canvas.shader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.resource.language.I18n;

import grondag.canvas.CanvasMod;
import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.varia.CanvasGlHelper;
import grondag.frex.api.material.Uniform;
import grondag.frex.api.material.Uniform.Uniform1f;
import grondag.frex.api.material.Uniform.Uniform1i;
import grondag.frex.api.material.Uniform.Uniform2f;
import grondag.frex.api.material.Uniform.Uniform2i;
import grondag.frex.api.material.Uniform.Uniform3f;
import grondag.frex.api.material.Uniform.Uniform3i;
import grondag.frex.api.material.Uniform.Uniform4f;
import grondag.frex.api.material.Uniform.Uniform4i;
import grondag.frex.api.material.Uniform.UniformArrayf;
import grondag.frex.api.material.Uniform.UniformArrayi;
import grondag.frex.api.material.Uniform.UniformMatrix4f;
import grondag.frex.api.material.UniformRefreshFrequency;

public class GlProgram {
	private static GlProgram activeProgram;

	public static void deactivate() {
		if (activeProgram != null) {
			activeProgram = null;
			GL21.glUseProgram(0);
		}
	}

	private int progID = -1;
	private boolean isErrored = false;

	public final GlShader vertexShader;
	public final GlShader fragmentShader;
	public final ShaderPass pass;
	public final MaterialVertexFormat pipelineVertexFormat;

	// UGLY: special casing, public
	public Uniform3fImpl modelOrigin;

	private final ObjectArrayList<UniformImpl<?>> uniforms = new ObjectArrayList<>();
	private final ObjectArrayList<UniformImpl<?>> renderTickUpdates = new ObjectArrayList<>();
	private final ObjectArrayList<UniformImpl<?>> gameTickUpdates = new ObjectArrayList<>();

	protected boolean hasDirty = false;

	public int programId() {
		return progID;
	}

	public void actvateWithiModelOrigin(int x, int y, int z) {
		activate();
		modelOrigin.set(x, y, z);
		modelOrigin.upload();
	}

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
			hasDirty = true;
			flags |= FLAG_NEEDS_UPLOAD;
		}

		protected final void markForInitialization() {
			hasDirty = true;
			flags |= FLAG_NEEDS_INITIALIZATION;
		}

		private final void load(int programID) {
			this.unifID = GL21.glGetUniformLocation(programID, name);
			if (this.unifID == -1) {
				CanvasMod.LOG.debug(I18n.translate("debug.canvas.missing_uniform", name,
						vertexShader.shaderSource.toString(), fragmentShader.shaderSource.toString()));
				this.flags = 0;
			} else {
				// dirty flag will be reset before uniforms are loaded
				hasDirty = true;
				this.flags = FLAG_NEEDS_INITIALIZATION | FLAG_NEEDS_UPLOAD;
			}
		}

		@SuppressWarnings("unchecked")
		public final void upload() {
			if (this.flags == 0) {
				return;
			}

			if ((this.flags & FLAG_NEEDS_INITIALIZATION) == FLAG_NEEDS_INITIALIZATION) {
				this.initializer.accept((T) this);
			}

			if ((this.flags & FLAG_NEEDS_UPLOAD) == FLAG_NEEDS_UPLOAD) {
				this.uploadInner();
			}

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
	}

	public class UniformArrayfImpl extends UniformFloat<UniformArrayf> implements UniformArrayf {
		protected UniformArrayfImpl(String name, Consumer<UniformArrayf> initializer, UniformRefreshFrequency frequency, int size) {
			super(name, initializer, frequency, size);
		}

		@Override
		public final void set(float[] data) {
			if (unifID == -1) {
				return;
			}

			final int limit = data.length;
			for(int i = 0; i < limit; i++) {
				if (uniformFloatBuffer.get(i) != data[i]) {
					uniformFloatBuffer.put(i, data[i]);
					setDirty();
				}
			}
		}

		@Override
		protected void uploadInner() {
			GL21.glUniform1fv(unifID, uniformFloatBuffer);
		}
	}

	private <T extends UniformImpl<?>> void addUniform(T toAdd) {
		uniforms.add(toAdd);
		if (toAdd.frequency == UniformRefreshFrequency.PER_FRAME) {
			renderTickUpdates.add(toAdd);
		} else if (toAdd.frequency == UniformRefreshFrequency.PER_TICK) {
			gameTickUpdates.add(toAdd);
		}
	}

	public Uniform1f uniform1f(String name, UniformRefreshFrequency frequency, Consumer<Uniform1f> initializer) {
		final Uniform1fImpl result = new Uniform1fImpl(name, initializer, frequency);

		if (containsUniformSpec("float", name)) {
			addUniform(result);
		}

		return result;
	}

	public Uniform2f uniform2f(String name, UniformRefreshFrequency frequency, Consumer<Uniform2f> initializer) {
		final Uniform2fImpl result = new Uniform2fImpl(name, initializer, frequency);

		if (containsUniformSpec("vec2", name)) {
			addUniform(result);
		}

		return result;
	}

	public Uniform3f uniform3f(String name, UniformRefreshFrequency frequency, Consumer<Uniform3f> initializer) {
		final Uniform3fImpl result = new Uniform3fImpl(name, initializer, frequency);

		if (containsUniformSpec("vec3", name)) {
			addUniform(result);
		}

		return result;
	}

	public Uniform4f uniform4f(String name, UniformRefreshFrequency frequency, Consumer<Uniform4f> initializer) {
		final Uniform4fImpl result = new Uniform4fImpl(name, initializer, frequency);

		if (containsUniformSpec("vec4", name)) {
			addUniform(result);
		}

		return result;
	}

	public UniformArrayf uniformArrayf(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayf> initializer, int size) {
		final UniformArrayfImpl result = new UniformArrayfImpl(name, initializer, frequency, size);

		if (containsUniformSpec("float\\s*\\[\\s*[0-9]+\\s*]", name)) {
			addUniform(result);
		}

		return result;
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
			for(int i = 0; i < limit; i++) {
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
	}

	public Uniform1i uniformSampler2d(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		final Uniform1iImpl result = new Uniform1iImpl(name, initializer, frequency);

		if (containsUniformSpec("sampler2D", name)) {
			addUniform(result);
		}

		return result;
	}

	public Uniform1i uniform1i(String name, UniformRefreshFrequency frequency, Consumer<Uniform1i> initializer) {
		final Uniform1iImpl result = new Uniform1iImpl(name, initializer, frequency);

		if (containsUniformSpec("int", name)) {
			addUniform(result);
		}

		return result;
	}

	public Uniform2i uniform2i(String name, UniformRefreshFrequency frequency, Consumer<Uniform2i> initializer) {
		final Uniform2iImpl result = new Uniform2iImpl(name, initializer, frequency);

		if (containsUniformSpec("ivec2", name)) {
			addUniform(result);
		}

		return result;
	}

	public Uniform3i uniform3i(String name, UniformRefreshFrequency frequency, Consumer<Uniform3i> initializer) {
		final Uniform3iImpl result = new Uniform3iImpl(name, initializer, frequency);

		if (containsUniformSpec("ivec3", name)) {
			addUniform(result);
		}

		return result;
	}

	public Uniform4i uniform4i(String name, UniformRefreshFrequency frequency, Consumer<Uniform4i> initializer) {
		final Uniform4iImpl result = new Uniform4iImpl(name, initializer, frequency);

		if (containsUniformSpec("ivec4", name)) {
			addUniform(result);
		}

		return result;
	}

	public UniformArrayi uniformArrayi(String name, UniformRefreshFrequency frequency, Consumer<UniformArrayi> initializer, int size) {
		final UniformArrayiImpl result = new UniformArrayiImpl(name, initializer, frequency, size);

		if (containsUniformSpec("int\\s*\\[\\s*[0-9]+\\s*]", name)) {
			addUniform(result);
		}

		return result;
	}

	public GlProgram(GlShader vertexShader, GlShader fragmentShader, MaterialVertexFormat format, ShaderContext shaderContext) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		pipelineVertexFormat = format;
		pass = shaderContext.pass;
	}

	public final void activate() {
		if (isErrored) {
			return;
		}

		if (activeProgram != this) {
			activeProgram = this;
			activateInner();
		}
	}

	private final void activateInner() {
		if (isErrored) {
			return;
		}

		GL21.glUseProgram(progID);

		if (hasDirty) {
			final int count = uniforms.size();

			for (int i = 0; i < count; i++) {
				uniforms.get(i).upload();
			}

			hasDirty = false;
		}
	}

	public static GlProgram activeProgram() {
		return activeProgram;
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
			bufferAddress = MemoryUtil.memAddress(this.uniformFloatBuffer);
		}

		@Override
		public final void set(Matrix4f matrix) {
			if (unifID == -1) {
				return;
			}

			if(matrix == null || matrix.equals(lastValue)) {
				return;
			}

			lastValue.set(matrix);

			matrix.get(uniformFloatBuffer);

			setDirty();
		}

		@Override
		protected void uploadInner() {
			GL21.glUniformMatrix4fv(unifID, false, uniformFloatBuffer);
		}
	}

	public UniformMatrix4fImpl uniformMatrix4f(String name, UniformRefreshFrequency frequency, Consumer<UniformMatrix4f> initializer) {
		final UniformMatrix4fImpl result = new UniformMatrix4fImpl(name, initializer, frequency);

		if (containsUniformSpec("mat4", name)) {
			addUniform(result);
		}

		return result;
	}

	public UniformMatrix4fImpl uniformMatrix4f(String name, UniformRefreshFrequency frequency, FloatBuffer floatBuffer, Consumer<UniformMatrix4f> initializer) {
		final UniformMatrix4fImpl result = new UniformMatrix4fImpl(name, initializer, frequency);

		if (containsUniformSpec("mat4", name)) {
			addUniform(result);
		}

		return result;
	}

	public final void load() {
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
			final int limit = uniforms.size();
			for (int i = 0; i < limit; i++) {
				uniforms.get(i).load(progID);
			}
		}

	}

	public final void unload() {
		if  (progID > 0) {
			GL21.glDeleteProgram(progID);
			progID = -1;
		}
	}

	/**
	 * Return true on success
	 */
	private final boolean loadInner() {
		final int programID = progID;
		if (programID <= 0) {
			return false;
		}

		final int vertId = vertexShader.glId();
		if (vertId <= 0) {
			return false;
		}

		final int fragId = fragmentShader.glId();
		if (fragId <= 0) {
			return false;
		}

		GL21.glAttachShader(programID, vertId);
		GL21.glAttachShader(programID, fragId);

		pipelineVertexFormat.bindProgramAttributes(programID);

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

	public boolean containsUniformSpec(String type, String name) {
		final String regex = "(?m)^\\s*uniform\\s+" + type + "\\s+" + name + "\\s*;";
		final Pattern pattern = Pattern.compile(regex);
		return pattern.matcher(vertexShader.getSource()).find()
				|| pattern.matcher(fragmentShader.getSource()).find();
	}
}
