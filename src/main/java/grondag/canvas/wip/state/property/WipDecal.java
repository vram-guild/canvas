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

package grondag.canvas.wip.state.property;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Layering;

public enum WipDecal {
	NONE(Runnables.doNothing(), Runnables.doNothing()),

	POLYGON_OFFSET(() -> {
		RenderSystem.polygonOffset(-1.0F, -10.0F);
		RenderSystem.enablePolygonOffset();
	}, () -> {
		RenderSystem.polygonOffset(0.0F, 0.0F);
		RenderSystem.disablePolygonOffset();
	}),

	VIEW_OFFSET(() -> {
		RenderSystem.pushMatrix();
		RenderSystem.scalef(0.99975586F, 0.99975586F, 0.99975586F);
	}, RenderSystem::popMatrix);

	private final Runnable startAction;
	private final Runnable endAction;

	private WipDecal(Runnable startAction, Runnable endAction) {
		this.startAction = startAction;
		this.endAction = endAction;
	}

	public void enable() {
		if (active != null && active != this) {
			active.endAction.run();
		}

		startAction.run();
		active = this;
	}

	public static void disable() {
		if (active != null) {
			active.endAction.run();
			active = null;
		}
	}

	private static WipDecal active = null;

	public static WipDecal fromPhase(Layering phase) {
		if (phase == RenderPhase.VIEW_OFFSET_Z_LAYERING) {
			return VIEW_OFFSET;
		} else if (phase == RenderPhase.POLYGON_OFFSET_LAYERING) {
			return POLYGON_OFFSET;
		} else {
			return NONE;
		}
	}
}
