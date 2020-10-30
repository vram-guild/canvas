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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Transparency;

public enum MaterialTransparency {
	NONE (() -> {
		RenderSystem.disableBlend();
	}),

	ADDITIVE (() -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
	}),

	LIGHTNING (() -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
	}),

	GLINT (() -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_COLOR, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
	}),

	CRUMBLING (() -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.SRC_COLOR, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
	}),

	TRANSLUCENT (() -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
	}),

	/** used for terrain particles */
	DEFAULT (() -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
	});

	private final Runnable action;

	private MaterialTransparency(Runnable action) {
		this.action = action;
	}

	public void enable() {
		if (active != this) {
			action.run();
			active = this;
		}
	}

	public static MaterialTransparency fromPhase(Transparency phase) {
		if (phase == RenderPhase.ADDITIVE_TRANSPARENCY) {
			return ADDITIVE;
		} else if (phase == RenderPhase.LIGHTNING_TRANSPARENCY) {
			return LIGHTNING;
		} else if (phase == RenderPhase.GLINT_TRANSPARENCY) {
			return GLINT;
		} else if (phase == RenderPhase.CRUMBLING_TRANSPARENCY) {
			return CRUMBLING;
		} else if (phase == RenderPhase.TRANSLUCENT_TRANSPARENCY) {
			return TRANSLUCENT;
		} else {
			return NONE;
		}
	}

	private static MaterialTransparency active = null;

	public static void disable() {
		if (active != null) {
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			active = null;
		}
	}
}
