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

import com.google.common.util.concurrent.Runnables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Target;

@SuppressWarnings("resource")
public enum MaterialTarget {
	MAIN(Runnables.doNothing(), Runnables.doNothing()),

	OUTLINE(() -> {
		MinecraftClient.getInstance().worldRenderer.getEntityOutlinesFramebuffer().beginWrite(false);
	}, () -> {
		MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
	}),

	TRANSLUCENT(() -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().worldRenderer.getTranslucentFramebuffer().beginWrite(false);
		}
	}, () -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
		}
	}),

	PARTICLES(() -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().worldRenderer.getParticlesFramebuffer().beginWrite(false);
		}
	}, () -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
		}
	}),

	WEATHER(() -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().worldRenderer.getWeatherFramebuffer().beginWrite(false);
		}
	}, () -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
		}
	}),

	CLOUDS(() -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer().beginWrite(false);
		}
	}, () -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
		}
	}),

	ENTITIES(() -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().worldRenderer.getEntityFramebuffer().beginWrite(false);
		}
	}, () -> {
		if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
		}
	});

	private final Runnable startAction;
	private final Runnable endAction;

	private MaterialTarget(Runnable startAction, Runnable endAction) {
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

	public static MaterialTarget fromPhase(Target phase) {
		if (phase == RenderPhase.TRANSLUCENT_TARGET) {
			return TRANSLUCENT;
		} else if (phase == RenderPhase.OUTLINE_TARGET) {
			return OUTLINE;
		} else if (phase == RenderPhase.PARTICLES_TARGET){
			return PARTICLES;
		} else if (phase == RenderPhase.WEATHER_TARGET){
			return WEATHER;
		} else if (phase == RenderPhase.CLOUDS_TARGET){
			return CLOUDS;
		} else if (phase == RenderPhase.ITEM_TARGET){
			return ENTITIES;
		} else {
			assert phase == RenderPhase.MAIN_TARGET : "Unsupported render target";
			return MAIN;
		}
	}
}
