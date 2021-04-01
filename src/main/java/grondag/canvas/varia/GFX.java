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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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

		if ((error != 0 || Configurator.logGlStateChanges) && message != null) {
			CanvasMod.LOG.info("GFX: " + message);
		}

		if (error == 0) {
			return true;
		} else {
			CanvasMod.LOG.warn(String.format("OpenGL Error: %s (%d)", GlSymbolLookup.reverseLookup(error), error));
			//WIP2: put back to false
			return true;
		}
	}

	// WIP2: remove pre-checks

	public static void disableVertexAttribArray(int index) {
		assert checkError("PRE disableVertexAttribArray");
		glDisableVertexAttribArray(index);
		assert checkError(String.format("glDisableVertexAttribArray(%d)", index));
	}

	public static void enableVertexAttribArray(int index) {
		assert checkError("PRE enableVertexAttribArray");
		glEnableVertexAttribArray(index);
		assert checkError(String.format("glEnableVertexAttribArray(%d)", index));
	}

	public static String getProgramInfoLog(int program) {
		assert checkError("PRE getProgramInfoLog");
		final String result = glGetProgramInfoLog(program, glGetProgrami(program, GL_INFO_LOG_LENGTH));
		assert checkError(String.format("glGetProgramInfoLog(%d)", program));
		return result;
	}

	public static String getShaderInfoLog(int shader) {
		assert checkError("PRE getShaderInfoLog");
		final String result = glGetShaderInfoLog(shader, glGetShaderi(shader, GL_INFO_LOG_LENGTH));
		assert checkError(String.format("glGetShaderInfoLog(%d)", shader));
		return result;
	}

	public static int getProgramInfo(int program, int paramName) {
		assert checkError("PRE getProgramInfo");
		final int result = glGetProgrami(program, paramName);
		assert checkError(String.format("glGetProgrami(%d, %s)", program, GlSymbolLookup.reverseLookup(paramName)));
		return result;
	}

	public static int getUniformLocation(int program, String name) {
		assert checkError("PRE getUniformLocation");
		final int result = glGetUniformLocation(program, name);
		assert checkError(String.format("glGetUniformLocation(%d, %s)", program, name));
		return result;
	}

	public static void bindBuffer(int target, int buffer) {
		assert checkError("PRE bindBuffer");
		glBindBuffer(target, buffer);
		assert checkError(String.format("glBindBuffer(%s, %d)", GlSymbolLookup.reverseLookup(target), buffer));
	}

	public static void genBuffers(IntBuffer buffers) {
		assert checkError("PRE genBuffers");
		glGenBuffers(buffers);
		assert checkError("glGenBuffers");
	}

	public static void deleteBuffers(int buffer) {
		assert checkError("PRE deleteBuffers");
		glDeleteBuffers(buffer);
		assert checkError(String.format("glDeleteBuffers(%d)", buffer));
	}

	public static void bufferData(int target, ByteBuffer buffer, int usage) {
		assert checkError("PRE bufferData");
		glBufferData(target, buffer, usage);
		assert checkError(String.format("glBufferData(%s, %d)", GlSymbolLookup.reverseLookup(target), usage));
	}

	public static void bindVertexArray(int array) {
		assert checkError("PRE bindVertexArray");
		glBindVertexArray(array);
		assert checkError(String.format("glBindVertexArray(%d)", array));
	}

	public static void bindTexture(int target, int texture) {
		assert checkError("PRE bindTexture");
		glBindTexture(target, texture);
		assert checkError(String.format("glBindTexture(%s, %d)", GlSymbolLookup.reverseLookup(target), texture));
	}

	public static void deleteTexture(int texture) {
		assert checkError("PRE deleteTexture");
		glDeleteTextures(texture);
		assert checkError(String.format("glDeleteTextures(%d)", texture));
	}

	public static void activeTexture(int texture) {
		assert checkError("PRE activeTexture");
		glActiveTexture(texture);
		assert checkError(String.format("glActiveTexture(%d)", texture));
	}

	// PERF: avoid thread checks in GlStateManager outside of dev env

	public static void disableDepthTest() {
		assert checkError("PRE disableDepthTest");
		GlStateManager.disableDepthTest();
		assert checkError("GlStateManager.disableDepthTest()");
	}

	public static void enableDepthTest() {
		assert checkError("PRE enableDepthTest");
		GlStateManager.enableDepthTest();
		assert checkError("GlStateManager.enableDepthTest()");
	}

	public static void depthFunc(int func) {
		assert checkError("PRE depthFunc");
		GlStateManager.depthFunc(func);
		assert checkError(String.format("GlStateManager.depthFunc(%s)", GlSymbolLookup.reverseLookup(func)));
	}

	public static void depthMask(boolean mask) {
		assert checkError("PRE depthMask");
		GlStateManager.depthMask(mask);
		assert checkError(String.format("GlStateManager.depthMask(%s)", Boolean.toString(mask)));
	}

	public static void enableBlend() {
		assert checkError("PRE enableBlend");
		GlStateManager.enableBlend();
		assert checkError("GlStateManager.enableBlend()");
	}

	public static void disableBlend() {
		assert checkError("PRE disableBlend");
		GlStateManager.disableBlend();
		assert checkError("GlStateManager.disableBlend()");
	}

	public static void enableCull() {
		assert checkError("PRE enableCull");
		GlStateManager.enableCull();
		assert checkError("GlStateManager.enableCull()");
	}

	public static void disableCull() {
		assert checkError("PRE disableCull");
		GlStateManager.disableCull();
		assert checkError("GlStateManager.disableCull()");
	}

	public static void backupProjectionMatrix() {
		RenderSystem.backupProjectionMatrix();
	}

	public static void restoreProjectionMatrix() {
		RenderSystem.restoreProjectionMatrix();
	}

	public static void viewport(int x, int y, int width, int height) {
		assert checkError("PRE viewport");
		glViewport(x, y, width, height);
		assert checkError(String.format("glViewport(%d, %d, %d, %d)", x, y, width, height));
	}

	public static void deleteProgram(int program) {
		assert checkError("PRE deleteProgram");
		glDeleteProgram(program);
		assert checkError(String.format("glDeleteProgram(%d)", program));
	}

	public static int createProgram() {
		assert checkError("PRE createProgram");
		final int result = glCreateProgram();
		assert checkError("glCreateProgram");
		return result;
	}

	public static void useProgram(int program) {
		assert checkError("PRE useProgram");
		glUseProgram(program);
		assert checkError(String.format("glUseProgram(%d)", program));
	}

	public static void linkProgram(int program) {
		assert checkError("PRE linkProgram");
		glLinkProgram(program);
		assert checkError(String.format("glLinkProgram(%d)", program));
	}

	public static void uniform1fv(int location, FloatBuffer value) {
		assert checkError("PRE uniform1fv");
		glUniform1fv(location, value);
		assert checkError(String.format("glUniform1fv(%d)", location));
	}

	public static void uniform2fv(int location, FloatBuffer value) {
		assert checkError("PRE uniform2fv");
		glUniform2fv(location, value);
		assert checkError(String.format("glUniform2fv(%d)", location));
	}

	public static void uniform3fv(int location, FloatBuffer value) {
		assert checkError("PRE uniform3fv");
		glUniform3fv(location, value);
		assert checkError(String.format("glUniform3fv(%d)", location));
	}

	public static void uniform4fv(int location, FloatBuffer value) {
		assert checkError("PRE uniform4fv");
		glUniform4fv(location, value);
		assert checkError(String.format("glUniform4fv(%d)", location));
	}

	public static void uniform1iv(int location, IntBuffer value) {
		assert checkError("PRE uniform1iv");
		glUniform1iv(location, value);
		assert checkError(String.format("glUniform1iv(%d)", location));
	}

	public static void uniform2iv(int location, IntBuffer value) {
		assert checkError("PRE uniform2iv");
		glUniform2iv(location, value);
		assert checkError(String.format("glUniform2iv(%d)", location));
	}

	public static void uniform3iv(int location, IntBuffer value) {
		assert checkError("PRE uniform3iv");
		glUniform3iv(location, value);
		assert checkError(String.format("glUniform3iv(%d)", location));
	}

	public static void uniform4iv(int location, IntBuffer value) {
		assert checkError("PRE uniform4iv");
		glUniform4iv(location, value);
		assert checkError(String.format("glUniform4iv(%d)", location));
	}

	public static void uniform1uiv(int location, IntBuffer value) {
		assert checkError("PRE uniform1uiv");
		glUniform1uiv(location, value);
		assert checkError(String.format("glUniform1uiv(%d)", location));
	}

	public static void uniform2uiv(int location, IntBuffer value) {
		assert checkError("PRE uniform2uiv");
		glUniform2uiv(location, value);
		assert checkError(String.format("glUniform2uiv(%d)", location));
	}

	public static void uniform3uiv(int location, IntBuffer value) {
		assert checkError("PRE uniform3uiv");
		glUniform3uiv(location, value);
		assert checkError(String.format("glUniform3uiv(%d)", location));
	}

	public static void uniform4uiv(int location, IntBuffer value) {
		assert checkError("PRE uniform4uiv");
		glUniform4uiv(location, value);
		assert checkError(String.format("glUniform4uiv(%d)", location));
	}

	public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
		assert checkError("PRE uniformMatrix4fv");
		glUniformMatrix4fv(location, transpose, value);
		assert checkError(String.format("glUniformMatrix4fv(%d)", location));
	}

	public static void uniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
		assert checkError("PRE uniformMatrix3fv");
		glUniformMatrix3fv(location, transpose, value);
		assert checkError(String.format("glUniformMatrix3fv(%d)", location));
	}

	public static void drawArrays(int mode, int first, int count) {
		assert checkError("PRE drawArrays");
		glDrawArrays(mode, first, count);
		assert checkError(String.format("glDrawArrays(%s, %d, %d)", GlSymbolLookup.reverseLookup(mode), first, count));
	}

	public static void drawElements(int mode, int count, int type, long indices) {
		assert checkError("PRE drawElements");
		glDrawElements(mode, count, type, indices);
		assert checkError(String.format("glDrawElements(%s, %d, %s, %d)",
				GlSymbolLookup.reverseLookup(mode), count, GlSymbolLookup.reverseLookup(type), indices));
	}

	public static void drawElementsBaseVertex(int mode, int count, int type, long indices, int baseVertex) {
		assert checkError("PRE drawElementsBaseVertex");
		glDrawElementsBaseVertex(mode, count, type, indices, baseVertex);
		assert checkError(String.format("glDrawElementsBaseVertex(%s, %d, %s, %d, %d)",
				GlSymbolLookup.reverseLookup(mode), count, GlSymbolLookup.reverseLookup(type), indices, baseVertex));
	}
}
