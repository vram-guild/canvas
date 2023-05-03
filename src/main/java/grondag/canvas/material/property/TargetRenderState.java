/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.material.property;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;

import io.vram.frex.api.material.MaterialConstants;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.pipeline.Pipeline;

@SuppressWarnings("resource")
public enum TargetRenderState implements Predicate<RenderState> {
	SOLID(
		MaterialConstants.TARGET_SOLID,
		"solid",
			() -> {
				Pipeline.solidTerrainFbo.bind();
			},
			() -> {
				Pipeline.defaultFbo.bind();
			}
	),
	OUTLINE(
		MaterialConstants.TARGET_OUTLINE,
		"outline",
			() -> {
				Minecraft.getInstance().levelRenderer.entityTarget().bindWrite(false);
			},
			() -> {
				Pipeline.defaultFbo.bind();
			}
	),
	TRANSLUCENT(
		MaterialConstants.TARGET_TRANSLUCENT,
		"translucent",
			() -> {
				Pipeline.translucentTerrainFbo.bind();
			},
			() -> {
				Pipeline.defaultFbo.bind();
			}
	),
	PARTICLES(
		MaterialConstants.TARGET_PARTICLES,
		"particles",
			() -> {
				Pipeline.translucentParticlesFbo.bind();
			},
			() -> {
				Pipeline.defaultFbo.bind();
			}
	),
	WEATHER(
		MaterialConstants.TARGET_WEATHER,
		"weather",
			() -> {
				Pipeline.weatherFbo.bind();
			},
			() -> {
				Pipeline.defaultFbo.bind();
			}
	),
	CLOUDS(
		MaterialConstants.TARGET_CLOUDS,
		"clouds",
			() -> {
				Pipeline.cloudsFbo.bind();
			},
			() -> {
				Pipeline.defaultFbo.bind();
			}
	),
	ENTITIES(
		MaterialConstants.TARGET_ENTITIES,
		"entities",
			() -> {
				Pipeline.translucentEntityFbo.bind();
			},
			() -> {
				Pipeline.defaultFbo.bind();
			}
	);

	private static final TargetRenderState[] VALUES = values();

	public static TargetRenderState fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable startAction;
	private final Runnable endAction;

	TargetRenderState(int index, String name, Runnable startAction, Runnable endAction) {
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

	private static TargetRenderState active = null;

	@Override
	public boolean test(RenderState mat) {
		return mat.target == this;
	}
}
