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

package grondag.canvas.mixinterface;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Matrix4f;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface Matrix4fExt {
	float a00();

	float a01();

	float a02();

	float a03();

	float a10();

	float a11();

	float a12();

	float a13();

	float a20();

	float a21();

	float a22();

	float a23();

	float a30();

	float a31();

	float a32();

	float a33();

	void a00(float val);

	void a01(float val);

	void a02(float val);

	void a03(float val);

	void a10(float val);

	void a11(float val);

	void a12(float val);

	void a13(float val);

	void a20(float val);

	void a21(float val);

	void a22(float val);

	void a23(float val);

	void a30(float val);

	void a31(float val);

	void a32(float val);

	void a33(float val);

	default void multiply(Matrix4fExt val) {
		((Matrix4f) (Object) this).multiply((Matrix4f) (Object) val);
	}

	default void loadIdentity() {
		((Matrix4f) (Object) this).loadIdentity();
	}

	default void set(Matrix4fExt val) {
		a00(val.a00());
		a01(val.a01());
		a02(val.a02());
		a03(val.a03());

		a10(val.a10());
		a11(val.a11());
		a12(val.a12());
		a13(val.a13());

		a20(val.a20());
		a21(val.a21());
		a22(val.a22());
		a23(val.a23());

		a30(val.a30());
		a31(val.a31());
		a32(val.a32());
		a33(val.a33());
	}

	default void set(Matrix4f val) {
		set((Matrix4fExt) (Object) val);
	}

	default boolean matches(Matrix4fExt val) {
		return a00() == val.a00()
			&& a01() == val.a01()
			&& a02() == val.a02()
			&& a03() == val.a03()

			&& a10() == val.a10()
			&& a11() == val.a11()
			&& a12() == val.a12()
			&& a13() == val.a13()

			&& a20() == val.a20()
			&& a21() == val.a21()
			&& a22() == val.a22()
			&& a23() == val.a23()

			&& a30() == val.a30()
			&& a31() == val.a31()
			&& a32() == val.a32()
			&& a33() == val.a33();
	}

	default boolean matches(Matrix4f val) {
		return matches((Matrix4fExt) (Object) val);
	}

	default void fastTransform(Vector3f vec) {
		final float x = vec.getX();
		final float y = vec.getY();
		final float z = vec.getZ();

		vec.set(
			a00() * x + a01() * y + a02() * z + a03(),
			a10() * x + a11() * y + a12() * z + a13(),
			a20() * x + a21() * y + a22() * z + a23());
	}

	default void translate(float x, float y, float z) {
		final float b03 = a00() * x + a01() * y + a02() * z + a03();
		final float b13 = a10() * x + a11() * y + a12() * z + a13();
		final float b23 = a20() * x + a21() * y + a22() * z + a23();
		final float b33 = a30() * x + a31() * y + a32() * z + a33();

		a03(b03);
		a13(b13);
		a23(b23);
		a33(b33);
	}

	default void scale(float x, float y, float z) {
		final float b00 = a00() * x;
		final float b01 = a01() * y;
		final float b02 = a02() * z;
		final float b10 = a10() * x;
		final float b11 = a11() * y;
		final float b12 = a12() * z;
		final float b20 = a20() * x;
		final float b21 = a21() * y;
		final float b22 = a22() * z;
		final float b30 = a30() * x;
		final float b31 = a31() * y;
		final float b32 = a32() * z;

		a00(b00);
		a01(b01);
		a02(b02);
		a10(b10);
		a11(b11);
		a12(b12);
		a20(b20);
		a21(b21);
		a22(b22);
		a30(b30);
		a31(b31);
		a32(b32);
	}
}
