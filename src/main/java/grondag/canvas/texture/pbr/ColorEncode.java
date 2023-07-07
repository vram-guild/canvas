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

// TODO: this class has to already exist somewhere..
public class ColorEncode {
	static int encode(int r, int g, int b, int a) {
		return ((r & 0xFF) << 0) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | ((a & 0xFF) << 24);
	}

	static int r(int color) {
		return (color >> 0) & 0xFF;
	}

	static int g(int color) {
		return (color >> 8) & 0xFF;
	}

	static int b(int color) {
		return (color >> 16) & 0xFF;
	}

	static int a(int color) {
		return (color >> 24) & 0xFF;
	}

	static final int RED_BYTE_OFFSET = 0;
	static final int GREEN_BYTE_OFFSET = 1;
	static final int BLUE_BYTE_OFFSET = 2;
	static final int ALPHA_BYTE_OFFSET = 3;
}
