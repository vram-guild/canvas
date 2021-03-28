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

package grondag.canvas.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import grondag.canvas.render.CanvasTextureState;

@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {
	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static void activeTexture(int textureUnit) {
		CanvasTextureState.activeTextureUnit(textureUnit);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static void bindTexture(int texture) {
		CanvasTextureState.bindTexture(GL11.GL_TEXTURE_2D, texture);
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static int method_34411() {
		return CanvasTextureState.activeTextureUnit();
	}

	/**
	 * @author grondag
	 * @reason 12 units / 2D only not enough
	 */
	@Overwrite
	public static void deleteTexture(int texture) {
		CanvasTextureState.deleteTexture(texture);
	}
}
