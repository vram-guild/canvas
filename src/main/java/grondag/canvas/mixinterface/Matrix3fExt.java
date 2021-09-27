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

import com.mojang.math.Matrix3f;

public interface Matrix3fExt {
	float m00();

	float m01();

	float m02();

	float m10();

	float m11();

	float m12();

	float m20();

	float m21();

	float m22();

	void m00(float val);

	void m01(float val);

	void m02(float val);

	void m10(float val);

	void m11(float val);

	void m12(float val);

	void m20(float val);

	void m21(float val);

	void m22(float val);

	int canvas_transform(int packedNormal);

	default void set(Matrix3fExt val) {
		m00(val.m00());
		m01(val.m01());
		m02(val.m02());

		m10(val.m10());
		m11(val.m11());
		m12(val.m12());

		m20(val.m20());
		m21(val.m21());
		m22(val.m22());
	}

	default void set(Matrix3f val) {
		set((Matrix3fExt) (Object) val);
	}

	default void writeToBuffer(FloatBuffer floatBuffer) {
		floatBuffer.put(0 * 3 + 0, m00());
		floatBuffer.put(1 * 3 + 0, m01());
		floatBuffer.put(2 * 3 + 0, m02());
		floatBuffer.put(0 * 3 + 1, m10());
		floatBuffer.put(1 * 3 + 1, m11());
		floatBuffer.put(2 * 3 + 1, m12());
		floatBuffer.put(0 * 3 + 2, m20());
		floatBuffer.put(1 * 3 + 2, m21());
		floatBuffer.put(2 * 3 + 2, m22());
	}

	boolean canvas_isIdentity();
}
