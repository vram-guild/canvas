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
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static void activeTexture(int textureUnit) {
		CanvasTextureState.activeTextureUnit(textureUnit);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void activeTextureUntracked(int textureUnit) {
		CanvasTextureState.activeTextureUnit(textureUnit);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _enableTexture() {
		// NOOP
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disableTexture() {
		// NOOP
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void deleteTextures(int[] is) {
		CanvasTextureState.deleteTextures(is);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int _getTextureId(int i) {
		return CanvasTextureState.getTextureId(i);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static void bindTexture(int texture) {
		CanvasTextureState.bindTexture(GFX.GL_TEXTURE_2D, texture);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static int _getActiveTexture() {
		return CanvasTextureState.activeTextureUnit();
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static void deleteTexture(int texture) {
		CanvasTextureState.deleteTexture(texture);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disableScissorTest() {
		GFX.disableScissorTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void enableScissorTest() {
		GFX.enableScissorTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void scissor(int x, int y, int width, int height) {
		GFX.scissor(x, y, width, height);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disableDepthTest() {
		GFX.disableDepthTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void enableDepthTest() {
		GFX.enableDepthTest();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void depthFunc(int func) {
		GFX.depthFunc(func);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void depthMask(boolean mask) {
		GFX.depthMask(mask);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disableBlend() {
		GFX.disableBlend();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void enableBlend() {
		GFX.enableBlend();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void blendFunc(int srcFactor, int dstFactor) {
		GFX.blendFunc(srcFactor, dstFactor);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
		GFX.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void blendEquation(int mode) {
		GFX.blendEquation(mode);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getProgram(int program, int pname) {
		return GFX.getProgrami(program, pname);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void attachShader(int program, int shader) {
		GFX.attachShader(program, shader);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void deleteShader(int shader) {
		GFX.deleteShader(shader);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int createShader(int type) {
		return GFX.createShader(type);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void shaderSource(int shader, List<String> strings) {
		GFX.shaderSource(shader, strings.toArray(new CharSequence[0]));
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void compileShader(int shader) {
		GFX.compileShader(shader);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getShader(int shader, int pname) {
		return GFX.getShader(shader, pname);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void useProgram(int program) {
		GFX.useProgram(program);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int createProgram() {
		return GFX.createProgram();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void deleteProgram(int program) {
		GFX.deleteProgram(program);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void linkProgram(int program) {
		GFX.linkProgram(program);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getUniformLocation(int program, CharSequence name) {
		return GFX.getUniformLocation(program, name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform1(int location, IntBuffer value) {
		GFX.uniform1iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform1(int location, int value) {
		GFX.uniform1i(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform1(int location, FloatBuffer value) {
		GFX.uniform1fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform2(int location, IntBuffer value) {
		GFX.uniform2iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform2(int location, FloatBuffer value) {
		GFX.uniform2fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform3(int location, IntBuffer value) {
		GFX.uniform3iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform3(int location, FloatBuffer value) {
		GFX.uniform3fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform4(int location, IntBuffer value) {
		GFX.uniform4iv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniform4(int location, FloatBuffer value) {
		GFX.uniform4fv(location, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniformMatrix2(int location, boolean transpose, FloatBuffer value) {
		GFX.uniformMatrix2fv(location, transpose, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniformMatrix3(int location, boolean transpose, FloatBuffer value) {
		GFX.uniformMatrix3fv(location, transpose, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void uniformMatrix4(int location, boolean transpose, FloatBuffer value) {
		GFX.uniformMatrix4fv(location, transpose, value);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getAttribLocation(int program, CharSequence name) {
		return GFX.getAttribLocation(program, name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void bindAttribLocation(int program, int index, CharSequence name) {
		GFX.bindAttribLocation(program, index, name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int genBuffer() {
		return GFX.genBuffer();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int genVertexArray() {
		return GFX.genVertexArray();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void bindBuffer(int target, int buffer) {
		GFX.bindBuffer(target, buffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void bindVertexArray(int array) {
		GFX.bindVertexArray(array);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void bufferData(int target, ByteBuffer data, int usage) {
		GFX.bufferData(target, data, usage);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void bufferData(int target, long size, int usage) {
		GFX.bufferData(target, size, usage);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	@Nullable
	public static ByteBuffer mapBuffer(int target, int access) {
		return GFX.mapBuffer(target, access);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void unmapBuffer(int target) {
		GFX.unmapBuffer(target);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void deleteBuffer(int buffer) {
		GFX.deleteBuffers(buffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _glCopyTexSubImage2D(int i, int j, int k, int l, int m, int n, int o, int p) {
		GFX.copyTexSubImage2D(i, j, k, l, m, n, o, p);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void deleteVertexArray(int array) {
		GFX.deleteVertexArray(array);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void bindFramebuffer(int target, int framebuffer) {
		GFX.bindFramebuffer(target, framebuffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
		GFX.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _glBindRenderbuffer(int i, int j) {
		GFX.bindRenderbuffer(i, j);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _glDeleteRenderbuffers(int i) {
		GFX.deleteRenderbuffer(i);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void deleteFramebuffer(int framebuffer) {
		GFX.deleteFramebuffer(framebuffer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int genFramebuffer() {
		return GFX.genFramebuffer();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int glGenRenderbuffers() {
		return GFX.genRenderbuffer();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _glRenderbufferStorage(int i, int j, int k, int l) {
		GFX.renderbufferStorage(i, j, k, l);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _glFramebufferRenderbuffer(int i, int j, int k, int l) {
		GFX.framebufferRenderbuffer(i, j, k, l);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int checkFramebufferStatus(int target) {
		return GFX.checkFramebufferStatus(target);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
		GFX.framebufferTexture2D(target, attachment, textureTarget, texture, level);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getBoundFramebuffer() {
		return GFX.getInteger(GFX.GL_DRAW_FRAMEBUFFER_BINDING);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void blendFuncSeparateUntracked(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
		GFX.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static String getShaderInfoLog(int shader, int maxLength) {
		return GFX.getShaderInfoLog(shader, maxLength);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static String getProgramInfoLog(int program, int maxLength) {
		return GFX.getProgramInfoLog(program, maxLength);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite

	public static void enableCull() {
		GFX.enableCull();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disableCull() {
		GFX.disableCull();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void polygonMode(int face, int mode) {
		GFX.polygonMode(face, mode);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void enablePolygonOffset() {
		GFX.enablePolygonOffset();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disablePolygonOffset() {
		GFX.disablePolygonOffset();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void polygonOffset(float factor, float units) {
		GFX.polygonOffset(factor, units);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void enableColorLogicOp() {
		GFX.enableColorLogicOp();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disableColorLogicOp() {
		GFX.disableColorLogicOp();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void logicOp(int op) {
		GFX.logicOp(op);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void texParameter(int target, int pname, float param) {
		GFX.texParameter(target, pname, param);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void texParameter(int target, int pname, int param) {
		GFX.texParameter(target, pname, param);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getTexLevelParameter(int target, int level, int pname) {
		return GFX.getTexLevelParameter(target, level, pname);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int genTextures() {
		return GFX.genTexture();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void genTextures(int[] is) {
		GFX.genTextures(is);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable IntBuffer pixels) {
		GFX.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
		GFX.texSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void getTexImage(int target, int level, int format, int type, long pixels) {
		GFX.getTexImage(target, level, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void viewport(int x, int y, int width, int height) {
		GFX.viewport(x, y, width, height);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		GFX.colorMask(red, green, blue, alpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void stencilFunc(int func, int ref, int mask) {
		GFX.stencilFunc(func, ref, mask);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void stencilMask(int mask) {
		GFX.stencilMask(mask);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void stencilOp(int sfail, int dpfail, int dppass) {
		GFX.stencilOp(sfail, dpfail, dppass);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void clearDepth(double depth) {
		GFX.clearDepth(depth);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void clearColor(float red, float green, float blue, float alpha) {
		GFX.clearColor(red, green, blue, alpha);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void clearStencil(int stencil) {
		GFX.clearStencil(stencil);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void clear(int mask, boolean getError) {
		GFX.clear(mask, getError);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _glDrawPixels(int i, int j, int k, int l, long m) {
		GFX.drawPixels(i, j, k, l, m);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
		GFX.vertexAttribPointer(index, size, type, normalized, stride, pointer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
		GFX.vertexAttribIPointer(index, size, type, stride, pointer);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void enableVertexAttribArray(int index) {
		GFX.enableVertexAttribArray(index);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void disableVertexAttribArray(int index) {
		GFX.disableVertexAttribArray(index);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void drawElements(int mode, int first, int type, long indices) {
		GFX.drawElements(mode, first, type, indices);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void pixelStore(int pname, int param) {
		GFX.pixelStore(pname, param);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void readPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
		GFX.readPixels(x, y, width, height, format, type, pixels);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static void _readPixels(int i, int j, int k, int l, int m, int n, long o) {
		GFX.readPixels(i, j, k, l, m, n, o);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getError() {
		return GFX.getError();
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static String getString(int name) {
		return GFX.getString(name);
	}

	/**
	 * @author grondag
	 * @reason nukem from space - it's only way to be sure
	 */
	@Overwrite
	public static int getInteger(int pname) {
		return GFX.getInteger(pname);
	}
}
