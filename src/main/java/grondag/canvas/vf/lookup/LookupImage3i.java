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

package grondag.canvas.vf.lookup;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.varia.GFX;

@Environment(EnvType.CLIENT)
public class LookupImage3i extends AbstractLookupImage {
	public LookupImage3i(int textureUnit, int texelCapacity) {
		super(textureUnit, GFX.GL_RGB32I, texelCapacity, 3);
	}

	public void set(int index, int x, int y, int z) {
		index *= intsPerTexel;
		values[index] = x;
		values[index + 1] = y;
		values[index + 2] = z;
		isDirty.set(true);
	}
}
