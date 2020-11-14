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
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.RenderPhase.Texturing;

// Texture setting - may need to be uniform or conditional compile if fixed pipeline filtering doesn't work
// sets up outline, glint or default texturing
// these probably won't work as-is with shaders because they use texture env settings
// so may be best to leave them for now


// UGLY: use this and handle portal texturing and overlay texturing, somehow
public enum MaterialTexturing {
	DEFAULT(Runnables.doNothing(), Runnables.doNothing()),

	GLINT(() -> RenderPhase.GLINT_TEXTURING.startDrawing(), () -> RenderPhase.GLINT_TEXTURING.endDrawing()),

	ENTITY_GLINT(() -> RenderPhase.ENTITY_GLINT_TEXTURING.startDrawing(), () -> RenderPhase.ENTITY_GLINT_TEXTURING.endDrawing()),

	OUTLINE(RenderSystem::setupOutline, RenderSystem::teardownOutline);

	public final Runnable startAction;
	public final Runnable endAction;

	private MaterialTexturing(Runnable startAction, Runnable endAction) {
		this.startAction = startAction;
		this.endAction = endAction;
	}

	public static MaterialTexturing fromPhase(Texturing phase) {
		if (phase == RenderPhase.GLINT_TEXTURING) {
			return GLINT;
		} else if (phase == RenderPhase.ENTITY_GLINT_TEXTURING) {
			return ENTITY_GLINT;
		} else if (phase == RenderPhase.OUTLINE_TEXTURING) {
			return OUTLINE;
		} else {
			assert phase == RenderPhase.DEFAULT_TEXTURING : "Encountered unsupported texturing render phase";
			return DEFAULT;
		}
	}
}
