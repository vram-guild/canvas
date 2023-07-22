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

package grondag.canvas.texture;

import org.lwjgl.opengl.GL21;

public class TextureData {
	// bindings MC uses during world rendering
	public static final int MC_SPRITE_ATLAS = GL21.GL_TEXTURE0;
	//public static final int MC_OVELAY = GL21.GL_TEXTURE1;
	public static final int MC_LIGHTMAP = GL21.GL_TEXTURE2;

	// want these outside of the range managed by Mojang's damn GlStateManager
	public static final int SHADOWMAP = GL21.GL_TEXTURE12;
	public static final int SHADOWMAP_TEXTURE = SHADOWMAP + 1;
	public static final int COLORED_LIGHTS_DATA = SHADOWMAP_TEXTURE + 1;
	public static final int MATERIAL_INFO = COLORED_LIGHTS_DATA + 1;
	public static final int PROGRAM_SAMPLERS = MATERIAL_INFO + 1;
}
