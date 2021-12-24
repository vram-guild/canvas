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
