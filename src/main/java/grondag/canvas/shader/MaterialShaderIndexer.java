/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.shader;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public final class MaterialShaderIndexer {
	public static final MaterialShaderIndexer INSTANCE = new MaterialShaderIndexer();

	private MaterialShaderIndexer() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialShaderIndexer init");
		}
	}

	public synchronized void register(int vertexShaderIndex, int fragmentShaderIndex, ProgramType programType) {
		final long key = key(vertexShaderIndex, fragmentShaderIndex, programType);

		if (KEYS.add(key)) {
			boolean isNew;

			if (programType.isDepth) {
				isNew = DEPTH_VERTEX_INDEXES.add(vertexShaderIndex);
				isNew |= DEPTH_FRAGMENT_INDEXES.add(fragmentShaderIndex);
			} else {
				isNew = VERTEX_INDEXES.add(vertexShaderIndex);
				isNew |= FRAGMENT_INDEXES.add(fragmentShaderIndex);
			}

			// ensure shaders are recompiled when new sub-shader source referenced
			if (isNew) {
				GlProgramManager.INSTANCE.reload();
			}
		}
	}

	/** Tracks which vertex sub-shaders are in use by materials. */
	private static final IntOpenHashSet VERTEX_INDEXES = new IntOpenHashSet();

	/** Tracks which fragment sub-shaders are in use by materials. */
	private static final IntOpenHashSet FRAGMENT_INDEXES = new IntOpenHashSet();

	/** Tracks which vertex depth sub-shaders are in use by materials. */
	private static final IntOpenHashSet DEPTH_VERTEX_INDEXES = new IntOpenHashSet();

	/** Tracks which fragment depth sub-shaders are in use by materials. */
	private static final IntOpenHashSet DEPTH_FRAGMENT_INDEXES = new IntOpenHashSet();

	private static final LongOpenHashSet KEYS = new LongOpenHashSet();

	private static long key(int vertexShaderIndex, int fragmentShaderIndex, ProgramType programType) {
		// PERF: don't need key space this big
		return programType.ordinal() | ((long) fragmentShaderIndex << 16) | ((long) vertexShaderIndex << 32);
	}

	static int[] vertexIds(ProgramType programType) {
		return programType.isDepth ? DEPTH_VERTEX_INDEXES.toIntArray() : VERTEX_INDEXES.toIntArray();
	}

	static int[] fragmentIds(ProgramType programType) {
		return programType.isDepth ? DEPTH_FRAGMENT_INDEXES.toIntArray() : FRAGMENT_INDEXES.toIntArray();
	}
}
