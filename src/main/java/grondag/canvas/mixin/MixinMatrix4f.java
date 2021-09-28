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

import java.nio.FloatBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.math.Matrix4f;

import grondag.canvas.mixinterface.Matrix4fExt;

@Mixin(Matrix4f.class)
public class MixinMatrix4f implements Matrix4fExt {
	@Shadow protected float m00;
	@Shadow protected float m01;
	@Shadow protected float m02;
	@Shadow protected float m03;
	@Shadow protected float m10;
	@Shadow protected float m11;
	@Shadow protected float m12;
	@Shadow protected float m13;
	@Shadow protected float m20;
	@Shadow protected float m21;
	@Shadow protected float m22;
	@Shadow protected float m23;
	@Shadow protected float m30;
	@Shadow protected float m31;
	@Shadow protected float m32;
	@Shadow protected float m33;

	@Override
	public float m00() {
		return m00;
	}

	@Override
	public float m01() {
		return m01;
	}

	@Override
	public float m02() {
		return m02;
	}

	@Override
	public float m03() {
		return m03;
	}

	@Override
	public float m10() {
		return m10;
	}

	@Override
	public float m11() {
		return m11;
	}

	@Override
	public float m12() {
		return m12;
	}

	@Override
	public float m13() {
		return m13;
	}

	@Override
	public float m20() {
		return m20;
	}

	@Override
	public float m21() {
		return m21;
	}

	@Override
	public float m22() {
		return m22;
	}

	@Override
	public float m23() {
		return m23;
	}

	@Override
	public float m30() {
		return m30;
	}

	@Override
	public float m31() {
		return m31;
	}

	@Override
	public float m32() {
		return m32;
	}

	@Override
	public float m33() {
		return m33;
	}

	@Override
	public void m00(float val) {
		m00 = val;
	}

	@Override
	public void m01(float val) {
		m01 = val;
	}

	@Override
	public void m02(float val) {
		m02 = val;
	}

	@Override
	public void m03(float val) {
		m03 = val;
	}

	@Override
	public void m10(float val) {
		m10 = val;
	}

	@Override
	public void m11(float val) {
		m11 = val;
	}

	@Override
	public void m12(float val) {
		m12 = val;
	}

	@Override
	public void m13(float val) {
		m13 = val;
	}

	@Override
	public void m20(float val) {
		m20 = val;
	}

	@Override
	public void m21(float val) {
		m21 = val;
	}

	@Override
	public void m22(float val) {
		m22 = val;
	}

	@Override
	public void m23(float val) {
		m23 = val;
	}

	@Override
	public void m30(float val) {
		m30 = val;
	}

	@Override
	public void m31(float val) {
		m31 = val;
	}

	@Override
	public void m32(float val) {
		m32 = val;
	}

	@Override
	public void m33(float val) {
		m33 = val;
	}

	@Override
	public void writeToBuffer(int baseIndex, FloatBuffer floatBuffer) {
		floatBuffer.put(baseIndex + 0, m00);
		floatBuffer.put(baseIndex + 1, m10);
		floatBuffer.put(baseIndex + 2, m20);
		floatBuffer.put(baseIndex + 3, m30);

		floatBuffer.put(baseIndex + 4, m01);
		floatBuffer.put(baseIndex + 5, m11);
		floatBuffer.put(baseIndex + 6, m21);
		floatBuffer.put(baseIndex + 7, m31);

		floatBuffer.put(baseIndex + 8, m02);
		floatBuffer.put(baseIndex + 9, m12);
		floatBuffer.put(baseIndex + 10, m22);
		floatBuffer.put(baseIndex + 11, m32);

		floatBuffer.put(baseIndex + 12, m03);
		floatBuffer.put(baseIndex + 13, m13);
		floatBuffer.put(baseIndex + 14, m23);
		floatBuffer.put(baseIndex + 15, m33);
	}
}
