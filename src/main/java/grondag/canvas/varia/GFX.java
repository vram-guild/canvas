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

package grondag.canvas.varia;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL46C;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.pipeline.GlSymbolLookup;

public class GFX extends GL46C {
	public static boolean checkError() {
		return checkError(null);
	}

	public static boolean checkError(String message) {
		if (!RenderSystem.isOnRenderThread()) {
			throw new IllegalStateException("GFX called outside render thread.");
		}

		final int error = glGetError();

		if (Configurator.logGlStateChanges && message != null) {
			CanvasMod.LOG.info("GFX: " + message);
		}

		if (error == 0) {
			return true;
		} else {
			if (message == null) {
				message = "unknown";
			}

			CanvasMod.LOG.warn(String.format("OpenGL Error: %s (%d) in %s", GlSymbolLookup.reverseLookup(error), error, message));
			// WIP2: put back to false?
			return true;
		}
	}

	public static void disableVertexAttribArray(int index) {
		glDisableVertexAttribArray(index);
		assert checkError(String.format("glDisableVertexAttribArray(%d)", index));
	}

	public static void enableVertexAttribArray(int index) {
		glEnableVertexAttribArray(index);
		assert checkError(String.format("glEnableVertexAttribArray(%d)", index));
	}

	public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
		glVertexAttribPointer(index, size, type, normalized, stride, pointer);
		assert checkError("glVertexAttribPointer");
	}

	public static void bindAttribLocation(int program, int index, CharSequence name) {
		glBindAttribLocation(program, index, name);
		assert checkError(String.format("glBindAttribLocation(%d, %d, %s)", program, index, name));
	}

	public static int getAttribLocation(int program, CharSequence name) {
		final int result = glGetAttribLocation(program, name);
		assert checkError(String.format("glGetAttribLocation(%d, %s)", program, name));
		return result;
	}

	public static String getProgramInfoLog(int program) {
		final String result = glGetProgramInfoLog(program, glGetProgrami(program, GL_INFO_LOG_LENGTH));
		assert checkError(String.format("glGetProgramInfoLog(%d)", program));
		return result;
	}

	public static String getShaderInfoLog(int shader) {
		final String result = glGetShaderInfoLog(shader, glGetShaderi(shader, GL_INFO_LOG_LENGTH));
		assert checkError(String.format("glGetShaderInfoLog(%d)", shader));
		return result;
	}

	public static int getProgramInfo(int program, int paramName) {
		final int result = glGetProgrami(program, paramName);
		assert checkError(String.format("glGetProgrami(%d, %s)", program, GlSymbolLookup.reverseLookup(paramName)));
		return result;
	}

	public static void clearDepth(double depth) {
		glClearDepth(depth);
		assert checkError("clearDepth");
	}

	public static void clear(int mask, boolean getError) {
		glClear(mask);

		if (getError) {
			glGetError();
		}
	}

	public static void clearColor(float red, float green, float blue, float alpha) {
		glClearColor(red, green, blue, alpha);
		assert checkError("clearColor");
	}

	public static int getUniformLocation(int program, CharSequence name) {
		final int result = glGetUniformLocation(program, name);
		assert checkError(String.format("glGetUniformLocation(%d, %s)", program, name));
		return result;
	}

	public static void cullFace(int mode) {
		glCullFace(mode);
		assert checkError(String.format("glCullFace(%s)", GlSymbolLookup.reverseLookup(mode)));
	}

	public static void polygonOffset(float factor, float units) {
		glPolygonOffset(factor, units);
		assert checkError(String.format("glPolygonOffset(%f, %f)", factor, units));
	}

	public static void disable(int target) {
		glDisable(target);
		assert checkError(String.format("glDisable(%s)", GlSymbolLookup.reverseLookup(target)));
	}

	public static void enable(int target) {
		glEnable(target);
		assert checkError(String.format("glEnable(%s)", GlSymbolLookup.reverseLookup(target)));
	}

	public static void bindBuffer(int target, int buffer) {
		glBindBuffer(target, buffer);
		assert checkError(String.format("glBindBuffer(%s, %d)", GlSymbolLookup.reverseLookup(target), buffer));
	}

	public static int genFramebuffer() {
		final int result = glGenFramebuffers();
		assert checkError("genFramebuffer");
		return result;
	}

	public static int checkFramebufferStatus(int target) {
		final int result = glCheckFramebufferStatus(target);
		assert checkError(String.format("glCheckFramebufferStatus(%s)", GlSymbolLookup.reverseLookup(target)));
		return result;
	}

	public static void bindFramebuffer(int target, int buffer) {
		glBindFramebuffer(target, buffer);
		assert checkError(String.format("glBindFramebuffer(%s, %d)", GlSymbolLookup.reverseLookup(target), buffer));
	}

	public static void deleteFramebuffer(int buffer) {
		glDeleteFramebuffers(buffer);
		assert checkError(String.format("glDeleteFramebuffers(%d)", buffer));
	}

	public static void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
		glFramebufferTexture2D(target, attachment, textarget, texture, level);
		assert checkError(String.format("glFramebufferTexture2D(%s, %s, %s, %d, %d)",
				GlSymbolLookup.reverseLookup(target),
				GlSymbolLookup.reverseLookup(attachment),
				GlSymbolLookup.reverseLookup(textarget), texture, level));
	}

	public static void framebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {
		glFramebufferTextureLayer(target, attachment, texture, level, layer);
		assert checkError(String.format("glFramebufferTextureLayer(%s, %s, %d, %d, %d)",
				GlSymbolLookup.reverseLookup(target), GlSymbolLookup.reverseLookup(attachment),
				texture, level, layer));
	}

	public static void drawBuffer(int buffer) {
		glDrawBuffer(buffer);
		assert checkError(String.format("glDrawBuffer(%s)", GlSymbolLookup.reverseLookup(buffer)));
	}

	public static void readBuffer(int buffer) {
		glReadBuffer(buffer);
		assert checkError(String.format("glReadBuffer(%s)", GlSymbolLookup.reverseLookup(buffer)));
	}

	public static void drawBuffers(int buffer) {
		glDrawBuffers(buffer);
		assert checkError(String.format("glDrawBuffers(%s)", GlSymbolLookup.reverseLookup(buffer)));
	}

	public static void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
		glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
		assert checkError(String.format("glBlitFramebuffer(%d, %d, %d, %d, %d, %d, %d, %d, %d, %d)", srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter));
	}

	public static void genBuffers(IntBuffer buffers) {
		glGenBuffers(buffers);
		assert checkError("glGenBuffers");
	}

	public static int genBuffer() {
		final int result = glGenBuffers();
		assert checkError("glGenBuffers");
		return result;
	}

	public static int genVertexArray() {
		final int result = glGenVertexArrays();
		assert checkError("glGenVertexArrays");
		return result;
	}

	public static void genVertexArrays(IntBuffer buff) {
		glGenVertexArrays(buff);
		assert checkError("glGenVertexArrays");
	}

	public static void deleteVertexArray(int array) {
		glDeleteVertexArrays(array);
		assert checkError(String.format("glDeleteVertexArrays(%d)", array));
	}

	public static void deleteBuffers(int buffer) {
		glDeleteBuffers(buffer);
		assert checkError(String.format("glDeleteBuffers(%d)", buffer));
	}

	public static void bufferData(int target, ByteBuffer buffer, int usage) {
		glBufferData(target, buffer, usage);
		assert checkError(String.format("glBufferData(%s, %d)", GlSymbolLookup.reverseLookup(target), usage));
	}

	public static void bufferData(int target, long size, int usage) {
		glBufferData(target, size, usage);
		assert checkError(String.format("glBufferData(%s, %d, %d)", GlSymbolLookup.reverseLookup(target), size, usage));
	}

	public static void bindVertexArray(int array) {
		glBindVertexArray(array);
		assert checkError(String.format("glBindVertexArray(%d)", array));
	}

	public static void bindTexture(int target, int texture) {
		glBindTexture(target, texture);
		assert checkError(String.format("glBindTexture(%s, %d)", GlSymbolLookup.reverseLookup(target), texture));
	}

	public static void deleteTexture(int texture) {
		glDeleteTextures(texture);
		assert checkError(String.format("glDeleteTextures(%d)", texture));
	}

	public static void activeTexture(int texture) {
		glActiveTexture(texture);
		assert checkError(String.format("glActiveTexture(%d)", texture));
	}

	public static void texParameter(int target, int pname, int param) {
		glTexParameteri(target, pname, param);
		assert checkError(String.format("glTexParameteri(%s, %s, %s)",
				GlSymbolLookup.reverseLookup(target), GlSymbolLookup.reverseLookup(pname), GlSymbolLookup.reverseLookup(param)));
	}

	public static void texParameter(int target, int pname, float param) {
		glTexParameterf(target, pname, param);
		assert checkError(String.format("glTexParameteri(%s, %s, %f)",
				GlSymbolLookup.reverseLookup(target), GlSymbolLookup.reverseLookup(pname), param));
	}

	public static void pixelStore(int pname, int param) {
		glPixelStorei(pname, param);
		assert checkError(String.format("glPixelStorei(%s, %d)",
				GlSymbolLookup.reverseLookup(pname), param));
	}

	public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable IntBuffer pixels) {
		glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
		assert checkError(String.format("glTexImage2D(%s, %d, %s, %d, %d, %d, %s, %s)",
				GlSymbolLookup.reverseLookup(target), level,
				GlSymbolLookup.reverseLookup(internalFormat), width, height, border,
				GlSymbolLookup.reverseLookup(format), GlSymbolLookup.reverseLookup(type)));
	}

	public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
		glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
		assert checkError(String.format("glTexImage2D(%s, %d, %s, %d, %d, %d, %s, %s)",
				GlSymbolLookup.reverseLookup(target), level,
				GlSymbolLookup.reverseLookup(internalFormat), width, height, border,
				GlSymbolLookup.reverseLookup(format), GlSymbolLookup.reverseLookup(type)));
	}

	public static void texImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type, @Nullable ByteBuffer pixels) {
		glTexImage3D(target, level, internalFormat, width, height, depth, border, format, type, pixels);
		assert checkError(String.format("glTexImage3D(%s, %d, %s, %d, %d, %d, %d, %s, %s)",
				GlSymbolLookup.reverseLookup(target), level,
				GlSymbolLookup.reverseLookup(internalFormat), width, height, depth, border,
				GlSymbolLookup.reverseLookup(format), GlSymbolLookup.reverseLookup(type)));
	}

	public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, long pixels) {
		glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
		assert checkError(String.format("glTexImage2D(%s, %d, %s, %d, %d, %d, %s, %s)",
				GlSymbolLookup.reverseLookup(target), level,
				GlSymbolLookup.reverseLookup(internalFormat), width, height, border,
				GlSymbolLookup.reverseLookup(format), GlSymbolLookup.reverseLookup(type)));
	}

	public static void texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
		glTexSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
		assert checkError(String.format("glTexSubImage2D(%s, %d, %d, %d, %d, %d, %s, %s)",
				GlSymbolLookup.reverseLookup(target), level, offsetX, offsetY, width, height,
				GlSymbolLookup.reverseLookup(format), GlSymbolLookup.reverseLookup(type)));
	}

	private static boolean maskRed = true, maskGreen = true, maskBlue = true, maskAlpha = true;

	public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		if (red != maskRed || green != maskGreen || blue != maskBlue || alpha != maskAlpha) {
			maskRed = red;
			maskGreen = green;
			maskBlue = blue;
			maskAlpha = alpha;
			glColorMask(red, green, blue, alpha);
			assert checkError("glColorMask");
		}
	}

	private static boolean depthTest;

	public static void disableDepthTest() {
		if (depthTest) {
			disable(GL_DEPTH_TEST);
			depthTest = false;
		}
	}

	public static void enableDepthTest() {
		if (!depthTest) {
			enable(GL_DEPTH_TEST);
			depthTest = true;
		}
	}

	private static int depthFunc = GL_LESS;

	public static void depthFunc(int func) {
		if (func != depthFunc) {
			depthFunc = func;
			glDepthFunc(func);
			assert checkError("glDepthFunc");
		}
	}

	private static boolean depthMask = true;

	public static void depthMask(boolean mask) {
		if (mask != depthMask) {
			depthMask = mask;
			glDepthMask(mask);
			assert checkError("glDepthMask");
		}
	}

	private static boolean blend = false;

	public static void enableBlend() {
		if (!blend) {
			enable(GL_BLEND);
			blend = true;
		}
	}

	public static void disableBlend() {
		if (blend) {
			disable(GL_BLEND);
			blend = false;
		}
	}

	public static void defaultBlendFunc() {
		blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
	}

	private static int _srcFactorRGB, _dstFactorRGB, _srcFactorAlpha, _dstFactorAlpha;

	public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
		if (srcFactorRGB != _srcFactorRGB || dstFactorRGB != _dstFactorRGB || srcFactorAlpha != _srcFactorAlpha || dstFactorAlpha != _dstFactorAlpha) {
			_srcFactorRGB = srcFactorRGB;
			_dstFactorRGB = dstFactorRGB;
			_srcFactorAlpha = srcFactorAlpha;
			_dstFactorAlpha = dstFactorAlpha;
			glBlendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
		}

		assert checkError("glBlendFuncSeparate");
	}

	public static void blendFunc(int srcFactor, int dstFactor) {
		if (srcFactor != _srcFactorRGB || dstFactor != _dstFactorRGB) {
			_srcFactorRGB = srcFactor;
			_dstFactorRGB = dstFactor;
			glBlendFunc(srcFactor, dstFactor);
		}

		assert checkError("glBlendFunc");
	}

	private static boolean cull = false;

	public static void enableCull() {
		if (!cull) {
			enable(GL_CULL_FACE);
			cull = true;
		}
	}

	public static void disableCull() {
		if (cull) {
			disable(GL_CULL_FACE);
			cull = false;
		}
	}

	public static void backupProjectionMatrix() {
		RenderSystem.backupProjectionMatrix();
	}

	public static void restoreProjectionMatrix() {
		RenderSystem.restoreProjectionMatrix();
	}

	public static void viewport(int x, int y, int width, int height) {
		glViewport(x, y, width, height);
		assert checkError(String.format("glViewport(%d, %d, %d, %d)", x, y, width, height));
	}

	public static void deleteProgram(int program) {
		glDeleteProgram(program);
		assert checkError(String.format("glDeleteProgram(%d)", program));
	}

	public static int createProgram() {
		final int result = glCreateProgram();
		assert checkError("glCreateProgram");
		return result;
	}

	public static void useProgram(int program) {
		glUseProgram(program);
		assert checkError(String.format("glUseProgram(%d)", program));
	}

	public static void linkProgram(int program) {
		glLinkProgram(program);
		assert checkError(String.format("glLinkProgram(%d)", program));
	}

	public static void uniform1fv(int location, FloatBuffer value) {
		glUniform1fv(location, value);
		assert checkError(String.format("glUniform1fv(%d)", location));
	}

	public static void uniform2fv(int location, FloatBuffer value) {
		glUniform2fv(location, value);
		assert checkError(String.format("glUniform2fv(%d)", location));
	}

	public static void uniform3fv(int location, FloatBuffer value) {
		glUniform3fv(location, value);
		assert checkError(String.format("glUniform3fv(%d)", location));
	}

	public static void uniform4fv(int location, FloatBuffer value) {
		glUniform4fv(location, value);
		assert checkError(String.format("glUniform4fv(%d)", location));
	}

	public static void uniform1iv(int location, IntBuffer value) {
		glUniform1iv(location, value);
		assert checkError(String.format("glUniform1iv(%d)", location));
	}

	public static void uniform1i(int location, int value) {
		glUniform1i(location, value);
		assert checkError(String.format("glUniform1i(%d)", location));
	}

	public static void uniform2iv(int location, IntBuffer value) {
		glUniform2iv(location, value);
		assert checkError(String.format("glUniform2iv(%d)", location));
	}

	public static void uniform3iv(int location, IntBuffer value) {
		glUniform3iv(location, value);
		assert checkError(String.format("glUniform3iv(%d)", location));
	}

	public static void uniform4iv(int location, IntBuffer value) {
		glUniform4iv(location, value);
		assert checkError(String.format("glUniform4iv(%d)", location));
	}

	public static void uniform1uiv(int location, IntBuffer value) {
		glUniform1uiv(location, value);
		assert checkError(String.format("glUniform1uiv(%d)", location));
	}

	public static void uniform2uiv(int location, IntBuffer value) {
		glUniform2uiv(location, value);
		assert checkError(String.format("glUniform2uiv(%d)", location));
	}

	public static void uniform3uiv(int location, IntBuffer value) {
		glUniform3uiv(location, value);
		assert checkError(String.format("glUniform3uiv(%d)", location));
	}

	public static void uniform4uiv(int location, IntBuffer value) {
		glUniform4uiv(location, value);
		assert checkError(String.format("glUniform4uiv(%d)", location));
	}

	public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
		glUniformMatrix4fv(location, transpose, value);
		assert checkError(String.format("glUniformMatrix4fv(%d)", location));
	}

	public static void uniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
		glUniformMatrix2fv(location, transpose, value);
		assert checkError(String.format("glUniformMatrix2fv(%d)", location));
	}

	public static void uniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
		glUniformMatrix3fv(location, transpose, value);
		assert checkError(String.format("glUniformMatrix3fv(%d)", location));
	}

	public static void drawArrays(int mode, int first, int count) {
		glDrawArrays(mode, first, count);
		assert checkError(String.format("glDrawArrays(%s, %d, %d)", GlSymbolLookup.reverseLookup(mode), first, count));
	}

	public static void drawElements(int mode, int count, int type, long indices) {
		glDrawElements(mode, count, type, indices);
		assert checkError(String.format("glDrawElements(%s, %d, %s, %d)",
				GlSymbolLookup.reverseLookup(mode), count, GlSymbolLookup.reverseLookup(type), indices));
	}

	public static void drawElementsBaseVertex(int mode, int count, int type, long indices, int baseVertex) {
		glDrawElementsBaseVertex(mode, count, type, indices, baseVertex);
		assert checkError(String.format("glDrawElementsBaseVertex(%s, %d, %s, %d, %d)",
				GlSymbolLookup.reverseLookup(mode), count, GlSymbolLookup.reverseLookup(type), indices, baseVertex));
	}

	private static boolean scissorTest = false;
	public static void disableScissorTest() {
		if (scissorTest) {
			disable(GL_SCISSOR_TEST);
			scissorTest = false;
		}
	}

	public static void enableScissorTest() {
		if (!scissorTest) {
			enable(GL_SCISSOR_TEST);
			scissorTest = true;
		}
	}

	public static void scissor(int x, int y, int width, int height) {
		glScissor(x, y, width, height);
		assert checkError("glScissor");
	}

	public static void blendEquation(int mode) {
		glBlendEquation(mode);
		assert checkError(String.format("glDeleteProgram(%s)", GlSymbolLookup.reverseLookup(mode)));
	}

	public static int getProgrami(int program, int pname) {
		final int result = glGetProgrami(program, pname);
		assert checkError("glGetProgrami");
		return result;
	}

	public static void attachShader(int program, int shader) {
		glAttachShader(program, shader);
		assert checkError("glAttachShader");
	}

	public static void deleteShader(int shader) {
		glDeleteShader(shader);
		assert checkError("glDeleteShader");
	}

	public static int createShader(int type) {
		final int result = glCreateShader(type);
		assert checkError("glCreateShader");
		return result;
	}

	public static void shaderSource(int shader, CharSequence[] source) {
		glShaderSource(shader, source);
		assert checkError("glShaderSource");
	}

	public static void compileShader(int shader) {
		glCompileShader(shader);
		assert checkError("glCompileShader");
	}

	public static int getShader(int shader, int pname) {
		final int result = glGetShaderi(shader, pname);
		assert checkError("glGetShaderi");
		return result;
	}

	public static @Nullable ByteBuffer mapBuffer(int target, int access) {
		final ByteBuffer result = glMapBuffer(target, access);
		assert checkError("glMapBuffer");
		return result;
	}

	public static void unmapBuffer(int target) {
		glUnmapBuffer(target);
		assert checkError("unmapBuffer");
	}

	public static void copyTexSubImage2D(int i, int j, int k, int l, int m, int n, int o, int p) {
		glCopyTexSubImage2D(i, j, k, l, m, n, o, p);
		assert checkError("glCopyTexSubImage2D");
	}

	public static void bindRenderbuffer(int target, int renderBuffer) {
		glBindRenderbuffer(target, renderBuffer);
		assert checkError("glBindRenderbuffer");
	}

	public static void deleteRenderbuffer(int buffer) {
		glDeleteRenderbuffers(buffer);
		assert checkError("glDeleteRenderbuffers");
	}

	public static int genRenderbuffer() {
		final int result = glGenRenderbuffers();
		assert checkError("glGenRenderbuffers");
		return result;
	}

	public static void renderbufferStorage(int target, int internalformat, int width, int height) {
		glRenderbufferStorage(target, internalformat, width, height);
		assert checkError("glRenderbufferStorage");
	}

	public static void framebufferRenderbuffer(int i, int j, int k, int l) {
		glFramebufferRenderbuffer(i, j, k, l);
		assert checkError("glFramebufferRenderbuffer");
	}

	public static int getInteger(int pname) {
		final int result = glGetInteger(pname);
		assert checkError("glGetInteger");
		return result;
	}

	public static String getShaderInfoLog(int shader, int maxLength) {
		final String result = glGetShaderInfoLog(shader, maxLength);
		assert checkError("glGetShaderInfoLog");
		return result;
	}

	public static String getProgramInfoLog(int program, int maxLength) {
		final String result = glGetProgramInfoLog(program, maxLength);
		assert checkError("glGetProgramInfoLog");
		return result;
	}

	public static void polygonMode(int face, int mode) {
		glPolygonMode(face, mode);
		assert checkError("glPolygonMode");
	}

	private static boolean polygonOffset = false;

	public static void enablePolygonOffset() {
		if (!polygonOffset) {
			enable(GL_POLYGON_OFFSET_FILL);
			polygonOffset = true;
		}
	}

	public static void disablePolygonOffset() {
		if (polygonOffset) {
			disable(GL_POLYGON_OFFSET_FILL);
			polygonOffset = false;
		}
	}

	private static boolean colorLogic = false;

	public static void enableColorLogicOp() {
		if (!colorLogic) {
			enable(GL_COLOR_LOGIC_OP);
			colorLogic = true;
		}
	}

	public static void disableColorLogicOp() {
		if (colorLogic) {
			disable(GL_COLOR_LOGIC_OP);
			colorLogic = false;
		}
	}

	private static int logicOp = GL_COPY;

	public static void logicOp(int op) {
		if (op != logicOp) {
			logicOp = op;
			glLogicOp(op);
			assert checkError("glLogicOp");
		}
	}

	public static int getTexLevelParameter(int target, int level, int pname) {
		final int result = glGetTexLevelParameteri(target, level, pname);
		assert checkError("glGetTexLevelParameteri");
		return result;
	}

	public static int genTexture() {
		final int result = glGenTextures();
		assert checkError("glGenTextures");
		return result;
	}

	public static void genTextures(int[] is) {
		glGenTextures(is);
		checkError("glGenTextures");
	}

	public static void getTexImage(int target, int level, int format, int type, long pixels) {
		glGetTexImage(target, level, format, type, pixels);
		checkError("glGetTexImage");
	}

	private static int stencilFunc, stencilRef, stencilMask;

	public static void stencilFunc(int func, int ref, int mask) {
		if (func != stencilFunc || ref != stencilRef || mask != stencilMask) {
			stencilFunc = func;
			stencilRef = ref;
			stencilMask = mask;
			glStencilFunc(func, ref, mask);
			checkError("glStencilFunc");
		}
	}

	public static void stencilMask(int mask) {
		if (mask != stencilMask) {
			stencilMask = mask;
			glStencilMask(mask);
			checkError("glStencilMask");
		}
	}

	private static int stencil_sfail, stencil_dpfail, stencil_dppass;

	public static void stencilOp(int sfail, int dpfail, int dppass) {
		if (sfail != stencil_sfail || dpfail != stencil_dpfail || dppass != stencil_dppass) {
			stencil_sfail = sfail;
			stencil_dpfail = dpfail;
			stencil_dppass = dppass;
			glStencilOp(sfail, dpfail, dppass);
			checkError("glStencilOp");
		}
	}

	public static void clearStencil(int stencil) {
		glClearStencil(stencil);
		checkError("glClearStencil");
	}

	public static void drawPixels(int i, int j, int k, int l, long m) {
		// NON-CORE???
		GL11.glDrawPixels(i, j, k, l, m);
		checkError("glDrawPixels");
	}

	public static void vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
		glVertexAttribIPointer(index, size, type, stride, pointer);
		checkError("vertexAttribIPointer");
	}

	public static void readPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
		glReadPixels(x, y, width, height, format, type, pixels);
		checkError("glReadPixels");
	}

	public static void readPixels(int i, int j, int k, int l, int m, int n, long o) {
		glReadPixels(i, j, k, l, m, n, o);
		checkError("glReadPixels");
	}

	public static int getError() {
		return glGetError();
	}

	public static String getString(int name) {
		final String result = glGetString(name);
		checkError("glGetString");
		return result;
	}
}
