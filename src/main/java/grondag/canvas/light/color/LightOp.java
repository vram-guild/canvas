/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.light.color;

public enum LightOp {
	R(0xF000, 12, 0),
	G(0x0F00, 8, 1),
	B(0x00F0, 4, 2),
	A(0x000F, 0, 3);

	public final int mask;
	public final int shift;
	public final int pos;

	LightOp(int mask, int shift, int pos) {
		this.mask = mask;
		this.shift = shift;
		this.pos = pos;
	}

	private static final int EMITTER_FLAG = 0b1;
	private static final int OCCLUDER_FLAG = 0b10;
	private static final int FULL_FLAG = 0b100;
	private static final int USEFUL_FLAG = 0b1000;

	public static short EMPTY = 0;

	public static short encode(int r, int g, int b, int a) {
		return (short) ((r << R.shift) | (g << G.shift) | (b << B.shift) | (a << A.shift));
	}

	public static short encodeLight(int r, int g, int b, boolean isFull, boolean isEmitter, boolean isOccluding) {
		return ensureUsefulness(encode(r, g, b, encodeAlpha(isFull, isEmitter, isOccluding)));
	}

	public static short encodeLight(int pureLight, boolean isFull, boolean isEmitter, boolean isOccluding) {
		return ensureUsefulness((short) (pureLight | encodeAlpha(isFull, isEmitter, isOccluding)));
	}

	private static int encodeAlpha(boolean isFull, boolean isEmitter, boolean isOccluding) {
		return (isFull ? FULL_FLAG : 0) | (isEmitter ? EMITTER_FLAG : 0) | (isOccluding ? OCCLUDER_FLAG : 0);
	}

	private static short ensureUsefulness(short light) {
		boolean useful = lit(light) || !occluder(light);
		return useful ? (short) (light | USEFUL_FLAG) : (short) (light & ~USEFUL_FLAG);
	}

	public static boolean emitter(short light) {
		return (light & EMITTER_FLAG) != 0;
	}

	public static boolean occluder(short light) {
		return (light & OCCLUDER_FLAG) != 0;
	}

	public static boolean full(short light) {
		return (light & FULL_FLAG) != 0;
	}

	public static boolean lit(short light) {
		return (light & 0xfff0) != 0;
	}

	public static short max(short master, short sub) {
		final short max = (short) (Math.max(master & R.mask, sub & R.mask)
				| Math.max(master & G.mask, sub & G.mask)
				| Math.max(master & B.mask, sub & B.mask)
				| master & 0xf);
		return ensureUsefulness(max);
	}

	public static short replaceMinusOne(short target, short source, BVec opFlag) {
		if (opFlag.r) {
			target = R.replace(target, (short) (R.of(source) - 1));
		}

		if (opFlag.g) {
			target = G.replace(target, (short) (G.of(source) - 1));
		}

		if (opFlag.b) {
			target = B.replace(target, (short) (B.of(source) - 1));
		}

		return LightOp.ensureUsefulness(target);
	}

	public static short remove(short target, BVec opFlag) {
		int mask = (opFlag.r ? LightOp.R.mask : 0) | (opFlag.g ? LightOp.G.mask : 0) | (opFlag.b ? LightOp.B.mask : 0);
		return LightOp.ensureUsefulness((short) (target & ~mask));
	}

	public int of(short light) {
		return (light >> shift) & 0xF;
	}

	private short replace(short source, short elemLight) {
		return (short) ((source & ~mask) | (elemLight << shift));
	}

	public static String text(short light) {
		return "(" + R.of(light) + "," + G.of(light) + "," + B.of(light) + ")";
	}

	static class BVec {
		boolean r, g, b;

		BVec() {
			this.r = false;
			this.g = false;
			this.b = false;
		}

		boolean any() {
			return r || g || b;
		}

		boolean all() {
			return r && g && b;
		}

		BVec not() {
			r = !r;
			g = !g;
			b = !b;
			return this;
		}

		BVec lessThan(short left, short right) {
			r = R.of(left) < R.of(right);
			g = G.of(left) < G.of(right);
			b = B.of(left) < B.of(right);
			return this;
		}

		BVec lessThanMinusOne(short left, short right) {
			r = R.of(left) < R.of(right) - 1;
			g = G.of(left) < G.of(right) - 1;
			b = B.of(left) < B.of(right) - 1;
			return this;
		}

		BVec and(BVec other, BVec another) {
			r = other.r && another.r;
			g = other.g && another.g;
			b = other.b && another.b;
			return this;
		}
	}
}
