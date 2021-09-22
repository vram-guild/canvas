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
	@Shadow protected float m00;
	@Shadow protected float m01;
	@Shadow protected float m02;
	@Shadow protected float m10;
	@Shadow protected float m11;
	@Shadow protected float m12;
	@Shadow protected float m20;
	@Shadow protected float m21;
	@Shadow protected float m22;

	@Override
	public int canvas_transform(int packedNormal) {
		final float x = PackedVector3f.packedX(packedNormal);
		final float y = PackedVector3f.packedY(packedNormal);
		final float z = PackedVector3f.packedZ(packedNormal);

		final float nx = fma(m00, x, fma(m01, y, m02 * z));
		final float ny = fma(m10, x, fma(m11, y, m12 * z));
		final float nz = fma(m20, x, fma(m21, y, m22 * z));

		return PackedVector3f.pack(nx, ny, nz);
	}

	@Override
	public boolean canvas_isIdentity() {
		return m00 == 1.0F && m01 == 0.0F && m02 == 0.0F
				&& m10 == 0.0F && m11 == 1.0F && m12 == 0.0
				&& m20 == 0.0F && m21 == 0.0F && m22 == 1.0F;
	}

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
}
