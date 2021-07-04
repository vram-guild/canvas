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

package grondag.canvas.vf;

import grondag.canvas.texture.TextureData;
import grondag.canvas.varia.GFX;

public class VfInt extends VfTexture<IntElement> {
	public static final VfInt UV = new VfInt(TextureData.VF_UV, GFX.GL_RG16);
	public static final VfInt COLOR = new VfInt(TextureData.VF_COLOR, GFX.GL_RGBA8);

	//final AtomicInteger count = new AtomicInteger();

	private static final ThreadLocal<IntElement> SEARCH_KEY = ThreadLocal.withInitial(IntElement::new);

	private VfInt(int textureUnit, int imageFormat) {
		super(textureUnit, imageFormat, IntElement.class);
	}

	public int index(int c0, int c1, int c2, int c3) {
		//count.incrementAndGet();

		// WIP: avoid threadlocal
		final IntElement k = SEARCH_KEY.get();
		k.set(c0, c1, c2, c3);
		return MAP.computeIfAbsent(k, mapFunc).index;
	}
}
