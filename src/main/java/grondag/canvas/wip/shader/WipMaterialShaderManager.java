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

package grondag.canvas.wip.shader;

import grondag.canvas.CanvasMod;
import grondag.canvas.Configurator;
import grondag.canvas.wip.encoding.WipVertexFormat;
import grondag.canvas.wip.state.WipProgramType;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.fermion.varia.IndexedInterner;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;


public enum WipMaterialShaderManager implements ClientTickEvents.EndTick {
	INSTANCE;

	public static final int MAX_SHADERS = 0xFFFF;

	private final SimpleUnorderedArrayList<WipMaterialShaderImpl> shaders = new SimpleUnorderedArrayList<>();

	/**
	 * Count of client ticks observed by renderer since last restart.
	 */
	private int tickIndex = 0;

	/**
	 * Count of frames observed by renderer since last restart.
	 */
	private int frameIndex = 0;

	private WipMaterialShaderManager() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: MaterialShaderManager init");
		}

		ClientTickEvents.END_CLIENT_TICK.register(this);
	}

	public void reload() {
		final int limit = shaders.size();

		for (int i = 0; i < limit; i++) {
			shaders.get(i).reload();
		}
	}

	public synchronized WipMaterialShaderImpl find(int vertexShaderIndex, int fragmentShaderIndex, WipProgramType programType, WipVertexFormat format) {
		final long key = key(vertexShaderIndex, fragmentShaderIndex, programType, format);

		WipMaterialShaderImpl result = KEYMAP.get(key);

		if (result == null) {
			result = create(vertexShaderIndex, fragmentShaderIndex, programType, format);
			KEYMAP.put(key, result);
		}

		return result;
	}

	private synchronized WipMaterialShaderImpl create(int vertexShaderIndex, int fragmentShaderIndex, WipProgramType programType, WipVertexFormat format) {
		final WipMaterialShaderImpl result = new WipMaterialShaderImpl(shaders.size(), vertexShaderIndex, fragmentShaderIndex, programType, format);
		shaders.add(result);
		return result;
	}

	public WipMaterialShaderImpl get(int index) {
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

	public static final IndexedInterner<Identifier> fragmentIndex = new IndexedInterner<>(Identifier.class);
	public static final IndexedInterner<Identifier> vertexIndex = new IndexedInterner<>(Identifier.class);
	private static final Long2ObjectOpenHashMap<WipMaterialShaderImpl> KEYMAP = new Long2ObjectOpenHashMap<>();

	private static long key(int vertexShaderIndex, int fragmentShaderIndex, WipProgramType programType, WipVertexFormat format) {
		return format.formatIndex | (programType.ordinal() << 16) | ((long) fragmentShaderIndex << 32) | ((long) vertexShaderIndex << 48);
	}
}
