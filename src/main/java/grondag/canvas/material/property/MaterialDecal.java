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

package grondag.canvas.material.property;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.systems.RenderSystem;
import grondag.frex.api.material.MaterialFinder;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Layering;

public final class MaterialDecal {
	public static final int DECAL_COUNT = 3;
	private static final MaterialDecal[] VALUES = new MaterialDecal[DECAL_COUNT];

	public static MaterialDecal fromIndex(int index) {
		return VALUES[index];
	}

	public static final MaterialDecal NONE = new MaterialDecal(
		MaterialFinder.DECAL_NONE,
		"none",
		0,
		Runnables.doNothing(),
		Runnables.doNothing());

	public static final MaterialDecal POLYGON_OFFSET = new MaterialDecal(
		MaterialFinder.DECAL_POLYGON_OFFSET,
		"polygon_offset",
		1,
		() -> {
			RenderSystem.polygonOffset(-1.0F, -10.0F);
			RenderSystem.enablePolygonOffset();
		},
		() -> {
			RenderSystem.polygonOffset(0.0F, 0.0F);
			RenderSystem.disablePolygonOffset();
		});

	public static final MaterialDecal VIEW_OFFSET = new MaterialDecal(
		MaterialFinder.DECAL_VIEW_OFFSET,
		"view_offset",
		2,
		() -> {
			RenderSystem.pushMatrix();
			RenderSystem.scalef(0.99975586F, 0.99975586F, 0.99975586F);
		},
		RenderSystem::popMatrix);

	static {
		VALUES[MaterialFinder.DECAL_NONE] = NONE;
		VALUES[MaterialFinder.DECAL_POLYGON_OFFSET] = POLYGON_OFFSET;
		VALUES[MaterialFinder.DECAL_VIEW_OFFSET] = VIEW_OFFSET;
	}

	public final int index;
	public final String name;
	private final Runnable startAction;
	private final Runnable endAction;

	/** higher goes first */
	public final int drawPriority;

	private MaterialDecal(int index, String name, int drawPriority, Runnable startAction, Runnable endAction) {
		this.index = index;
		this.name = name;
		this.drawPriority = drawPriority;
		this.startAction = startAction;
		this.endAction = endAction;
	}

	public void enable() {
		// must run end action for view offset each time to prevent matrix stack overrun
		if (active != null && (active == VIEW_OFFSET || active != this)) {
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

	private static MaterialDecal active = null;

	public static int fromPhase(Layering phase) {
		if (phase == RenderPhase.VIEW_OFFSET_Z_LAYERING) {
			return MaterialFinder.DECAL_VIEW_OFFSET;
		} else if (phase == RenderPhase.POLYGON_OFFSET_LAYERING) {
			return MaterialFinder.DECAL_POLYGON_OFFSET;
		} else {
			return MaterialFinder.DECAL_NONE;
		}
	}
}
