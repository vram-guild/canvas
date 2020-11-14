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
import grondag.frex.api.material.MaterialFinder;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Transparency;

public class MaterialTransparency {
	public static final MaterialTransparency NONE = new MaterialTransparency(
		MaterialFinder.TRANSPARENCY_NONE,
		"none",
		6,
		() -> {
			RenderSystem.disableBlend();
		});

	public static final MaterialTransparency ADDITIVE = new MaterialTransparency(
		MaterialFinder.TRANSPARENCY_ADDITIVE,
		"none",
		2,
		() -> {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
		});

	public static final MaterialTransparency LIGHTNING = new MaterialTransparency(
		MaterialFinder.TRANSPARENCY_LIGHTNING,
		"lightning",
		5,
		() -> {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
		});

	public static final MaterialTransparency GLINT = new MaterialTransparency(
		MaterialFinder.TRANSPARENCY_GLINT,
		"glint",
		1,
		() -> {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_COLOR, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
		});

	public static final MaterialTransparency CRUMBLING = new MaterialTransparency(
		MaterialFinder.TRANSPARENCY_CRUMBLING,
		"crumbling",
		0,
		() -> {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.SRC_COLOR, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
		});

	public static final MaterialTransparency TRANSLUCENT = new MaterialTransparency(
		MaterialFinder.TRANSPARENCY_TRANSLUCENT,
		"translucent",
		4,
		() -> {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		});

	/** used for terrain particles */
	public static final MaterialTransparency DEFAULT = new MaterialTransparency(
		MaterialFinder.TRANSPARENCY_DEFAULT,
		"default",
		3,
		() -> {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
		});

	public static final int TRANSPARENCY_COUNT = 7;
	private static final MaterialTransparency[] VALUES = new MaterialTransparency[TRANSPARENCY_COUNT];

	static {
		VALUES[MaterialFinder.TRANSPARENCY_NONE] = NONE;
		VALUES[MaterialFinder.TRANSPARENCY_ADDITIVE] = ADDITIVE;
		VALUES[MaterialFinder.TRANSPARENCY_LIGHTNING] = LIGHTNING;
		VALUES[MaterialFinder.TRANSPARENCY_GLINT] = GLINT;
		VALUES[MaterialFinder.TRANSPARENCY_CRUMBLING] = CRUMBLING;
		VALUES[MaterialFinder.TRANSPARENCY_TRANSLUCENT] = TRANSLUCENT;
		VALUES[MaterialFinder.TRANSPARENCY_DEFAULT] = DEFAULT;
	}

	public static MaterialTransparency fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable action;

	/** higher goes first */
	public final int drawPriority;

	private MaterialTransparency(int index, String name, int drawPriority, Runnable action) {
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

	public static int fromPhase(Transparency phase) {
		if (phase == RenderPhase.ADDITIVE_TRANSPARENCY) {
			return MaterialFinder.TRANSPARENCY_ADDITIVE;
		} else if (phase == RenderPhase.LIGHTNING_TRANSPARENCY) {
			return MaterialFinder.TRANSPARENCY_LIGHTNING;
		} else if (phase == RenderPhase.GLINT_TRANSPARENCY) {
			return MaterialFinder.TRANSPARENCY_GLINT;
		} else if (phase == RenderPhase.CRUMBLING_TRANSPARENCY) {
			return MaterialFinder.TRANSPARENCY_CRUMBLING;
		} else if (phase == RenderPhase.TRANSLUCENT_TRANSPARENCY) {
			return MaterialFinder.TRANSPARENCY_TRANSLUCENT;
		} else {
			return MaterialFinder.TRANSPARENCY_NONE;
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
