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

package grondag.canvas.texture;

import org.lwjgl.opengl.GL21;

public class TextureData {
	// bindings MC uses during world rendering
	public static final int MC_SPRITE_ATLAS = GL21.GL_TEXTURE0;
	public static final int MC_OVELAY = GL21.GL_TEXTURE1;
	public static final int MC_LIGHTMAP = GL21.GL_TEXTURE2;

	// NB: texture 3 seems to be used by something
	public static final int HD_LIGHTMAP = GL21.GL_TEXTURE4;
	public static final int DITHER = GL21.GL_TEXTURE5;
	public static final int MATERIAL_INFO = GL21.GL_TEXTURE7;
	public static final int VF_COLOR = GL21.GL_TEXTURE8;
	// want these outside of the range managed by Mojang's damn GlStateManager
	public static final int SHADOWMAP = GL21.GL_TEXTURE12;
	public static final int SHADOWMAP_TEXTURE = GL21.GL_TEXTURE13;
	public static final int PROGRAM_SAMPLERS = GL21.GL_TEXTURE14;
}
