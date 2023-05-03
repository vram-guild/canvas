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
import java.util.function.Supplier;

import io.vram.frex.api.material.MaterialConstants;

import grondag.canvas.material.state.RenderState;
import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.PipelineFramebuffer;

public enum TargetRenderState implements Predicate<RenderState> {
	SOLID(MaterialConstants.TARGET_SOLID, "solid", () -> Pipeline.solidFbo),
	OUTLINE(MaterialConstants.TARGET_SOLID, "outline", () -> null), // TODO not used
	TRANSLUCENT(MaterialConstants.TARGET_TRANSLUCENT, "translucent", () -> Pipeline.translucentTerrainFbo),
	PARTICLES(MaterialConstants.TARGET_PARTICLES, "particles", () -> Pipeline.translucentParticlesFbo),
	WEATHER(MaterialConstants.TARGET_WEATHER, "weather", () -> Pipeline.weatherFbo),
	CLOUDS(MaterialConstants.TARGET_CLOUDS, "clouds", () -> Pipeline.cloudsFbo),
	ENTITIES(MaterialConstants.TARGET_ENTITIES, "entities", () -> Pipeline.translucentEntitiesFbo);

	private static final TargetRenderState[] VALUES = values();

	public static TargetRenderState fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Supplier<PipelineFramebuffer> framebufferSupplier;

	TargetRenderState(int index, String name, Supplier<PipelineFramebuffer> framebufferSupplier) {
		this.index = index;
		this.name = name;
		this.framebufferSupplier = framebufferSupplier;
	}

	public void enable() {
		if (active != null && active != this) {
			Pipeline.defaultFbo.bind();
		}

		framebufferSupplier.get().bind();
		active = this;
	}

	public static void disable() {
		if (active != null) {
			Pipeline.defaultFbo.bind();
			active = null;
		}
	}

	private static TargetRenderState active = null;

	@Override
	public boolean test(RenderState mat) {
		return mat.target == this;
	}
}
