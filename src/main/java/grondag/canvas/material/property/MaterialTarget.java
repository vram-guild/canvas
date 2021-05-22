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

import java.util.function.Predicate;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Target;

import grondag.canvas.material.state.RenderMaterialImpl;
import grondag.canvas.pipeline.Pipeline;
import grondag.frex.api.material.MaterialFinder;

@SuppressWarnings("resource")
public class MaterialTarget implements Predicate<RenderMaterialImpl> {
	public static final MaterialTarget MAIN = new MaterialTarget(
		MaterialFinder.TARGET_MAIN,
		"main",
		() -> {
			Pipeline.solidTerrainFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget OUTLINE = new MaterialTarget(
		MaterialFinder.TARGET_OUTLINE,
		"outline",
		() -> {
			MinecraftClient.getInstance().worldRenderer.getEntityOutlinesFramebuffer().beginWrite(false);
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget TRANSLUCENT = new MaterialTarget(
		MaterialFinder.TARGET_TRANSLUCENT,
		"translucent",
		() -> {
			Pipeline.translucentTerrainFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget PARTICLES = new MaterialTarget(
		MaterialFinder.TARGET_PARTICLES,
		"particles",
		() -> {
			Pipeline.translucentParticlesFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget WEATHER = new MaterialTarget(
		MaterialFinder.TARGET_WEATHER,
		"weather",
		() -> {
			Pipeline.weatherFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget CLOUDS = new MaterialTarget(
		MaterialFinder.TARGET_CLOUDS,
		"clouds",
		() -> {
			Pipeline.cloudsFbo.bind();
		},
		() -> {
			Pipeline.defaultFbo.bind();
		}
	);

	public static final MaterialTarget ENTITIES = new MaterialTarget(
		MaterialFinder.TARGET_ENTITIES,
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
		VALUES[MaterialFinder.TARGET_MAIN] = MAIN;
		VALUES[MaterialFinder.TARGET_OUTLINE] = OUTLINE;
		VALUES[MaterialFinder.TARGET_TRANSLUCENT] = TRANSLUCENT;
		VALUES[MaterialFinder.TARGET_PARTICLES] = PARTICLES;
		VALUES[MaterialFinder.TARGET_WEATHER] = WEATHER;
		VALUES[MaterialFinder.TARGET_CLOUDS] = CLOUDS;
		VALUES[MaterialFinder.TARGET_ENTITIES] = ENTITIES;
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

	public static int fromPhase(Target phase) {
		if (phase == RenderPhase.TRANSLUCENT_TARGET) {
			return MaterialFinder.TARGET_TRANSLUCENT;
		} else if (phase == RenderPhase.OUTLINE_TARGET) {
			return MaterialFinder.TARGET_OUTLINE;
		} else if (phase == RenderPhase.PARTICLES_TARGET) {
			return MaterialFinder.TARGET_PARTICLES;
		} else if (phase == RenderPhase.WEATHER_TARGET) {
			return MaterialFinder.TARGET_WEATHER;
		} else if (phase == RenderPhase.CLOUDS_TARGET) {
			return MaterialFinder.TARGET_CLOUDS;
		} else if (phase == RenderPhase.ITEM_TARGET) {
			return MaterialFinder.TARGET_ENTITIES;
		} else {
			assert phase == RenderPhase.MAIN_TARGET : "Unsupported render target";
			return MaterialFinder.TARGET_MAIN;
		}
	}

	@Override
	public boolean test(RenderMaterialImpl mat) {
		return mat.target == this;
	}
}
