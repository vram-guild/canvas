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

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import grondag.canvas.CanvasMod;

public class VaoTracker {
	private static final Int2IntOpenHashMap MAP = new Int2IntOpenHashMap();

	private static int bound = 0;
	private static int flags = 0;

	public static void gen(int vao) {
		if (vao <= 0) {
			CanvasMod.LOG.warn("Vao " + vao + " invalid gen");
		} else if (MAP.containsKey(vao)) {
			CanvasMod.LOG.warn("Vao " + vao + " already in use");
		} else {
			MAP.put(vao, 0);
		}
	}

	public static void del(int vao) {
		if (vao <= 0) {
			CanvasMod.LOG.warn("Vao " + vao + " invalid del");
		} else if (MAP.containsKey(vao)) {
			if (vao == bound) {
				CanvasMod.LOG.warn("Bound vao " + vao + " deleted");
				bound = 0;
				flags = 0;
			}

			MAP.remove(vao);
		} else {
			CanvasMod.LOG.warn("Vao " + vao + " not in use");
		}
	}

	public static void bind(int vao) {
		if (vao < 0) {
			CanvasMod.LOG.warn("Vao " + vao + " invalid bind");
		} else if (vao > 0 && !MAP.containsKey(vao)) {
			CanvasMod.LOG.warn("Vao " + vao + " bind unallocated");
		} else {
			bound = vao;
			flags = MAP.get(vao);
		}
	}

	public static void enable(int index) {
		if (bound == 0) {
			CanvasMod.LOG.warn("Enable vao index " + index + " without bound vao");
		} else {
			final int mask = 1 << index;

			if ((flags & mask) == 0) {
				flags |= mask;
				MAP.put(bound, flags);
			} else {
				CanvasMod.LOG.warn("Enable vao index " + index + " already enabled for vao " + bound);
			}
		}
	}

	public static void disable(int index) {
		if (bound == 0) {
			CanvasMod.LOG.warn("Disable vao index " + index + " without bound vao");
		} else {
			final int mask = 1 << index;

			if ((flags & mask) != 0) {
				flags &= ~mask;
				MAP.put(bound, flags);
			} else {
				CanvasMod.LOG.warn("Disable vao index " + index + " already disabled for vao " + bound);
			}
		}
	}
}
