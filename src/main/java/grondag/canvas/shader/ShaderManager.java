/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.shader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.event.client.ClientTickCallback;

import grondag.canvas.apiimpl.MaterialShaderImpl;
import grondag.canvas.varia.DitherTexture;
import grondag.canvas.varia.WorldDataManager;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;
import grondag.frex.api.material.UniformRefreshFrequency;


public final class ShaderManager implements ClientTickCallback {
	public static final int MAX_SHADERS = 0xFFFF;

	public static final ShaderManager INSTANCE = new ShaderManager();

	private final SimpleUnorderedArrayList<MaterialShaderImpl> shaders = new SimpleUnorderedArrayList<>();

	private final MaterialShaderImpl defaultShader;

	/**
	 * World time ticks at last render reload..
	 */
	private long baseWorldTime;

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

	private ShaderManager() {
		super();

		ClientTickCallback.EVENT.register(this);

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

		addStandardUniforms(result);

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

	private void addStandardUniforms(MaterialShaderImpl shader) {
		shader.uniformArrayf("_cvu_world", UniformRefreshFrequency.PER_TICK, u -> u.set(WorldDataManager.data()), WorldDataManager.LENGTH);

		shader.uniformSampler2d("_cvu_textures", UniformRefreshFrequency.ON_LOAD, u -> u.set(0));

		shader.uniformSampler2d("_cvu_lightmap", UniformRefreshFrequency.ON_LOAD, u -> u.set(2));

		// FIX: may need to move because of lightmap move
		shader.uniformSampler2d("_cvu_dither", UniformRefreshFrequency.ON_LOAD, u -> u.set(5));

		// FIX: may need to move because of lightmap move
		//UGLY: needs a better GLSL name
		shader.uniformSampler2d("_cvu_utility", UniformRefreshFrequency.ON_LOAD, u -> u.set(4));
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

		WorldDataManager.update(fractionalTicks, (cameraEntity.getEntityWorld().getTime() - baseWorldTime + fractionalTicks) / 20);

		onRenderTick();
	}

	@Override
	public void tick(MinecraftClient client) {
		tickIndex++;
		final int limit = shaders.size();
		for (int i = 0; i < limit; i++) {
			shaders.get(i).onGameTick();
		}

		//UGLY: need central tick handler
		DitherTexture.instance().tick();
	}

	public void onRenderTick() {
		frameIndex++;
		final int limit = shaders.size();
		for (int i = 0; i < limit; i++) {
			shaders.get(i).onRenderTick();
		}
	}
}
