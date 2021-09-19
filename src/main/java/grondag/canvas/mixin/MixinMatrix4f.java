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
	@Shadow
	protected float a00;
	@Shadow
	protected float a01;
	@Shadow
	protected float a02;
	@Shadow
	protected float a03;
	@Shadow
	protected float a10;
	@Shadow
	protected float a11;
	@Shadow
	protected float a12;
	@Shadow
	protected float a13;
	@Shadow
	protected float a20;
	@Shadow
	protected float a21;
	@Shadow
	protected float a22;
	@Shadow
	protected float a23;
	@Shadow
	protected float a30;
	@Shadow
	protected float a31;
	@Shadow
	protected float a32;
	@Shadow
	protected float a33;

	@Override
	public float a00() {
		return a00;
	}

	@Override
	public float a01() {
		return a01;
	}

	@Override
	public float a02() {
		return a02;
	}

	@Override
	public float a03() {
		return a03;
	}

	@Override
	public float a10() {
		return a10;
	}

	@Override
	public float a11() {
		return a11;
	}

	@Override
	public float a12() {
		return a12;
	}

	@Override
	public float a13() {
		return a13;
	}

	@Override
	public float a20() {
		return a20;
	}

	@Override
	public float a21() {
		return a21;
	}

	@Override
	public float a22() {
		return a22;
	}

	@Override
	public float a23() {
		return a23;
	}

	@Override
	public float a30() {
		return a30;
	}

	@Override
	public float a31() {
		return a31;
	}

	@Override
	public float a32() {
		return a32;
	}

	@Override
	public float a33() {
		return a33;
	}

	@Override
	public void a00(float val) {
		a00 = val;
	}

	@Override
	public void a01(float val) {
		a01 = val;
	}

	@Override
	public void a02(float val) {
		a02 = val;
	}

	@Override
	public void a03(float val) {
		a03 = val;
	}

	@Override
	public void a10(float val) {
		a10 = val;
	}

	@Override
	public void a11(float val) {
		a11 = val;
	}

	@Override
	public void a12(float val) {
		a12 = val;
	}

	@Override
	public void a13(float val) {
		a13 = val;
	}

	@Override
	public void a20(float val) {
		a20 = val;
	}

	@Override
	public void a21(float val) {
		a21 = val;
	}

	@Override
	public void a22(float val) {
		a22 = val;
	}

	@Override
	public void a23(float val) {
		a23 = val;
	}

	@Override
	public void a30(float val) {
		a30 = val;
	}

	@Override
	public void a31(float val) {
		a31 = val;
	}

	@Override
	public void a32(float val) {
		a32 = val;
	}

	@Override
	public void a33(float val) {
		a33 = val;
	}

	@Override
	public void writeToBuffer(int baseIndex, FloatBuffer floatBuffer) {
		floatBuffer.put(baseIndex + 0, a00);
		floatBuffer.put(baseIndex + 1, a10);
		floatBuffer.put(baseIndex + 2, a20);
		floatBuffer.put(baseIndex + 3, a30);

		floatBuffer.put(baseIndex + 4, a01);
		floatBuffer.put(baseIndex + 5, a11);
		floatBuffer.put(baseIndex + 6, a21);
		floatBuffer.put(baseIndex + 7, a31);

		floatBuffer.put(baseIndex + 8, a02);
		floatBuffer.put(baseIndex + 9, a12);
		floatBuffer.put(baseIndex + 10, a22);
		floatBuffer.put(baseIndex + 11, a32);

		floatBuffer.put(baseIndex + 12, a03);
		floatBuffer.put(baseIndex + 13, a13);
		floatBuffer.put(baseIndex + 14, a23);
		floatBuffer.put(baseIndex + 15, a33);
	}
}
