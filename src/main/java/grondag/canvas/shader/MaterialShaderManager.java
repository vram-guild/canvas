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
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.resources.ResourceLocation;

import io.vram.sc.unordered.SimpleUnorderedArrayList;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.canvas.shader.data.ShaderStrings;
import grondag.fermion.varia.IndexedInterner;

public enum MaterialShaderManager {
	INSTANCE;

	private final SimpleUnorderedArrayList<MaterialShaderImpl> shaders = new SimpleUnorderedArrayList<>();

	MaterialShaderManager() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialShaderManager init");
		}
	}

	public synchronized MaterialShaderImpl find(int vertexShaderIndex, int fragmentShaderIndex, ProgramType programType) {
		final long key = key(vertexShaderIndex, fragmentShaderIndex, programType);
		MaterialShaderImpl result = KEYMAP.get(key);

		if (result == null) {
			result = create(vertexShaderIndex, fragmentShaderIndex, programType);
			KEYMAP.put(key, result);
		}

		return result;
	}

	private synchronized MaterialShaderImpl create(int vertexShaderIndex, int fragmentShaderIndex, ProgramType programType) {
		final MaterialShaderImpl result = new MaterialShaderImpl(shaders.size(), vertexShaderIndex, fragmentShaderIndex, programType);
		shaders.add(result);

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

		return result;
	}

	public MaterialShaderImpl get(int index) {
		return shaders.get(index);
	}

	/** Tracks which vertex sub-shaders are in use by materials. */
	private static final IntOpenHashSet VERTEX_INDEXES = new IntOpenHashSet();

	/** Tracks which fragment sub-shaders are in use by materials. */
	private static final IntOpenHashSet FRAGMENT_INDEXES = new IntOpenHashSet();

	/** Tracks which vertex depth sub-shaders are in use by materials. */
	private static final IntOpenHashSet DEPTH_VERTEX_INDEXES = new IntOpenHashSet();

	/** Tracks which fragment depth sub-shaders are in use by materials. */
	private static final IntOpenHashSet DEPTH_FRAGMENT_INDEXES = new IntOpenHashSet();

	public static final IndexedInterner<ResourceLocation> VERTEX_INDEXER = new IndexedInterner<>(ResourceLocation.class);
	public static final IndexedInterner<ResourceLocation> FRAGMENT_INDEXER = new IndexedInterner<>(ResourceLocation.class);
	private static final Long2ObjectOpenHashMap<MaterialShaderImpl> KEYMAP = new Long2ObjectOpenHashMap<>();

	private static long key(int vertexShaderIndex, int fragmentShaderIndex, ProgramType programType) {
		// PERF: don't need key space this big
		return programType.ordinal() | ((long) fragmentShaderIndex << 16) | ((long) vertexShaderIndex << 32);
	}

	public static final int DEFAULT_VERTEX_INDEX = VERTEX_INDEXER.toHandle(ShaderStrings.DEFAULT_VERTEX_SOURCE);
	public static final int DEFAULT_FRAGMENT_INDEX = FRAGMENT_INDEXER.toHandle(ShaderStrings.DEFAULT_FRAGMENT_SOURCE);

	static int[] vertexIds(ProgramType programType) {
		return programType.isDepth ? DEPTH_VERTEX_INDEXES.toIntArray() : VERTEX_INDEXES.toIntArray();
	}

	static int[] fragmentIds(ProgramType programType) {
		return programType.isDepth ? DEPTH_FRAGMENT_INDEXES.toIntArray() : FRAGMENT_INDEXES.toIntArray();
	}
}
