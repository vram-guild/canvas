package grondag.canvas.varia;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Quaternion;

public class CanvasMath {
	private CanvasMath() {}

	// WIP: remove
	public static boolean hack() {
		return false;
	}

	/**
	 * Non-allocating substitute for {@link Vector3f#rotate(Quaternion)}
	 */
	public static void applyRotation(Vector3f vec, Quaternion rotation) {
		final float rx = rotation.getX();
		final float ry = rotation.getY();
		final float rz = rotation.getZ();
		final float rw = rotation.getW();

		final float x = vec.getX();
		final float y = vec.getY();
		final float z = vec.getZ();

		final float qx = rw * x + ry * z - rz * y;
		final float qy = rw * y - rx * z + rz * x;
		final float qz = rw * z + rx * y - ry * x;
		final float qw = - rx * x - ry * y - rz * z;

		vec.set(
			qw * -rx + qx * rw - qy * rz + qz * ry,
			qw * -ry + qx * rz + qy * rw - qz * rx,
			qw * -rz - qx * ry + qy * rx + qz * rw);
	}

	public static void setRadialRotation(Quaternion target, Vector3f axis, float radians) {
		final float f = (float) Math.sin(radians / 2.0F);

		target.set(
			axis.getX() * f,
			axis.getY() * f,
			axis.getZ() * f,
			(float) Math.cos(radians / 2.0F));
	}
}
