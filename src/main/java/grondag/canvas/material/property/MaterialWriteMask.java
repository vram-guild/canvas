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

public class MaterialWriteMask {
	public static final MaterialWriteMask COLOR = new MaterialWriteMask(
		MaterialConstants.WRITE_MASK_COLOR,
		"color",
		0,
		() -> {
			GFX.depthMask(false);
			GFX.colorMask(true, true, true, true);
		}
	);

	public static final MaterialWriteMask DEPTH = new MaterialWriteMask(
		MaterialConstants.WRITE_MASK_DEPTH,
		"depth",
		2,
		() -> {
			GFX.depthMask(true);
			GFX.colorMask(false, false, false, false);
		}
	);

	public static final MaterialWriteMask COLOR_DEPTH = new MaterialWriteMask(
		MaterialConstants.WRITE_MASK_COLOR_DEPTH,
		"color_depth",
		1,
		() -> {
			GFX.depthMask(true);
			GFX.colorMask(true, true, true, true);
		}
	);

	public static final int WRITE_MASK_COUNT = 3;
	private static final MaterialWriteMask[] VALUES = new MaterialWriteMask[WRITE_MASK_COUNT];

	static {
		VALUES[MaterialConstants.WRITE_MASK_COLOR] = COLOR;
		VALUES[MaterialConstants.WRITE_MASK_DEPTH] = DEPTH;
		VALUES[MaterialConstants.WRITE_MASK_COLOR_DEPTH] = COLOR_DEPTH;
	}

	public static MaterialWriteMask fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable action;

	/** Higher goes first. */
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

	private static MaterialWriteMask active = null;

	public static void disable() {
		if (active != null) {
			COLOR_DEPTH.action.run();
			active = null;
		}
	}
}
