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

package grondag.canvas.apiimpl.material;

import java.util.ArrayList;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.util.Identifier;

import grondag.canvas.material.MaterialVertexFormat;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.shader.GlProgram.Uniform3fImpl;
import grondag.canvas.shader.GlShader;
import grondag.canvas.shader.GlShaderManager;
import grondag.canvas.shader.ShaderContext;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.UniformRefreshFrequency;

public final class MaterialShaderImpl implements MaterialShader {
	private final int index;
	private final Identifier vertexShader;
	private final Identifier fragmentShader;
	private final ArrayList<Consumer<GlProgram>> programSetups = new ArrayList<>();

	private final Int2ObjectOpenHashMap<GlProgram> programMap = new Int2ObjectOpenHashMap<>();
	// list is for fast, no-alloc iteration
	private final ObjectArrayList<GlProgram> programList = new ObjectArrayList<>();

	public MaterialShaderImpl(int index, Identifier vertexShader, Identifier fragmentShader) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.index = index;
	}

	private GlProgram getOrCreate(ShaderContext context,  MaterialVertexFormat format) {
		final int key = context.index;
		final GlProgram result = programMap.get(key);

		if(result == null) {
			final GlShader vs = GlShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, context);
			final GlShader fs = GlShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, context);
			final GlProgram newProgram = new GlProgram(vs, fs, format, context);
			programSetups.forEach(ps -> ps.accept(newProgram));
			newProgram.modelOrigin = (Uniform3fImpl) newProgram.uniform3f("_cvu_modelOrigin", UniformRefreshFrequency.ON_LOAD, u -> u.set(0, 0, 0));
			newProgram.load();
			programMap.put(key, newProgram);
			programList.add(newProgram);
			return newProgram;
		} else {
			return result;
		}
	}

	public void activate(ShaderContext context, MaterialVertexFormat format, int x, int y, int z) {
		getOrCreate(context, format).actvateWithiModelOrigin(x, y, z);
	}

	public void reload() {
		programList.forEach(p -> p.unload());
		programList.clear();
		programMap.clear();
	}

	public int getIndex() {
		return index;
	}

	public void addProgramSetup(Consumer<GlProgram> setup) {
		assert setup != null;
		programSetups.add(setup);
	}

	//PERF: hmmm....
	public void onRenderTick() {
		final int limit = programList.size();

		for(int i = 0; i < limit; i++) {
			programList.get(i).onRenderTick();
		}
	}

	//PERF: hmmm....
	public void onGameTick() {
		final int limit = programList.size();
		for(int i = 0; i < limit; i++) {
			programList.get(i).onGameTick();
		}
	}
}
