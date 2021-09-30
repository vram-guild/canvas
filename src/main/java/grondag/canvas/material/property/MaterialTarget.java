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

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;

import io.vram.frex.api.material.MaterialConstants;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.pipeline.Pipeline;

@SuppressWarnings("resource")
public class MaterialTarget implements Predicate<RenderState> {
	public static final MaterialTarget MAIN = new MaterialTarget(
		MaterialConstants.TARGET_MAIN,
		"main",
		() -> {
			Pipeline.solidTerrainFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget OUTLINE = new MaterialTarget(
		MaterialConstants.TARGET_OUTLINE,
		"outline",
		() -> {
			Minecraft.getInstance().levelRenderer.entityTarget().bindWrite(false);
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget TRANSLUCENT = new MaterialTarget(
		MaterialConstants.TARGET_TRANSLUCENT,
		"translucent",
		() -> {
			Pipeline.translucentTerrainFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget PARTICLES = new MaterialTarget(
		MaterialConstants.TARGET_PARTICLES,
		"particles",
		() -> {
			Pipeline.translucentParticlesFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget WEATHER = new MaterialTarget(
		MaterialConstants.TARGET_WEATHER,
		"weather",
		() -> {
			Pipeline.weatherFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget CLOUDS = new MaterialTarget(
		MaterialConstants.TARGET_CLOUDS,
		"clouds",
		() -> {
			Pipeline.cloudsFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget ENTITIES = new MaterialTarget(
		MaterialConstants.TARGET_ENTITIES,
		"entities",
		() -> {
			Pipeline.translucentEntityFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final int TARGET_COUNT = 7;
	private static final MaterialTarget[] VALUES = new MaterialTarget[TARGET_COUNT];

	static {
		VALUES[MaterialConstants.TARGET_MAIN] = MAIN;
		VALUES[MaterialConstants.TARGET_OUTLINE] = OUTLINE;
		VALUES[MaterialConstants.TARGET_TRANSLUCENT] = TRANSLUCENT;
		VALUES[MaterialConstants.TARGET_PARTICLES] = PARTICLES;
		VALUES[MaterialConstants.TARGET_WEATHER] = WEATHER;
		VALUES[MaterialConstants.TARGET_CLOUDS] = CLOUDS;
		VALUES[MaterialConstants.TARGET_ENTITIES] = ENTITIES;
	}

	public static MaterialTarget fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable startAction;
	private final Runnable endAction;

	private MaterialTarget(int index, String name, Runnable startAction, Runnable endAction) {
		this.index = index;
		this.name = name;
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

	private static MaterialTarget active = null;

	@Override
	public boolean test(RenderState mat) {
		return mat.target == this;
	}
}
