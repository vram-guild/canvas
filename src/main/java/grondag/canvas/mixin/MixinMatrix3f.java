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

import static java.lang.Math.fma;

import com.mojang.math.Matrix3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import grondag.canvas.apiimpl.util.PackedVector3f;
import grondag.canvas.mixinterface.Matrix3fExt;

@Mixin(Matrix3f.class)
public class MixinMatrix3f implements Matrix3fExt {
	@Shadow
	protected float a00;
	@Shadow
	protected float a01;
	@Shadow
	protected float a02;
	@Shadow
	protected float a10;
	@Shadow
	protected float a11;
	@Shadow
	protected float a12;
	@Shadow
	protected float a20;
	@Shadow
	protected float a21;
	@Shadow
	protected float a22;

	@Override
	public int canvas_transform(int packedNormal) {
		final float x = PackedVector3f.packedX(packedNormal);
		final float y = PackedVector3f.packedY(packedNormal);
		final float z = PackedVector3f.packedZ(packedNormal);

		final float nx = fma(a00, x, fma(a01, y, a02 * z));
		final float ny = fma(a10, x, fma(a11, y, a12 * z));
		final float nz = fma(a20, x, fma(a21, y, a22 * z));

		return PackedVector3f.pack(nx, ny, nz);
	}

	@Override
	public boolean canvas_isIdentity() {
		return a00 == 1.0F && a01 == 0.0F && a02 == 0.0F
				&& a10 == 0.0F && a11 == 1.0F && a12 == 0.0
				&& a20 == 0.0F && a21 == 0.0F && a22 == 1.0F;
	}

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
}
