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

package grondag.canvas.material.property;

import io.vram.frex.api.material.MaterialConstants;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Transparency;

import grondag.canvas.varia.GFX;

public class MaterialTransparency {
	public static final MaterialTransparency NONE = new MaterialTransparency(
		MaterialConstants.TRANSPARENCY_NONE,
		"none",
		6,
		() -> {
			GFX.disableBlend();
		});

	public static final MaterialTransparency ADDITIVE = new MaterialTransparency(
		MaterialConstants.TRANSPARENCY_ADDITIVE,
		"additive",
		2,
		() -> {
			GFX.enableBlend();
			GFX.blendFunc(GFX.GL_ONE, GFX.GL_ONE);
		});

	public static final MaterialTransparency LIGHTNING = new MaterialTransparency(
		MaterialConstants.TRANSPARENCY_LIGHTNING,
		"lightning",
		5,
		() -> {
			GFX.enableBlend();
			GFX.blendFunc(GFX.GL_SRC_ALPHA, GFX.GL_ONE);
		});

	public static final MaterialTransparency GLINT = new MaterialTransparency(
		MaterialConstants.TRANSPARENCY_GLINT,
		"glint",
		1,
		() -> {
			GFX.enableBlend();
			GFX.blendFuncSeparate(GFX.GL_SRC_COLOR, GFX.GL_ONE, GFX.GL_ZERO, GFX.GL_ONE);
		});

	public static final MaterialTransparency CRUMBLING = new MaterialTransparency(
		MaterialConstants.TRANSPARENCY_CRUMBLING,
		"crumbling",
		0,
		() -> {
			GFX.enableBlend();
			GFX.blendFuncSeparate(GFX.GL_DST_COLOR, GFX.GL_SRC_COLOR, GFX.GL_ONE, GFX.GL_ZERO);
		});

	public static final MaterialTransparency TRANSLUCENT = new MaterialTransparency(
		MaterialConstants.TRANSPARENCY_TRANSLUCENT,
		"translucent",
		4,
		() -> {
			GFX.enableBlend();
			GFX.blendFuncSeparate(GFX.GL_SRC_ALPHA, GFX.GL_ONE_MINUS_SRC_ALPHA, GFX.GL_ONE, GFX.GL_ONE_MINUS_SRC_ALPHA);
		});

	/** Used for terrain particles. */
	public static final MaterialTransparency DEFAULT = new MaterialTransparency(
		MaterialConstants.TRANSPARENCY_DEFAULT,
		"default",
		3,
		() -> {
			GFX.enableBlend();
			GFX.blendFuncSeparate(GFX.GL_SRC_ALPHA, GFX.GL_ONE_MINUS_SRC_ALPHA, GFX.GL_ONE, GFX.GL_ZERO);
		});

	public static final int TRANSPARENCY_COUNT = 7;
	private static final MaterialTransparency[] VALUES = new MaterialTransparency[TRANSPARENCY_COUNT];

	static {
		VALUES[MaterialConstants.TRANSPARENCY_NONE] = NONE;
		VALUES[MaterialConstants.TRANSPARENCY_ADDITIVE] = ADDITIVE;
		VALUES[MaterialConstants.TRANSPARENCY_LIGHTNING] = LIGHTNING;
		VALUES[MaterialConstants.TRANSPARENCY_GLINT] = GLINT;
		VALUES[MaterialConstants.TRANSPARENCY_CRUMBLING] = CRUMBLING;
		VALUES[MaterialConstants.TRANSPARENCY_TRANSLUCENT] = TRANSLUCENT;
		VALUES[MaterialConstants.TRANSPARENCY_DEFAULT] = DEFAULT;
	}

	public static MaterialTransparency fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable action;

	/** Higher goes first. */
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
			return MaterialConstants.TRANSPARENCY_ADDITIVE;
		} else if (phase == RenderPhase.LIGHTNING_TRANSPARENCY) {
			return MaterialConstants.TRANSPARENCY_LIGHTNING;
		} else if (phase == RenderPhase.GLINT_TRANSPARENCY) {
			return MaterialConstants.TRANSPARENCY_GLINT;
		} else if (phase == RenderPhase.CRUMBLING_TRANSPARENCY) {
			return MaterialConstants.TRANSPARENCY_CRUMBLING;
		} else if (phase == RenderPhase.TRANSLUCENT_TRANSPARENCY) {
			return MaterialConstants.TRANSPARENCY_TRANSLUCENT;
		} else {
			return MaterialConstants.TRANSPARENCY_NONE;
		}
	}

	private static MaterialTransparency active = null;

	public static void disable() {
		if (active != null) {
			GFX.disableBlend();
			GFX.defaultBlendFunc();
			active = null;
		}
	}
}
