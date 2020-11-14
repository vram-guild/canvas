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

import com.mojang.blaze3d.systems.RenderSystem;
import grondag.frex.api.material.MaterialFinder;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.DepthTest;

public class MaterialDepthTest {
	public static final MaterialDepthTest DISABLE = new MaterialDepthTest(
		MaterialFinder.DEPTH_TEST_DISABLE,
		"disable",
		() -> {
			RenderSystem.disableDepthTest();
			RenderSystem.depthFunc(GL11.GL_NEVER);
		});

	public static final MaterialDepthTest ALWAYS = new MaterialDepthTest(
		MaterialFinder.DEPTH_TEST_ALWAYS,
		"disable",
		() -> {
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_ALWAYS);
		});

	public static final MaterialDepthTest EQUAL = new MaterialDepthTest(
		MaterialFinder.DEPTH_TEST_EQUAL,
		"disable",
		() -> {
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_EQUAL);
		});

	public static final MaterialDepthTest LEQUAL = new MaterialDepthTest(
		MaterialFinder.DEPTH_TEST_LEQUAL,
		"disable",
		() -> {
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
		});

	public static final int DEPTH_TEST_COUNT = 4;
	private static final MaterialDepthTest[] VALUES = new MaterialDepthTest[DEPTH_TEST_COUNT];

	static {
		VALUES[MaterialFinder.DEPTH_TEST_DISABLE] = DISABLE;
		VALUES[MaterialFinder.DEPTH_TEST_ALWAYS] = ALWAYS;
		VALUES[MaterialFinder.DEPTH_TEST_EQUAL] = EQUAL;
		VALUES[MaterialFinder.DEPTH_TEST_LEQUAL] = LEQUAL;
	}

	public static MaterialDepthTest fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable action;

	private MaterialDepthTest(int index, String name, Runnable action) {
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

	public static int fromPhase(DepthTest phase) {
		if (phase == RenderPhase.ALWAYS_DEPTH_TEST) {
			return MaterialFinder.DEPTH_TEST_ALWAYS;
		} else if (phase == RenderPhase.EQUAL_DEPTH_TEST) {
			return MaterialFinder.DEPTH_TEST_EQUAL;
		} else if (phase == RenderPhase.LEQUAL_DEPTH_TEST) {
			return MaterialFinder.DEPTH_TEST_LEQUAL;
		} else {
			return MaterialFinder.DEPTH_TEST_DISABLE;
		}
	}

	private static MaterialDepthTest active = null;

	public static void disable() {
		if (active != null) {
			DISABLE.action.run();
			active = null;
		}
	}
}
