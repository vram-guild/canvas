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

import java.nio.FloatBuffer;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import grondag.bitraster.Matrix4L;

public interface Matrix4fExt {
	float m00();

	float m01();

	float m02();

	float m03();

	float m10();

	float m11();

	float m12();

	float m13();

	float m20();

	float m21();

	float m22();

	float m23();

	float m30();

	float m31();

	float m32();

	float m33();

	void m00(float val);

	void m01(float val);

	void m02(float val);

	void m03(float val);

	void m10(float val);

	void m11(float val);

	void m12(float val);

	void m13(float val);

	void m20(float val);

	void m21(float val);

	void m22(float val);

	void m23(float val);

	void m30(float val);

	void m31(float val);

	void m32(float val);

	void m33(float val);

	default void multiply(Matrix4fExt val) {
		((Matrix4f) (Object) this).multiply((Matrix4f) (Object) val);
	}

	default void loadIdentity() {
		((Matrix4f) (Object) this).setIdentity();
	}

	default void set(Matrix4fExt val) {
		m00(val.m00());
		m01(val.m01());
		m02(val.m02());
		m03(val.m03());

		m10(val.m10());
		m11(val.m11());
		m12(val.m12());
		m13(val.m13());

		m20(val.m20());
		m21(val.m21());
		m22(val.m22());
		m23(val.m23());

		m30(val.m30());
		m31(val.m31());
		m32(val.m32());
		m33(val.m33());
	}

	default void set(Matrix4f val) {
		set((Matrix4fExt) (Object) val);
	}

	default boolean matches(Matrix4fExt val) {
		return m00() == val.m00()
			&& m01() == val.m01()
			&& m02() == val.m02()
			&& m03() == val.m03()

			&& m10() == val.m10()
			&& m11() == val.m11()
			&& m12() == val.m12()
			&& m13() == val.m13()

			&& m20() == val.m20()
			&& m21() == val.m21()
			&& m22() == val.m22()
			&& m23() == val.m23()

			&& m30() == val.m30()
			&& m31() == val.m31()
			&& m32() == val.m32()
			&& m33() == val.m33();
	}

	default boolean matches(Matrix4f val) {
		return matches((Matrix4fExt) (Object) val);
	}

	default void fastTransform(Vector3f vec) {
		final float x = vec.x();
		final float y = vec.y();
		final float z = vec.z();

		vec.set(
			m00() * x + m01() * y + m02() * z + m03(),
			m10() * x + m11() * y + m12() * z + m13(),
			m20() * x + m21() * y + m22() * z + m23());
	}

	default void translate(float x, float y, float z) {
		final float b03 = m00() * x + m01() * y + m02() * z + m03();
		final float b13 = m10() * x + m11() * y + m12() * z + m13();
		final float b23 = m20() * x + m21() * y + m22() * z + m23();
		final float b33 = m30() * x + m31() * y + m32() * z + m33();

		m03(b03);
		m13(b13);
		m23(b23);
		m33(b33);
	}

	default void scale(float x, float y, float z) {
		final float b00 = m00() * x;
		final float b01 = m01() * y;
		final float b02 = m02() * z;
		final float b10 = m10() * x;
		final float b11 = m11() * y;
		final float b12 = m12() * z;
		final float b20 = m20() * x;
		final float b21 = m21() * y;
		final float b22 = m22() * z;
		final float b30 = m30() * x;
		final float b31 = m31() * y;
		final float b32 = m32() * z;

		m00(b00);
		m01(b01);
		m02(b02);
		m10(b10);
		m11(b11);
		m12(b12);
		m20(b20);
		m21(b21);
		m22(b22);
		m30(b30);
		m31(b31);
		m32(b32);
	}

	void writeToBuffer(int baseIndex, FloatBuffer floatBuffer);

	/**
	 * Maps view space (with camera pointing towards negative Z) to -1/+1 NDC
	 * coordinates expected by OpenGL.
	 *
	 * <P>Note comments on near and far distance! These are depth along z axis,
	 * or in other words, you must negate the view space z-axis bounds when passing them.
	 *
	 * @param left bound towards negative x axis
	 * @param right bound towards positive x axis
	 * @param bottom bound towards negative y axis
	 * @param top bound towards positive y axis
	 * @param near distance of near plane from camera (POSITIVE!)
	 * @param far distance of far plane from camera (POSITIVE!)
	 */
	default void setOrtho(float left, float right, float bottom, float top, float near, float far) {
		loadIdentity();
		m00(2.0f / (right - left));
		m03(-(right + left) / (right - left));

		m11(2.0f / (top - bottom));
		m13(-(top + bottom) / (top - bottom));

		m22(2.0f / (near - far));
		m23(-(far + near) / (far - near));
	}

	// best explanation seen so far:  http://www.songho.ca/opengl/gl_camera.html#lookat
	default void lookAt(
		float fromX, float fromY, float fromZ,
		float toX, float toY, float toZ,
		float basisX, float basisY, float basisZ
	) {
		// the forward (Z) axis is the implied look vector
		float forwardX, forwardY, forwardZ;
		forwardX = fromX - toX;
		forwardY = fromY - toY;
		forwardZ = fromZ - toZ;

		final float inverseForwardLength = 1.0f / (float) Math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ);
		forwardX *= inverseForwardLength;
		forwardY *= inverseForwardLength;
		forwardZ *= inverseForwardLength;

		// the left (X) axis is found with cross product of forward and given "up" vector
		float leftX, leftY, leftZ;
		leftX = basisY * forwardZ - basisZ * forwardY;
		leftY = basisZ * forwardX - basisX * forwardZ;
		leftZ = basisX * forwardY - basisY * forwardX;

		final float inverseLengthA = 1.0f / (float) Math.sqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
		leftX *= inverseLengthA;
		leftY *= inverseLengthA;
		leftZ *= inverseLengthA;

		// Orthonormal "up" axis (Y) is the cross product of those two
		// Should already be a unit vector as both inputs are.
		final float upX = forwardY * leftZ - forwardZ * leftY;
		final float upY = forwardZ * leftX - forwardX * leftZ;
		final float upZ = forwardX * leftY - forwardY * leftX;

		m00(leftX);
		m01(leftY);
		m02(leftZ);
		m03(-(leftX * fromX + leftY * fromY + leftZ * fromZ));
		m10(upX);
		m11(upY);
		m12(upZ);
		m13(-(upX * fromX + upY * fromY + upZ * fromZ));
		m20(forwardX);
		m21(forwardY);
		m22(forwardZ);
		m23(-(forwardX * fromX + forwardY * fromY + forwardZ * fromZ));
		m30(0.0f);
		m31(0.0f);
		m32(0.0f);
		m33(1.0f);
	}

	static void copy(Matrix4f src, Matrix4L target) {
		((Matrix4fExt) (Object) src).copyTo(target);
	}

	default void copyTo(Matrix4L target) {
		target.set(
				this.m00(), this.m01(), this.m02(), this.m03(),
				this.m10(), this.m11(), this.m12(), this.m13(),
				this.m20(), this.m21(), this.m22(), this.m23(),
				this.m30(), this.m31(), this.m32(), this.m33());
	}

	static Matrix4fExt cast(Matrix4f matrix) {
		return (Matrix4fExt) (Object) matrix;
	}
}
