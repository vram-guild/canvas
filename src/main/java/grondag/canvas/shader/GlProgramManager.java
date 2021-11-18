/*
 * Copyright © Original Authors
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

import net.minecraft.client.Minecraft;

import io.vram.sc.unordered.SimpleUnorderedArrayList;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public class GlProgramManager {
	public static final GlProgramManager INSTANCE = new GlProgramManager();

	private GlProgramManager() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: UniformTicker init");
		}
	}

	private final SimpleUnorderedArrayList<GlProgram> programs = new SimpleUnorderedArrayList<>();

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
