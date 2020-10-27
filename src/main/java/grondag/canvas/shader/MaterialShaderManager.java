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

package grondag.canvas.shader;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.IndexedInterner;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;


public enum MaterialShaderManager implements ClientTickEvents.EndTick {
	INSTANCE;

	private final SimpleUnorderedArrayList<MaterialShaderImpl> shaders = new SimpleUnorderedArrayList<>();

	/**
	 * Count of client ticks observed by renderer since last restart.
	 */
	private int tickIndex = 0;

	/**
	 * Count of frames observed by renderer since last restart.
	 */
	private int frameIndex = 0;

	private MaterialShaderManager() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialShaderManager init");
		}

		ClientTickEvents.END_CLIENT_TICK.register(this);
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

		final boolean newVert = VERTEX_INDEXES.add(vertexShaderIndex);
		final boolean newFrag = FRAGMENT_INDEXES.add(fragmentShaderIndex);

		// ensure shaders are recompiled when new sub-shader source referenced
		if (newVert || newFrag) {
			MaterialProgramManager.INSTANCE.reload();
		}

		return result;
	}

	public MaterialShaderImpl get(int index) {
		return shaders.get(index);
	}

	/**
	 * The number of shaders currently registered.
	 */
	public int shaderCount() {
		return shaders.size();
	}

	public int tickIndex() {
		return tickIndex;
	}

	public int frameIndex() {
		return frameIndex;
	}

	@Override
	public void onEndTick(MinecraftClient client) {
		tickIndex++;
		final int limit = shaders.size();
		for (int i = 0; i < limit; i++) {
			shaders.get(i).onGameTick();
		}
	}

	public void onRenderTick() {
		frameIndex++;
		final int limit = shaders.size();
		for (int i = 0; i < limit; i++) {
			shaders.get(i).onRenderTick();
		}
	}

	/** tracks which vertex sub-shaders are in use by materials */
	public static final IntOpenHashSet VERTEX_INDEXES = new IntOpenHashSet();

	/** tracks which fragmet sub-shaders are in use by materials */
	public static final IntOpenHashSet FRAGMENT_INDEXES = new IntOpenHashSet();

	public static final int MAX_SHADERS = 0xFFFF;
	public static final IndexedInterner<Identifier> VERTEX_INDEXER = new IndexedInterner<>(Identifier.class);
	public static final IndexedInterner<Identifier> FRAGMENT_INDEXER = new IndexedInterner<>(Identifier.class);
	private static final Long2ObjectOpenHashMap<MaterialShaderImpl> KEYMAP = new Long2ObjectOpenHashMap<>();

	private static long key(int vertexShaderIndex, int fragmentShaderIndex, ProgramType programType) {
		// PERF: don't need key space this big
		return programType.ordinal() | ((long) fragmentShaderIndex << 16) | ((long) vertexShaderIndex << 32);
	}

	public static final int DEFAULT_VERTEX_INDEX = VERTEX_INDEXER.toHandle(ShaderData.DEFAULT_VERTEX_SOURCE);
	public static final int DEFAULT_FRAGMENT_INDEX = FRAGMENT_INDEXER.toHandle(ShaderData.DEFAULT_FRAGMENT_SOURCE);
}
