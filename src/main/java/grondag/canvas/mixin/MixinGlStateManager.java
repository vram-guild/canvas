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

package grondag.canvas.mixin;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.canvas.render.CanvasTextureState;
import grondag.canvas.varia.GFX;

@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {
	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disableScissorTest() {
		GFX.disableScissorTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enableScissorTest() {
		GFX.enableScissorTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _scissorBox(int x, int y, int width, int height) {
		GFX.scissor(x, y, width, height);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disableDepthTest() {
		GFX.disableDepthTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enableDepthTest() {
		GFX.enableDepthTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _depthFunc(int func) {
		GFX.depthFunc(func);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _depthMask(boolean mask) {
		GFX.depthMask(mask);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disableBlend() {
		GFX.disableBlend();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enableBlend() {
		GFX.enableBlend();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _blendFunc(int srcFactor, int dstFactor) {
		GFX.blendFunc(srcFactor, dstFactor);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
		GFX.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _blendEquation(int mode) {
		GFX.blendEquation(mode);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int glGetProgrami(int program, int pname) {
		return GFX.getProgrami(program, pname);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void glAttachShader(int program, int shader) {
		GFX.attachShader(program, shader);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void glDeleteShader(int shader) {
		GFX.deleteShader(shader);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int glCreateShader(int type) {
		return GFX.createShader(type);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void glShaderSource(int shader, List<String> strings) {
		GFX.shaderSource(shader, strings.toArray(new CharSequence[0]));
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void glCompileShader(int shader) {
		GFX.compileShader(shader);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int glGetShaderi(int shader, int pname) {
		return GFX.getShader(shader, pname);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUseProgram(int program) {
		GFX.useProgram(program);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int glCreateProgram() {
		return GFX.createProgram();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void glDeleteProgram(int program) {
		GFX.deleteProgram(program);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void glLinkProgram(int program) {
		GFX.linkProgram(program);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _glGetUniformLocation(int program, CharSequence name) {
		return GFX.getUniformLocation(program, name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform1(int location, IntBuffer value) {
		GFX.uniform1iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform1i(int location, int value) {
		GFX.uniform1i(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform1(int location, FloatBuffer value) {
		GFX.uniform1fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform2(int location, IntBuffer value) {
		GFX.uniform2iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform2(int location, FloatBuffer value) {
		GFX.uniform2fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform3(int location, IntBuffer value) {
		GFX.uniform3iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform3(int location, FloatBuffer value) {
		GFX.uniform3fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform4(int location, IntBuffer value) {
		GFX.uniform4iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniform4(int location, FloatBuffer value) {
		GFX.uniform4fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniformMatrix2(int location, boolean transpose, FloatBuffer value) {
		GFX.uniformMatrix2fv(location, transpose, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
		GFX.uniformMatrix3fv(location, transpose, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
		GFX.uniformMatrix4fv(location, transpose, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _glGetAttribLocation(int program, CharSequence name) {
		return GFX.getAttribLocation(program, name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBindAttribLocation(int program, int index, CharSequence name) {
		GFX.bindAttribLocation(program, index, name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _glGenBuffers() {
		return GFX.genBuffer();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _glGenVertexArrays() {
		return GFX.genVertexArray();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBindBuffer(int target, int buffer) {
		GFX.bindBuffer(target, buffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBindVertexArray(int array) {
		GFX.bindVertexArray(array);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBufferData(int target, ByteBuffer data, int usage) {
		GFX.bufferData(target, data, usage);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBufferData(int target, long size, int usage) {
		GFX.bufferData(target, size, usage);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	@Nullable
	public static ByteBuffer mapBuffer(int target, int access) {
		return GFX.mapBuffer(target, access);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glUnmapBuffer(int target) {
		GFX.unmapBuffer(target);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glDeleteBuffers(int buffer) {
		GFX.deleteBuffers(buffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glCopyTexSubImage2D(int i, int j, int k, int l, int m, int n, int o, int p) {
		GFX.copyTexSubImage2D(i, j, k, l, m, n, o, p);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glDeleteVertexArrays(int array) {
		GFX.deleteVertexArray(array);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBindFramebuffer(int target, int framebuffer) {
		GFX.bindFramebuffer(target, framebuffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
		GFX.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glBindRenderbuffer(int i, int j) {
		GFX.bindRenderbuffer(i, j);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glDeleteRenderbuffers(int i) {
		GFX.deleteRenderbuffer(i);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glDeleteFramebuffers(int framebuffer) {
		GFX.deleteFramebuffer(framebuffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int glGenFramebuffers() {
		return GFX.genFramebuffer();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int glGenRenderbuffers() {
		return GFX.genRenderbuffer();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glRenderbufferStorage(int i, int j, int k, int l) {
		GFX.renderbufferStorage(i, j, k, l);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glFramebufferRenderbuffer(int i, int j, int k, int l) {
		GFX.framebufferRenderbuffer(i, j, k, l);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int glCheckFramebufferStatus(int target) {
		return GFX.checkFramebufferStatus(target);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glFramebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
		GFX.framebufferTexture2D(target, attachment, textureTarget, texture, level);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int getBoundFramebuffer() {
		return GFX.getInteger(GFX.GL_DRAW_FRAMEBUFFER_BINDING);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite(remap = false)
	public static void glActiveTexture(int textureUnit) {
		CanvasTextureState.activeTextureUnit(textureUnit);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void glBlendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
		GFX.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static String glGetShaderInfoLog(int shader, int maxLength) {
		return GFX.getShaderInfoLog(shader, maxLength);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static String glGetProgramInfoLog(int program, int maxLength) {
		return GFX.getProgramInfoLog(program, maxLength);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enableCull() {
		GFX.enableCull();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disableCull() {
		GFX.disableCull();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _polygonMode(int face, int mode) {
		GFX.polygonMode(face, mode);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enablePolygonOffset() {
		GFX.enablePolygonOffset();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disablePolygonOffset() {
		GFX.disablePolygonOffset();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _polygonOffset(float factor, float units) {
		GFX.polygonOffset(factor, units);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enableColorLogicOp() {
		GFX.enableColorLogicOp();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disableColorLogicOp() {
		GFX.disableColorLogicOp();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _logicOp(int op) {
		GFX.logicOp(op);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _activeTexture(int textureUnit) {
		CanvasTextureState.activeTextureUnit(textureUnit);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enableTexture() {
		// NOOP
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disableTexture() {
		// NOOP
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _texParameter(int target, int pname, float param) {
		GFX.texParameter(target, pname, param);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _texParameter(int target, int pname, int param) {
		GFX.texParameter(target, pname, param);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _getTexLevelParameter(int target, int level, int pname) {
		return GFX.getTexLevelParameter(target, level, pname);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _genTexture() {
		return GFX.genTexture();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _genTextures(int[] is) {
		GFX.genTextures(is);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite(remap = false)
	public static void _deleteTexture(int texture) {
		CanvasTextureState.deleteTexture(texture);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _deleteTextures(int[] is) {
		CanvasTextureState.deleteTextures(is);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite(remap = false)
	public static void _bindTexture(int texture) {
		CanvasTextureState.bindTexture(GFX.GL_TEXTURE_2D, texture);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _getTextureId(int i) {
		return CanvasTextureState.getTextureId(i);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite(remap = false)
	public static int _getActiveTexture() {
		return CanvasTextureState.activeTextureUnit();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable IntBuffer pixels) {
		GFX.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
		GFX.texSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _getTexImage(int target, int level, int format, int type, long pixels) {
		GFX.getTexImage(target, level, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _viewport(int x, int y, int width, int height) {
		GFX.viewport(x, y, width, height);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		GFX.colorMask(red, green, blue, alpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _stencilFunc(int func, int ref, int mask) {
		GFX.stencilFunc(func, ref, mask);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _stencilMask(int mask) {
		GFX.stencilMask(mask);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _stencilOp(int sfail, int dpfail, int dppass) {
		GFX.stencilOp(sfail, dpfail, dppass);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _clearDepth(double depth) {
		GFX.clearDepth(depth);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _clearColor(float red, float green, float blue, float alpha) {
		GFX.clearColor(red, green, blue, alpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _clearStencil(int stencil) {
		GFX.clearStencil(stencil);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _clear(int mask, boolean getError) {
		GFX.clear(mask, getError);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _glDrawPixels(int i, int j, int k, int l, long m) {
		GFX.drawPixels(i, j, k, l, m);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
		GFX.vertexAttribPointer(index, size, type, normalized, stride, pointer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
		GFX.vertexAttribIPointer(index, size, type, stride, pointer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _enableVertexAttribArray(int index) {
		GFX.enableVertexAttribArray(index);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _disableVertexAttribArray(int index) {
		GFX.disableVertexAttribArray(index);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _drawElements(int mode, int first, int type, long indices) {
		GFX.drawElements(mode, first, type, indices);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _pixelStore(int pname, int param) {
		GFX.pixelStore(pname, param);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _readPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
		GFX.readPixels(x, y, width, height, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static void _readPixels(int i, int j, int k, int l, int m, int n, long o) {
		GFX.readPixels(i, j, k, l, m, n, o);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _getError() {
		return GFX.getError();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static String _getString(int name) {
		return GFX.getString(name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite(remap = false)
	public static int _getInteger(int pname) {
		return GFX.getInteger(pname);
	}
}
