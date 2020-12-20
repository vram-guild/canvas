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

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Fog;

import grondag.canvas.varia.FogStateExtHolder;
import grondag.frex.api.material.MaterialFinder;

public class MaterialFog {
	public static final MaterialFog NONE = new MaterialFog(
		MaterialFinder.FOG_NONE,
		"none",
		() -> {
			BackgroundRenderer.setFogBlack();
			isEnabled = false;
			//RenderSystem.disableFog();
		}
	);

	public static final MaterialFog TINTED = new MaterialFog(
		MaterialFinder.FOG_TINTED,
		"tinted",
		() -> {
			BackgroundRenderer.setFogBlack();
			isEnabled = true;
			//RenderSystem.enableFog();
		}
	);

	public static final MaterialFog BLACK = new MaterialFog(
		MaterialFinder.FOG_BLACK,
		"black",
		() -> {
			RenderSystem.fog(2918, 0.0F, 0.0F, 0.0F, 1.0F);
			isEnabled = true;
			//RenderSystem.enableFog();
		}
	);

	public static final int FOG_COUNT = 3;
	private static final MaterialFog[] VALUES = new MaterialFog[FOG_COUNT];

	static {
		VALUES[MaterialFinder.FOG_NONE] = NONE;
		VALUES[MaterialFinder.FOG_TINTED] = TINTED;
		VALUES[MaterialFinder.FOG_BLACK] = BLACK;
	}

	public static MaterialFog fromIndex(int index) {
		return VALUES[index];
	}

	public final int index;
	public final String name;
	private final Runnable action;

	private MaterialFog(int index, String name, Runnable action) {
		this.index = index;
		this.name = name;
		this.action = action;
	}

	public void enable() {
		if (active != this) {
			action.run();
			active = this;
			updateShaderParam();
		}
	}

	public static int fromPhase(Fog phase) {
		if (phase == RenderPhase.FOG) {
			return MaterialFinder.FOG_TINTED;
		} else if (phase == RenderPhase.BLACK_FOG) {
			return MaterialFinder.FOG_BLACK;
		} else {
			assert phase == RenderPhase.NO_FOG : "Encounted unknown fog mode";
			return MaterialFinder.FOG_NONE;
		}
	}

	private static MaterialFog active = null;

	public static void disable() {
		if (active != null) {
			NONE.action.run();
			active = null;
			updateShaderParam();
		}
	}

	private static boolean isAllowed = false;
	private static boolean isEnabled = false;
	private static int shaderParam = 0;

	private static final int FOG_LINEAR = 0;
	private static final int FOG_EXP = 1;
	private static final int FOG_EXP2 = 2;
	private static final int FOG_DISABLE = 3;

	private static void updateShaderParam() {
		if (isAllowed && isEnabled) {
			final int fogMode = FogStateExtHolder.INSTANCE.getMode();

			if (fogMode == 2048) {
				shaderParam = FOG_EXP;
			} else if (fogMode == 2049) {
				shaderParam = FOG_EXP2;
			} else {
				assert fogMode == 9729;
				shaderParam = FOG_LINEAR;
			}
		} else {
			// disable
			shaderParam = FOG_DISABLE;
		}
	}

	/**
	 * Fog normally disabled except during world render.
	 */
	public static void allow(boolean allow) {
		isAllowed = allow;
		updateShaderParam();
	}

	public static int shaderParam() {
		return shaderParam;
	}
}
