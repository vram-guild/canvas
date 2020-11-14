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

import com.mojang.blaze3d.systems.RenderSystem;
import grondag.frex.api.material.MaterialFinder;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.WriteMaskState;

public class MaterialWriteMask {
	public static final MaterialWriteMask COLOR = new MaterialWriteMask(
		MaterialFinder.WRITE_MASK_COLOR,
		"color",
		0,
		() -> {
			RenderSystem.depthMask(false);
			RenderSystem.colorMask(true, true, true, true);
		}
	);

	public static final MaterialWriteMask DEPTH = new MaterialWriteMask(
		MaterialFinder.WRITE_MASK_DEPTH,
		"depth",
		2,
		() -> {
			RenderSystem.depthMask(true);
			RenderSystem.colorMask(false, false, false, false);
		}
	);

	public static final MaterialWriteMask COLOR_DEPTH = new MaterialWriteMask(
		MaterialFinder.WRITE_MASK_COLOR_DEPTH,
		"color_depth",
		1,
		() -> {
			RenderSystem.depthMask(true);
			RenderSystem.colorMask(true, true, true, true);
		}
	);

	public static final int WRITE_MASK_COUNT = 3;
	private static final MaterialWriteMask[] VALUES = new MaterialWriteMask[WRITE_MASK_COUNT];

	static {
		VALUES[MaterialFinder.WRITE_MASK_COLOR] = COLOR;
		VALUES[MaterialFinder.WRITE_MASK_DEPTH] = DEPTH;
		VALUES[MaterialFinder.WRITE_MASK_COLOR_DEPTH] = COLOR_DEPTH;
	}

	public static MaterialWriteMask fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable action;

	/** higher goes first */
	public final int drawPriority;

	private MaterialWriteMask(int index, String name, int drawPriority, Runnable action) {
		this.index = index;
		this.name = name;
		this.drawPriority = drawPriority;
		this.action = action;
	}

	public void enable() {
		if (active != this) {
			action.run();
			active = this;
		}
	}

	public static int fromPhase(WriteMaskState phase) {
		if (phase == RenderPhase.COLOR_MASK) {
			return MaterialFinder.WRITE_MASK_COLOR;
		} else if (phase == RenderPhase.DEPTH_MASK) {
			return MaterialFinder.WRITE_MASK_DEPTH;
		} else {
			return MaterialFinder.WRITE_MASK_COLOR_DEPTH;
		}
	}

	private static MaterialWriteMask active = null;

	public static void disable() {
		if (active != null) {
			COLOR_DEPTH.action.run();
			active = null;
		}
	}
}
