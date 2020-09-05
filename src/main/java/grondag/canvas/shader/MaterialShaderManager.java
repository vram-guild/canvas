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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import grondag.canvas.apiimpl.material.MaterialShaderImpl;
import grondag.canvas.varia.WorldDataManager;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;


public final class MaterialShaderManager implements ClientTickEvents.EndTick {

	public static final int MAX_SHADERS = 0xFFFF;

	public static final MaterialShaderManager INSTANCE = new MaterialShaderManager();

	private final SimpleUnorderedArrayList<MaterialShaderImpl> shaders = new SimpleUnorderedArrayList<>();

	private final MaterialShaderImpl defaultShader;

	/**
	 * Frames are (hopefully) shorter than a client tick. This is the fraction of a
	 * tick that has elapsed since the last complete client tick.
	 */
	private float fractionalTicks;

	/**
	 * Count of client ticks observed by renderer since last restart.
	 */
	private int tickIndex = 0;

	/**
	 * Count of frames observed by renderer since last restart.
	 */
	private int frameIndex = 0;

	private MaterialShaderManager() {
		super();

		ClientTickEvents.END_CLIENT_TICK.register(this);

		// add default shaders
		defaultShader= create(ShaderData.DEFAULT_VERTEX_SOURCE, ShaderData.DEFAULT_FRAGMENT_SOURCE);
	}

	public void reload() {
		final int limit = shaders.size();

		for (int i = 0; i < limit; i++) {
			shaders.get(i).reload();
		}
	}

	public synchronized MaterialShaderImpl create(Identifier vertexShaderSource, Identifier fragmentShaderSource) {

		if(vertexShaderSource == null) {
			vertexShaderSource = ShaderData.DEFAULT_VERTEX_SOURCE;
		}

		if(fragmentShaderSource == null) {
			fragmentShaderSource = ShaderData.DEFAULT_FRAGMENT_SOURCE;
		}

		final MaterialShaderImpl result = new MaterialShaderImpl(shaders.size(), vertexShaderSource, fragmentShaderSource);
		shaders.add(result);

		result.addProgramSetup(ShaderData.STANDARD_UNIFORM_SETUP);

		return result;
	}

	public MaterialShaderImpl get(int index) {
		return shaders.get(index);
	}

	public MaterialShaderImpl getDefault() {
		return defaultShader;
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

	/**
	 * Called just before terrain setup each frame after camera, fog and projection
	 * matrix are set up,
	 */
	public void prepareForFrame(Camera camera) {
		fractionalTicks = MinecraftClient.getInstance().getTickDelta();

		final Entity cameraEntity = camera.getFocusedEntity();
		assert cameraEntity != null;
		assert cameraEntity.getEntityWorld() != null;

		if (cameraEntity == null || cameraEntity.getEntityWorld() == null) {
			return;
		}

		WorldDataManager.update(fractionalTicks);

		onRenderTick();
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
}
