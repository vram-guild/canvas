/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.mixinterface;

import java.nio.FloatBuffer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface Matrix3fExt {
	float a00();

	float a01();

	float a02();

	float a10();

	float a11();

	float a12();

	float a20();

	float a21();

	float a22();

	void a00(float val);

	void a01(float val);

	void a02(float val);

	void a10(float val);

	void a11(float val);

	void a12(float val);

	void a20(float val);

	void a21(float val);

	void a22(float val);

	int canvas_transform(int packedNormal);

	default void set(Matrix3fExt val) {
		a00(val.a00());
		a01(val.a01());
		a02(val.a02());

		a10(val.a10());
		a11(val.a11());
		a12(val.a12());

		a20(val.a20());
		a21(val.a21());
		a22(val.a22());
	}

	@Environment(EnvType.CLIENT)
	default void writeToBuffer(FloatBuffer floatBuffer) {
		floatBuffer.put(0 * 3 + 0, a00());
		floatBuffer.put(1 * 3 + 0, a01());
		floatBuffer.put(2 * 3 + 0, a02());
		floatBuffer.put(0 * 3 + 1, a10());
		floatBuffer.put(1 * 3 + 1, a11());
		floatBuffer.put(2 * 3 + 1, a12());
		floatBuffer.put(0 * 3 + 2, a20());
		floatBuffer.put(1 * 3 + 2, a21());
		floatBuffer.put(2 * 3 + 2, a22());
	}
}
