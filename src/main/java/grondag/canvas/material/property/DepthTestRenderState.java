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

package grondag.canvas.material.property;

import io.vram.frex.api.material.MaterialConstants;

import grondag.canvas.varia.GFX;

public class DepthTestRenderState {
	public static final DepthTestRenderState DISABLE = new DepthTestRenderState(
		MaterialConstants.DEPTH_TEST_DISABLE,
		"disable",
		() -> {
			GFX.disableDepthTest();
			GFX.depthFunc(GFX.GL_LEQUAL);
		});

	public static final DepthTestRenderState ALWAYS = new DepthTestRenderState(
		MaterialConstants.DEPTH_TEST_ALWAYS,
		"disable",
		() -> {
			GFX.enableDepthTest();
			GFX.depthFunc(GFX.GL_ALWAYS);
		});

	public static final DepthTestRenderState EQUAL = new DepthTestRenderState(
		MaterialConstants.DEPTH_TEST_EQUAL,
		"disable",
		() -> {
			GFX.enableDepthTest();
			GFX.depthFunc(GFX.GL_EQUAL);
		});

	public static final DepthTestRenderState LEQUAL = new DepthTestRenderState(
		MaterialConstants.DEPTH_TEST_LEQUAL,
		"disable",
		() -> {
			GFX.enableDepthTest();
			GFX.depthFunc(GFX.GL_LEQUAL);
		});

	private static final DepthTestRenderState[] VALUES = new DepthTestRenderState[MaterialConstants.DEPTH_TEST_COUNT];

	static {
		VALUES[MaterialConstants.DEPTH_TEST_DISABLE] = DISABLE;
		VALUES[MaterialConstants.DEPTH_TEST_ALWAYS] = ALWAYS;
		VALUES[MaterialConstants.DEPTH_TEST_EQUAL] = EQUAL;
		VALUES[MaterialConstants.DEPTH_TEST_LEQUAL] = LEQUAL;
	}

	public static DepthTestRenderState fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable action;

	private DepthTestRenderState(int index, String name, Runnable action) {
		this.index = index;
		this.name = name;
		this.action = action;
	}

	public void enable() {
		if (active != this) {
			action.run();
			active = this;
		}
	}

	private static DepthTestRenderState active = null;

	public static void disable() {
		if (active != null) {
			DISABLE.action.run();
			active = null;
		}
	}
}
