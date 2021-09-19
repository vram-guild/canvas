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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.OutputStateShard;
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

	public static int fromPhase(OutputStateShard phase) {
		if (phase == RenderStateShard.TRANSLUCENT_TARGET) {
			return MaterialConstants.TARGET_TRANSLUCENT;
		} else if (phase == RenderStateShard.OUTLINE_TARGET) {
			return MaterialConstants.TARGET_OUTLINE;
		} else if (phase == RenderStateShard.PARTICLES_TARGET) {
			return MaterialConstants.TARGET_PARTICLES;
		} else if (phase == RenderStateShard.WEATHER_TARGET) {
			return MaterialConstants.TARGET_WEATHER;
		} else if (phase == RenderStateShard.CLOUDS_TARGET) {
			return MaterialConstants.TARGET_CLOUDS;
		} else if (phase == RenderStateShard.ITEM_ENTITY_TARGET) {
			return MaterialConstants.TARGET_ENTITIES;
		} else {
			assert phase == RenderStateShard.MAIN_TARGET : "Unsupported render target";
			return MaterialConstants.TARGET_MAIN;
		}
	}

	@Override
	public boolean test(RenderState mat) {
		return mat.target == this;
	}
}
