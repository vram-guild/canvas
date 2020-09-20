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

package grondag.canvas.wip.state.property;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Fog;

public enum WipFog {
	NO_FOG(() -> {
		BackgroundRenderer.setFogBlack();
		RenderSystem.disableFog();
	}),

	FOG(() -> {
		BackgroundRenderer.setFogBlack();
		RenderSystem.enableFog();
	}),

	BLACK_FOG(() -> {
		RenderSystem.fog(2918, 0.0F, 0.0F, 0.0F, 1.0F);
		RenderSystem.enableFog();
	});

	public final Runnable action;

	private WipFog(Runnable action) {
		this.action = action;
	}

	public static WipFog fromPhase(Fog phase) {
		if (phase == RenderPhase.FOG) {
			return FOG;
		} else if (phase == RenderPhase.BLACK_FOG) {
			return BLACK_FOG;
		} else {
			assert phase == RenderPhase.NO_FOG : "Encounted unknown fog mode";
			return NO_FOG;
		}
	}
}
