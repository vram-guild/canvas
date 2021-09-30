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

import io.vram.frex.api.material.MaterialConstants;

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

	private static MaterialTransparency active = null;

	public static void disable() {
		if (active != null) {
			GFX.disableBlend();
			GFX.defaultBlendFunc();
			active = null;
		}
	}
}
