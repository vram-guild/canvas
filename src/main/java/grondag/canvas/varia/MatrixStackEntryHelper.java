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

package grondag.canvas.varia;

import java.lang.reflect.Constructor;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;

public class MatrixStackEntryHelper {
	private static final Constructor<MatrixStack.Entry> CONSTRUCTOR;

	static {
		Constructor<MatrixStack.Entry> c;

		try {
			c = MatrixStack.Entry.class.getDeclaredConstructor(Matrix4f.class, Matrix3f.class);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		c.setAccessible(true);
		CONSTRUCTOR = c;
	}

	public static MatrixStack.Entry create(Matrix4f matrix4f, Matrix3f matrix3f) {
		try {
			return CONSTRUCTOR.newInstance(matrix4f, matrix3f);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
