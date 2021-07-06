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

package grondag.canvas.render;

import grondag.canvas.varia.GFX;

/**
 * Deals with Mojang's unfortunate assumptions regarding the existence of
 * anything that is not GL_TEXTURE_2D or more than 12 texture units.
 */
public class CanvasTextureState {
	private static final int MAX_TEXTURES = 64;
	private static final int[] BOUND_TEXTURES = new int[MAX_TEXTURES];
	private static int activeTextureUnit = 0;

	public static void bindTexture(int target, int texture) {
		if (texture != BOUND_TEXTURES[activeTextureUnit]) {
			BOUND_TEXTURES[activeTextureUnit] = texture;
			GFX.bindTexture(target, texture);
		}
	}

	public static void bindTexture(int texture) {
		bindTexture(GFX.GL_TEXTURE_2D, texture);
	}

	public static void deleteTexture(int texture) {
		GFX.deleteTexture(texture);

		for (int i = 0; i < MAX_TEXTURES; ++i) {
			if (BOUND_TEXTURES[i] == texture) {
				BOUND_TEXTURES[i] = 0;
			}
		}
	}

	public static int getActiveBoundTexture() {
		return BOUND_TEXTURES[activeTextureUnit];
	}

	public static void activeTextureUnit(int textureUnit) {
		if (activeTextureUnit != textureUnit - GFX.GL_TEXTURE0) {
			activeTextureUnit = textureUnit - GFX.GL_TEXTURE0;
			GFX.activeTexture(textureUnit);
		}
	}

	public static int activeTextureUnit() {
		return activeTextureUnit + GFX.GL_TEXTURE0;
	}

	public static void deleteTextures(int[] textures) {
		for (final int t : textures) {
			deleteTexture(t);
		}
	}

	public static int getTextureId(int i) {
		return (i < 0 || i >= MAX_TEXTURES) ? 0 : BOUND_TEXTURES[i];
	}
}
