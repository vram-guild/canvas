/*
 * Copyright © Contributing Authors
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

package grondag.canvas.material.property;

import com.google.common.util.concurrent.Runnables;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import io.vram.frex.api.material.MaterialConstants;

import grondag.canvas.varia.GFX;

public final class DecalRenderState {
	private static final DecalRenderState[] VALUES = new DecalRenderState[MaterialConstants.DECAL_COUNT];

	public static DecalRenderState fromIndex(int index) {
		return VALUES[index];
	}

	public static final DecalRenderState NONE = new DecalRenderState(
		MaterialConstants.DECAL_NONE,
		"none",
		0,
		Runnables.doNothing(),
		Runnables.doNothing());

	public static final DecalRenderState POLYGON_OFFSET = new DecalRenderState(
		MaterialConstants.DECAL_POLYGON_OFFSET,
		"polygon_offset",
		1,
		() -> {
			GFX.polygonOffset(-1.0F, -10.0F);
			GFX.enablePolygonOffset();
		},
		() -> {
			GFX.polygonOffset(0.0F, 0.0F);
			GFX.disablePolygonOffset();
		});

	public static final DecalRenderState VIEW_OFFSET = new DecalRenderState(
		MaterialConstants.DECAL_VIEW_OFFSET,
		"view_offset",
		2,
		() -> {
			final PoseStack matrixStack = RenderSystem.getModelViewStack();
			matrixStack.pushPose();
			matrixStack.scale(0.99975586F, 0.99975586F, 0.99975586F);
			RenderSystem.applyModelViewMatrix();
		},
		() -> {
			RenderSystem.getModelViewStack().popPose();
			RenderSystem.applyModelViewMatrix();
		});

	static {
		VALUES[MaterialConstants.DECAL_NONE] = NONE;
		VALUES[MaterialConstants.DECAL_POLYGON_OFFSET] = POLYGON_OFFSET;
		VALUES[MaterialConstants.DECAL_VIEW_OFFSET] = VIEW_OFFSET;
	}

	public final int index;
	public final String name;
	private final Runnable startAction;
	private final Runnable endAction;

	/** Higher goes first. */
	public final int drawPriority;

	private DecalRenderState(int index, String name, int drawPriority, Runnable startAction, Runnable endAction) {
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

	private static DecalRenderState active = null;
}
