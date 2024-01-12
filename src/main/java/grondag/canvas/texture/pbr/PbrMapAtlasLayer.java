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

package grondag.canvas.texture.pbr;

import static grondag.canvas.texture.pbr.PbrMapSpriteLayer.AO;
import static grondag.canvas.texture.pbr.PbrMapSpriteLayer.EMISSIVE;
import static grondag.canvas.texture.pbr.PbrMapSpriteLayer.HEIGHT;
import static grondag.canvas.texture.pbr.PbrMapSpriteLayer.NORMAL;
import static grondag.canvas.texture.pbr.PbrMapSpriteLayer.REFLECTANCE;
import static grondag.canvas.texture.pbr.PbrMapSpriteLayer.ROUGHNESS;

public enum PbrMapAtlasLayer {
	NORMAL_HEIGHT(0, new Swizzler() {
		@Override
		public int r(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.r(NORMAL, x, y);
		}

		@Override
		public int g(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.g(NORMAL, x, y);
		}

		@Override
		public int b(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.b(NORMAL, x, y);
		}

		@Override
		public int a(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.r(HEIGHT, x, y);
		}
	}),
	REFLECTANCE_ROUGHNESS_EMISSIVE_AO(1, new Swizzler() {
		@Override
		public int r(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.r(REFLECTANCE, x, y);
		}

		@Override
		public int g(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.r(ROUGHNESS, x, y);
		}

		@Override
		public int b(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.r(EMISSIVE, x, y);
		}

		@Override
		public int a(PbrMapSpriteLayer.LayeredImage image, int x, int y) {
			return image.r(AO, x, y);
		}
	});

	public final int layer;
	public final Swizzler swizzler;

	PbrMapAtlasLayer(int layer, Swizzler swizzler) {
		this.layer = layer;
		this.swizzler = swizzler;
	}

	public interface Swizzler {
		int r(PbrMapSpriteLayer.LayeredImage image, int x, int y);

		int g(PbrMapSpriteLayer.LayeredImage image, int x, int y);

		int b(PbrMapSpriteLayer.LayeredImage image, int x, int y);

		int a(PbrMapSpriteLayer.LayeredImage image, int x, int y);
	}
}
