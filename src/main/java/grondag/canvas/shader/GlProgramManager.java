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

package grondag.canvas.shader;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;
import grondag.fermion.sc.unordered.SimpleUnorderedArrayList;

public class GlProgramManager implements ClientTickEvents.EndTick {
	public static final GlProgramManager INSTANCE = new GlProgramManager();

	private GlProgramManager() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: UniformTicker init");
		}

		ClientTickEvents.END_CLIENT_TICK.register(this);
	}

	private final SimpleUnorderedArrayList<GlProgram> programs = new SimpleUnorderedArrayList<>();

	@Override
	public void onEndTick(Minecraft client) {
		final int limit = programs.size();

		for (int i = 0; i < limit; i++) {
			programs.get(i).onGameTick();
		}
	}

	public void onRenderTick() {
		final int limit = programs.size();

		for (int i = 0; i < limit; i++) {
			programs.get(i).onRenderTick();
		}
	}

	public void add(GlProgram program) {
		programs.addIfNotPresent(program);
	}

	public void remove(GlProgram program) {
		programs.remove(program);
	}

	public void reload() {
		programs.forEach(s -> s.forceReload());
		programs.clear();
	}
}
