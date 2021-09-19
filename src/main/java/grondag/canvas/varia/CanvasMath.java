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

package grondag.canvas.varia;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

public class CanvasMath {
	private CanvasMath() { }

	/**
	 * Non-allocating substitute for {@link Vector3f#rotate(Quaternion)}.
	 */
	public static void applyRotation(Vector3f vec, Quaternion rotation) {
		final float rx = rotation.i();
		final float ry = rotation.j();
		final float rz = rotation.k();
		final float rw = rotation.r();

		final float x = vec.x();
		final float y = vec.y();
		final float z = vec.z();

		final float qx = rw * x + ry * z - rz * y;
		final float qy = rw * y - rx * z + rz * x;
		final float qz = rw * z + rx * y - ry * x;
		final float qw = -rx * x - ry * y - rz * z;

		vec.set(
			qw * -rx + qx * rw - qy * rz + qz * ry,
			qw * -ry + qx * rz + qy * rw - qz * rx,
			qw * -rz - qx * ry + qy * rx + qz * rw);
	}

	/**
	 * Non-allocating substitute for {@link Vector3f#rotate(Quaternion)} that assumes vec.z == 0.
	 */
	public static void applyBillboardRotation(Vector3f vec, Quaternion rotation) {
		final float rx = rotation.i();
		final float ry = rotation.j();
		final float rz = rotation.k();
		final float rw = rotation.r();

		final float x = vec.x();
		final float y = vec.y();

		final float qx = rw * x - rz * y;
		final float qy = rw * y + rz * x;
		final float qz = rx * y - ry * x;
		final float qw = -rx * x - ry * y;

		vec.set(
			qw * -rx + qx * rw - qy * rz + qz * ry,
			qw * -ry + qx * rz + qy * rw - qz * rx,
			qw * -rz - qx * ry + qy * rx + qz * rw);
	}

	public static void setRadialRotation(Quaternion target, Vector3f axis, float radians) {
		final float f = (float) Math.sin(radians / 2.0F);

		target.set(
			axis.x() * f,
			axis.y() * f,
			axis.z() * f,
			(float) Math.cos(radians / 2.0F));
	}

	public static float squareDist(float x0, float y0, float z0, float x1, float y1, float z1) {
		final float dx = x1 - x0;
		final float dy = y1 - y0;
		final float dz = z1 - z0;
		return dx * dx + dy * dy + dz * dz;
	}

	public static float dist(float x0, float y0, float z0, float x1, float y1, float z1) {
		return (float) Math.sqrt(squareDist(x0, y0, z0, x1, y1, z1));
	}

	public static float clampNormalized(float val) {
		return val < 0f ? 0f : (val > 1f ? 1f : val);
	}
}
